package com.msoumaya.androidcoran.ui
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.msoumaya.androidcoran.audio.NativeRecorder
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
@Composable fun RecorderPanel(vm: CoranViewModel,invocation: JsonObject?=null,fixedRange: VerseRange?=null,compact: Boolean=false,onActiveChanged: (Boolean)->Unit={}) {
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var start by rememberSaveable { mutableStateOf("1") };var end by rememberSaveable { mutableStateOf("7") }
 var engine by remember { mutableStateOf<NativeRecorder?>(null) };var phase by remember { mutableStateOf(NativeRecorder.Phase.Idle) }
 var busy by remember { mutableStateOf(false) };var informedOwner by remember { mutableStateOf<String?>(null) }
 var captureOwner by remember { mutableStateOf("") };var captureRange by remember { mutableStateOf(VerseRange(1,7)) };var captureInvocation by remember { mutableStateOf<JsonObject?>(null) }
 var duration by remember { mutableLongStateOf(0) };var message by remember { mutableStateOf("") };var previewPlaying by remember { mutableStateOf(false) }
 fun audio(action: String,url: String?=null) { context.startService(Intent(context,RecitationService::class.java).setAction(action).apply { if(url!=null) putExtra("url",url) }) }
 fun run(block: suspend ()->Unit) { if(busy) return;busy=true;scope.launch { try { block() } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message?:"Enregistrement impossible" } finally { phase=engine?.phase?:NativeRecorder.Phase.Idle;duration=engine?.duration?:0;busy=false } } }
 fun startRecording() { run {
  val owner=vm.repo.user.value?:error("Connecte-toi dans Profil pour sauvegarder et synchroniser tes récitations.")
  check(owner==captureOwner) { "Le compte a changé. Recommence l’enregistrement." }
  audio("STOP");previewPlaying=false
  engine?.close();val next=NativeRecorder(context,File(context.filesDir,"recordings/$owner"));engine=next;next.start();message=""
 } }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) startRecording() else message="Autorise le microphone dans les réglages du téléphone." }
 fun permittedStart() { if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startRecording() else permission.launch(Manifest.permission.RECORD_AUDIO) }
 fun begin() { run {
  val owner=vm.repo.user.value?:error("Connecte-toi dans Profil pour sauvegarder et synchroniser tes récitations.")
  captureOwner=owner;captureRange=if(invocation!=null) VerseRange(1,1) else fixedRange?:VerseRange(start.toInt(),end.toInt());captureInvocation=invocation
  if(!vm.repo.recordingInformed(owner)) informedOwner=owner else { busy=false;permittedStart() }
 } }
 suspend fun persist() {
  val active=engine?:error("Fichier indisponible");val file=active.file?:error("Fichier indisponible")
  check(vm.repo.user.value==captureOwner) { "Le compte a changé. Reconnecte-toi pour sauvegarder cette récitation." }
  active.beginSave()
  try { withContext(NonCancellable+Dispatchers.IO) { vm.repo.saveRecording(file,captureRange,active.duration,captureOwner,captureInvocation);active.saved() } } catch(e: Exception) { active.saveFailed();throw e }
  message="Enregistré sur ce téléphone. Synchronisation automatique en cours."
  vm.action { vm.repo.uploadRecordings() }
 }
 fun save() { run { persist() } }
 LaunchedEffect(phase,busy) { onActiveChanged(busy||phase==NativeRecorder.Phase.Recording||phase==NativeRecorder.Phase.Paused) }
 LaunchedEffect(phase) { while(phase==NativeRecorder.Phase.Recording) { duration=engine?.duration?:0;delay(250) } }
 DisposableEffect(Unit) { onDispose { engine?.close();if(previewPlaying) audio("STOP");onActiveChanged(false) } }
 informedOwner?.let { owner -> AlertDialog(onDismissRequest={informedOwner=null},title={Text("Tes récitations")},text={Text("Vos récitations et prononciations enregistrées sont automatiquement sauvegardées et accessibles à l’administrateur pour le suivi de votre apprentissage et vos corrections. Elles restent sur ce téléphone après synchronisation. Tu peux demander leur suppression depuis ton compte.")},confirmButton={TextButton(onClick={informedOwner=null;run { vm.repo.acceptRecordingInformation(owner);busy=false;permittedStart() }}) { Text("Compris, enregistrer") }},dismissButton={TextButton(onClick={informedOwner=null}) { Text("Annuler") }}) }
 Panel(if(invocation!=null) "Ma prononciation" else "Enregistrer ma récitation") {
  if(invocation==null&&fixedRange==null) { OutlinedTextField(start,{start=it},label={Text("Premier verset (numéro global)")},enabled=phase==NativeRecorder.Phase.Idle&&!busy);OutlinedTextField(end,{end=it},label={Text("Dernier verset")},enabled=phase==NativeRecorder.Phase.Idle&&!busy) }
  else if(invocation!=null) { Text(invocation.str("arabic_text",invocation.str("french_text")));Text(invocation.str("phonetic_text")) }
  else Text(vm.repo.quran.reference(fixedRange!!))
  Text(when(phase) { NativeRecorder.Phase.Idle->"Enregistrement personnel";NativeRecorder.Phase.Recording->"● Enregistrement";NativeRecorder.Phase.Paused->"En pause";NativeRecorder.Phase.Preview->"Prêt à réécouter";NativeRecorder.Phase.Saving->"Sauvegarde en cours";NativeRecorder.Phase.Saved->"Récitation enregistrée" }+if(phase!=NativeRecorder.Phase.Idle) " · %02d:%02d".format(duration/60000,duration/1000%60) else "")
  if(phase==NativeRecorder.Phase.Idle) Button(enabled=!busy,onClick=::begin) { Text(if(compact) "Commencer" else "Enregistrer ma voix") }
  if(phase==NativeRecorder.Phase.Recording) TextButton(enabled=!busy,onClick={run { engine!!.pause() }}) { Text("Pause") }
  if(phase==NativeRecorder.Phase.Paused) TextButton(enabled=!busy,onClick={run { engine!!.resume() }}) { Text("Reprendre") }
  if(phase==NativeRecorder.Phase.Recording||phase==NativeRecorder.Phase.Paused) {
   Button(enabled=!busy,onClick={run { engine!!.finish();if(!compact&&captureInvocation==null) persist() }}) { Text(if(compact||invocation!=null) "Terminer" else "Terminer et sauvegarder") }
   TextButton(enabled=!busy,onClick={run { engine!!.restart();message="Enregistrement annulé. Rien n’a été envoyé." }}) { Text("Annuler") }
  }
  if(phase==NativeRecorder.Phase.Preview||phase==NativeRecorder.Phase.Saved) {
   TextButton(enabled=!busy,onClick={engine?.file?.let { audio("PLAY_LOCAL",it.toURI().toString());previewPlaying=true }}) { Text("Réécouter") }
   TextButton(enabled=!busy,onClick={if(previewPlaying) audio("STOP");previewPlaying=false;engine?.restart();phase=NativeRecorder.Phase.Idle;duration=0;message=""}) { Text("Recommencer") }
   if(phase==NativeRecorder.Phase.Preview) Button(enabled=!busy,onClick=::save) { Text("Enregistrer") }
  }
  if(message.isNotBlank()) Text(message)
 }
}
