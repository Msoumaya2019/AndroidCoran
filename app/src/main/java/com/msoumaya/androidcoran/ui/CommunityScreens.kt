package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.msoumaya.androidcoran.data.*
import kotlinx.coroutines.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.query
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun RecitationsScreen(vm: CoranViewModel,initialRecitationId: String?=null,onViewInvocation: ((String)->Unit)?=null) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val recordingVersion by vm.repo.recordingVersion.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope();val service=remember(vm) { RecitationLibraryService(vm.repo) }
    var filter by rememberSaveable { mutableStateOf("all") }
    var sharingId by rememberSaveable(owner,initialRecitationId) { mutableStateOf(initialRecitationId) }
    var friends by remember(owner) { mutableStateOf<List<RecitationFriend>>(emptyList()) }
    var sharingBusy by remember { mutableStateOf(false) };var friendsLoading by remember { mutableStateOf(false) };var shareError by remember(owner) { mutableStateOf("") }
    var removing by remember(owner) { mutableStateOf<JsonObject?>(null) };var removeBusy by remember { mutableStateOf(false) };var removeError by remember(owner) { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    var rows by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var corrections by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var feedback by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selected by remember(owner) { mutableStateOf<JsonObject?>(null) }
    val context=LocalContext.current
    fun audio(action: String,delta: Long=0L,key: String?=null) { context.startService(Intent(context,RecitationService::class.java).setAction(action).putExtra("delta",delta).apply { if(key!=null) putExtra("expectedRecordingKey",key) }) }
    fun title(row: JsonObject): String = if(row.str("recording_type")=="invocation")
        row.obj("invocation_snapshot").str("title").ifBlank { "Prononciation d’invocation" }
        else vm.repo.quran.reference(VerseRange(row.num("start_verse_id"),row.num("end_verse_id")))
    suspend fun refresh(synchronize: Boolean=false) {
        val account=owner?:return;if(refreshing) return;refreshing=true
        try {
            if(synchronize) try { withContext(Dispatchers.IO) { vm.repo.uploadRecordings() } } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback("Les fichiers locaux restent disponibles. "+e.message) }
            val result=vm.repo.query("recitations",mapOf("user_id" to account),size=100,cacheResult=true)
            if(vm.repo.user.value==account) rows=result
        } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Récitations indisponibles") } finally { refreshing=false }
    }
    fun load(synchronize: Boolean=false) { scope.launch { refresh(synchronize) } }
    fun play(path: String,local: Boolean=false,key: String?=null) { vm.action {
        val url=if(local) java.io.File(path).toURI().toString() else vm.repo.signedRecitation(path)
        context.startService(Intent(context,RecitationService::class.java).setAction(if(local) "PLAY_LOCAL" else "PLAY_URL").putExtra("url",url).apply { if(key!=null) putExtra("recordingKey",key) })
    } }
    LaunchedEffect(owner) { if(owner!=null) refresh(true) }
    LaunchedEffect(recordingVersion) { if(owner!=null) refresh() }
    LaunchedEffect(owner,initialRecitationId) {
        if(owner!=null&&initialRecitationId!=null) { refresh(true);repeat(10) { if(rows.any { it.str("id")==initialRecitationId }) return@LaunchedEffect;delay(3000);refresh() } }
    }
    val all=remember(owner,recordingVersion,rows) { recitationLibrary(owner,vm.repo.recordings(),rows,vm.repo.removedRecordingIds()) }
    val sharing=all.firstOrNull { it.str("id")==sharingId&&it.str("storage_path").isNotBlank() }
    LaunchedEffect(owner,sharing?.str("id")) {
        val account=owner
        if(sharing!=null&&account!=null) { friendsLoading=true;shareError="";try { val result=service.friends();if(vm.repo.user.value==account) friends=result } catch(e: Exception) { if(e is CancellationException) throw e;shareError=e.message?:"Amis indisponibles" } finally { friendsLoading=false } }
    }
    PageList {
        RecorderPanel(vm,onShare={sharingId=it;load(true)})
        Text("${all.count { !it.flag("synced") }} enregistrement(s) en attente")
        if(sharingId!=null&&sharing==null) Text("Synchronise cette récitation pour pouvoir la partager.")
        Button(enabled=!refreshing,onClick={load(true)}) { Text("Actualiser et synchroniser") }
        Row { listOf("all" to "Toutes","quran" to "Coran","invocation" to "Invocations").forEach { (value,label) -> FilterChip(selected=filter==value,onClick={filter=value},label={Text(label)}) } }
        all.filter { filter=="all"||it.str("recording_type","quran")==filter }.forEach { row -> Panel(title(row),row.str("created_at")+" · "+if(row.flag("synced")) "Synchronisé" else "En attente",{
            vm.action {
                val account=owner
                val c=vm.repo.query("recitation_corrections",mapOf("recitation_id" to row.str("id")),size=300,cacheResult=true)
                val f=vm.repo.query("recitation_feedback",mapOf("recitation_id" to row.str("id")),size=100,cacheResult=true)
                if(vm.repo.user.value==account) { selected=row;corrections=c;feedback=f }
            }
        }) {
            val playbackKey=recordingPlaybackKey(owner.orEmpty(),row.str("id"))
            LiveRecitationPlaybackControls(playbackKey,row.num("duration_ms").toLong(),onPlay={val local=row.str("local_path");if(local.isNotBlank()&&java.io.File(local).isFile) play(local,true,playbackKey) else play(row.str("storage_path"),key=playbackKey)},onToggle={audio("TOGGLE",key=playbackKey)},onSeek={audio("SEEK",it,playbackKey)})
            if(row.str("recording_type")=="invocation") {
                InvocationRecordingLink(row,onViewInvocation)
            }
            TextButton(onClick={removeError="";removing=row}) { Text("Supprimer") }
            if(row.str("recording_type","quran")=="quran") TextButton(enabled=row.str("storage_path").isNotBlank(),onClick={sharingId=row.str("id")}) { Text("Partager avec un ami") }
        } }
        selected?.takeIf { filter=="all"||it.str("recording_type","quran")==filter }?.let { row ->
            Text("Retours : ${title(row)}")
            feedback.forEach { f -> Panel("Commentaire général",f.str("comment")) {
                if(f.str("voice_path").isNotBlank()) TextButton(onClick={play(f.str("voice_path"))}) { Text("Écouter le retour vocal") }
            } }
            corrections.forEach { c -> Panel("Verset ${c.num("verse_id")}",c.str("comment")) {
                if(c.str("resolved_at").isNotBlank()) Text("Difficulté résolue")
                if(c.str("voice_path").isNotBlank()) TextButton(onClick={play(c.str("voice_path"))}) { Text("Écouter la correction") }
            } }
            if(corrections.isEmpty()&&feedback.isEmpty()) Text("Aucun retour pour cet enregistrement")
        }
        if(all.isEmpty()) Text("Aucune récitation chargée")
    }
    removing?.let { recording -> RecitationDeleteDialog(title(recording),removeBusy,removeError,onDelete={
        val account=owner
        if(account!=null&&!removeBusy) { removeBusy=true;scope.launch {
            try {
                val key=RecitationService.standaloneKey.value
                val local=recording.str("local_path").takeIf { it.isNotBlank() }?.let { java.io.File(it).toURI().toString() }
                if(key!=null&&(key==recordingPlaybackKey(account,recording.str("id"))||key==local)) audio("STOP",key=key)
                withContext(Dispatchers.IO) { vm.repo.removeRecording(account,recording) }
                if(vm.repo.user.value==account) { rows=rows.filter { it.str("id")!=recording.str("id") };if(selected?.str("id")==recording.str("id")) { selected=null;corrections=emptyList();feedback=emptyList() };if(sharingId==recording.str("id")) sharingId=null;removing=null;vm.repo.feedback("Récitation supprimée.") }
            } catch(e: Exception) { if(e is CancellationException) throw e;removeError=e.message?:"Suppression impossible" } finally { removeBusy=false }
        } }
    },onDismiss={removing=null}) }
    sharing?.let { recording -> RecitationSharingDialog(title(recording),friends,friendsLoading,sharingBusy,shareError,onShare={ linkId ->
        val account=owner
        if(account!=null&&!sharingBusy) { sharingBusy=true;shareError="";scope.launch {
            try { service.share(account,linkId,recording.str("id"),"Récitation vocale · "+title(recording));if(vm.repo.user.value==account) { sharingId=null;vm.repo.feedback("Récitation partagée dans votre conversation.") } }
            catch(e: Exception) { if(e is CancellationException) throw e;shareError=e.message?:"Partage impossible" } finally { sharingBusy=false }
        } }
    },onDismiss={sharingId=null},avatar={friend -> AvatarPreview(vm.repo,friend.profile.str("avatar_path"),androidx.compose.ui.Modifier.size(34.dp)) }) }
}
