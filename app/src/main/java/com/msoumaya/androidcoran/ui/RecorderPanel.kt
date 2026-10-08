package com.msoumaya.androidcoran.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.io.File

@Composable fun RecorderPanel(vm: CoranViewModel) {
    val context=LocalContext.current;var start by remember { mutableStateOf("1") };var end by remember { mutableStateOf("7") };var recording by remember { mutableStateOf(false) };var recorder by remember { mutableStateOf<MediaRecorder?>(null) };var file by remember { mutableStateOf<File?>(null) };var started by remember { mutableLongStateOf(0) };var owner by remember { mutableStateOf("") };var r by remember { mutableStateOf(VerseRange(1,7)) }
    fun begin() {
        try { check(vm.repo.user.value!=null) { "Connexion nécessaire" };r=VerseRange(start.toInt(),end.toInt());owner=vm.repo.user.value!!
            context.startService(android.content.Intent(context,com.msoumaya.androidcoran.audio.RecitationService::class.java).setAction("STOP"))
            val folder=File(context.filesDir,"recordings/$owner").apply { mkdirs() };file=File(folder,"${java.util.UUID.randomUUID()}.m4a")
            val m=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            m.setAudioSource(MediaRecorder.AudioSource.MIC);m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);m.setAudioSamplingRate(44100);m.setAudioEncodingBitRate(128000);m.setOutputFile(file!!.path);m.prepare();m.start();recorder=m;started=android.os.SystemClock.elapsedRealtime();recording=true
        } catch(e: Exception) { recorder?.release();recorder=null;recording=false;vm.repo.feedback(e.message?:"Microphone indisponible") }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) begin() else vm.repo.feedback("Autorisation microphone refusée") }
    DisposableEffect(Unit) { onDispose { recorder?.let { runCatching { it.stop() };it.release() };recorder=null } }
    Panel("Enregistrer ma récitation") {
        OutlinedTextField(start,{start=it},label={Text("Premier verset (numéro global)")},enabled=!recording);OutlinedTextField(end,{end=it},label={Text("Dernier verset")},enabled=!recording)
        Button(onClick={ if(recording) { try { recorder!!.stop();recorder!!.release();recorder=null;recording=false;val duration=android.os.SystemClock.elapsedRealtime()-started;val saved=file!!;val range=r;val user=owner;vm.action { vm.repo.saveRecording(saved,range,duration,user);vm.repo.feedback("Enregistrement sauvegardé sur cet appareil") } } catch(e: Exception) { recorder?.release();recorder=null;recording=false;vm.repo.feedback("Enregistrement trop court ou interrompu") } } else if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) begin() else permission.launch(Manifest.permission.RECORD_AUDIO) }) { Text(if(recording) "Arrêter et sauvegarder" else "Enregistrer") }
        Text("L’enregistrement s’arrête lorsque cet écran est fermé. Les fichiers sauvegardés restent sur cet appareil jusqu’à leur envoi.")
    }
}
