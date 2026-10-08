package com.msoumaya.androidcoran.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.msoumaya.androidcoran.data.AdminService
import com.msoumaya.androidcoran.audio.RecitationService
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.File
import java.util.UUID

@Composable fun AdminVoicePanel(vm: CoranViewModel,id: String,onUploaded: (String)->Unit) {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
    var phase by remember { mutableStateOf("idle") };var recorder by remember { mutableStateOf<MediaRecorder?>(null) };var file by remember { mutableStateOf<File?>(null) };var busy by remember { mutableStateOf(false) };var owner by remember { mutableStateOf<String?>(null) };var path by remember { mutableStateOf("") };var active by remember { mutableStateOf(true) }
    fun release() { recorder?.release();recorder=null }
    fun begin() { try {
        owner=vm.repo.user.value?:error("Connexion nécessaire")
        context.startService(Intent(context,RecitationService::class.java).setAction("STOP"))
        file=File(context.cacheDir,"feedback-${UUID.randomUUID()}.m4a");path="feedback/$owner/$id-${System.currentTimeMillis()}.m4a"
        val m=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        recorder=m;m.setAudioSource(MediaRecorder.AudioSource.MIC);m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);m.setAudioChannels(1);m.setAudioSamplingRate(44100);m.setAudioEncodingBitRate(64000);m.setOutputFile(file!!.path);m.prepare();m.start();phase="recording"
    } catch(e: Exception) { release();file?.delete();phase="idle";vm.repo.feedback(e.message?:"Microphone indisponible") } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) begin() else vm.repo.feedback("Autorise le microphone") }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP&&phase=="recording") { try { recorder?.pause();phase="paused" } catch(e: Exception) { release();file?.delete();phase="idle" } } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { active=false;lifecycle.lifecycle.removeObserver(observer);recorder?.let { runCatching { it.stop() } };release();file?.delete() }
    }
    Text("Commentaire vocal")
    when(phase) {
        "idle" -> TextButton(onClick={if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) begin() else permission.launch(Manifest.permission.RECORD_AUDIO)}) { Text("Enregistrer un commentaire vocal") }
        "recording","paused" -> {
            Text(if(phase=="recording") "Enregistrement" else "En pause")
            TextButton(onClick={try { if(phase=="recording") { recorder!!.pause();phase="paused" } else { recorder!!.resume();phase="recording" } } catch(e: Exception) { vm.repo.feedback("Microphone interrompu") }}) { Text(if(phase=="recording") "Pause" else "Reprendre") }
            TextButton(onClick={try { recorder!!.stop();release();phase="ready" } catch(e: Exception) { release();file?.delete();phase="idle";vm.repo.feedback("Enregistrement trop court") }}) { Text("Terminer") }
        }
        "ready" -> {
            TextButton(enabled=!busy,onClick={busy=true;vm.action { try {
                check(AdminService(vm.repo).authorized()&&owner==vm.repo.user.value)
                val saved=file?:error("Enregistrement indisponible");check(saved.isFile&&saved.length() in 1..100_000_000)
                val bytes=saved.readBytes();check(owner==vm.repo.user.value)
                val exists=try { vm.repo.signedRecitation(path);true } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e;false }
                if(!exists) vm.repo.db().storage.from("recitations").upload(path,bytes) { contentType=ContentType.parse("audio/mp4") }
                check(owner==vm.repo.user.value);if(active) { onUploaded(path);phase="uploaded";saved.delete();file=null }
            } finally { busy=false } }}) { Text("Joindre la correction vocale") }
            TextButton(enabled=!busy,onClick={file?.delete();file=null;phase="idle"}) { Text("Recommencer") }
        }
        "uploaded" -> Text("Audio joint. Valide maintenant la correction.")
    }
}
