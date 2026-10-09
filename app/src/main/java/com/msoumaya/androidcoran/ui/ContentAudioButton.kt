package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.ContentMedia
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

@Composable fun ContentAudioButton(vm: CoranViewModel,content: JsonObject) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val key=remember(owner,content) { "content:${owner.orEmpty()}:${content.str("id")}:${content.str("audio_url")}" }
    val active by RecitationService.standaloneKey.collectAsStateWithLifecycle()
    val playing by RecitationService.playing.collectAsStateWithLifecycle()
    val alive=remember(key) { mutableStateOf(true) }
    var pending by remember(key) { mutableStateOf(false) }
    DisposableEffect(key) { alive.value=true;onDispose {
        alive.value=false;
        if(RecitationService.standaloneKey.value==key) context.startService(Intent(context,RecitationService::class.java).setAction("STOP").putExtra("expectedRecordingKey",key))
    } }
    TextButton(enabled=!pending,onClick={
        if(active==key) context.startService(Intent(context,RecitationService::class.java).setAction("TOGGLE").putExtra("expectedRecordingKey",key))
        else { pending=true;scope.launch {
            try {
                val url=ContentMedia(vm.repo).resolve(content.str("audio_url"))
                if(alive.value&&vm.repo.user.value==owner) context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_URL").putExtra("url",url).putExtra("recordingKey",key))
            } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Audio indisponible") }
            finally { pending=false }
        } }
    }) { Text(if(pending) "Chargement…" else if(active==key&&playing) "Pause" else "Écouter") }
}
