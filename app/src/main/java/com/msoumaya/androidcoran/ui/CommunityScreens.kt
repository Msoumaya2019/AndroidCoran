package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.query
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun RecitationsScreen(vm: CoranViewModel) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var rows by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var corrections by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var feedback by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selected by remember(owner) { mutableStateOf<JsonObject?>(null) }
    val context=LocalContext.current
    fun title(row: JsonObject): String = if(row.str("recording_type")=="invocation")
        row.obj("invocation_snapshot").str("title").ifBlank { "Prononciation d’invocation" }
        else vm.repo.quran.reference(VerseRange(row.num("start_verse_id"),row.num("end_verse_id")))
    fun load() { vm.action {
        val account=owner?:error("Connexion nécessaire")
        val result=vm.repo.query("recitations",mapOf("user_id" to account),size=100,cacheResult=true)
        if(vm.repo.user.value==account) rows=result
    } }
    fun play(path: String,local: Boolean=false) { vm.action {
        val url=if(local) java.io.File(path).toURI().toString() else vm.repo.signedRecitation(path)
        context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_URL").putExtra("url",url))
    } }
    LaunchedEffect(owner) { if(owner!=null) load() }
    PageList {
        RecorderPanel(vm)
        Button(onClick={vm.action { vm.repo.uploadRecordings();load() }}) { Text("Envoyer les enregistrements sauvegardés") }
        val pending=vm.repo.recordings().filter { !it.flag("synced") }
        Text("${pending.size} enregistrement(s) en attente")
        pending.forEach { row -> Panel(title(row),"Sauvegardé sur ce téléphone") {
            TextButton(onClick={play(row.str("local_path"),true)}) { Text("Écouter") }
        } }
        Button(onClick={load()}) { Text("Actualiser les récitations") }
        rows.forEach { row -> Panel(title(row),row.str("created_at"),{
            vm.action {
                val account=owner
                val c=vm.repo.query("recitation_corrections",mapOf("recitation_id" to row.str("id")),size=300,cacheResult=true)
                val f=vm.repo.query("recitation_feedback",mapOf("recitation_id" to row.str("id")),size=100,cacheResult=true)
                if(vm.repo.user.value==account) { selected=row;corrections=c;feedback=f }
            }
        }) { TextButton(onClick={play(row.str("storage_path"))}) { Text("Écouter l’enregistrement") } } }
        selected?.let { row ->
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
        if(rows.isEmpty()) Text("Aucune récitation chargée")
    }
}
