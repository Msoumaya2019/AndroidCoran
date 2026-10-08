package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.util.UUID

@Composable fun AdminRecitationsPanel(vm: CoranViewModel) {
    val elapsed by RecitationService.elapsed.collectAsStateWithLifecycle()
    var playingId by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var profiles by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var corrected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var type by remember { mutableStateOf("all") };var filter by remember { mutableStateOf("pending") };var selected by remember { mutableStateOf<JsonObject?>(null) };var comments by remember { mutableStateOf<Map<Int,String>>(emptyMap()) };var general by remember { mutableStateOf("") };var request by remember { mutableStateOf(UUID.randomUUID().toString()) };var busy by remember { mutableStateOf(false) };var voice by remember { mutableStateOf<String?>(null) };var previous by remember { mutableStateOf<List<JsonObject>>(emptyList()) };val context=LocalContext.current
    fun act(block: suspend ()->Unit) { if(busy) return;busy=true;vm.action { try { check(AdminService(vm.repo).authorized());block() } finally { busy=false } } }
    suspend fun load() {
        rows=vm.repo.query("recitations",size=200)
        profiles=vm.repo.query("friend_profiles",ids="id" to rows.map { it.str("user_id") }.distinct(),orderBy="id",size=300)
        val ids=mutableSetOf<String>();var offset=0
        do { val page=vm.repo.query("recitation_corrections",offset=offset,size=300);ids.addAll(page.map { it.str("recitation_id") });offset+=page.size } while(page.size==300)
        corrected=ids
    }
    LaunchedEffect(Unit) { act { load() } }
    Text("Récitations des élèves",style=MaterialTheme.typography.headlineSmall)
    listOf("all" to "Toutes","pending" to "En attente","corrected" to "Corrigées").forEach { (id,label)->FilterChip(selected=filter==id,onClick={filter=id},label={Text(label)}) }
    listOf("all" to "Tout","quran" to "Coran","invocation" to "Invocations").forEach { (id,label)->FilterChip(selected=type==id,onClick={type=id},label={Text(label)}) }
    Button(enabled=!busy,onClick={act { load() }}) { Text("Actualiser") }
    rows.filter { (type=="all"||it.str("recording_type","quran")==type)&&(filter=="all"||(filter=="corrected")== (it.str("id") in corrected)) }.forEach { row ->
        val invocation=row.str("recording_type")=="invocation"
        val title=if(invocation) row.obj("invocation_snapshot").str("title","Prononciation") else vm.repo.quran.reference(VerseRange(row.num("start_verse_id"),row.num("end_verse_id")))
        Panel("${profiles.firstOrNull { it.str("id")==row.str("user_id") }?.str("display_name")?:"Élève"} · $title",row.str("created_at"),{act { selected=row;playingId=null;comments=emptyMap();general="";voice=null;request=UUID.randomUUID().toString();previous=vm.repo.query("recitation_corrections",mapOf("recitation_id" to row.str("id")),size=300)+vm.repo.query("recitation_feedback",mapOf("recitation_id" to row.str("id")),size=100) }}) {
            Text(if(row.str("id") in corrected) "Corrigée" else if(row.str("listened_at").isNotBlank()) "Écoutée" else "En attente")
            if(selected?.str("id")==row.str("id")) {
                TextButton(onClick={act { val url=vm.repo.signedRecitation(row.str("storage_path"));playingId=row.str("id");context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_URL").putExtra("url",url));if(row.str("listened_at").isBlank()) vm.repo.updateRows("recitations",json("listened_at" to java.time.Instant.now().toString()),mapOf("id" to row.str("id")));load() }}) { Text("Écouter") }
                if(playingId==row.str("id")) { Text("Position : ${elapsed/60000}:${(elapsed/1000%60).toString().padStart(2,'0')}");listOf(-10000L to "− 10 s",10000L to "+ 10 s").forEach { (delta,label) -> TextButton(onClick={context.startService(Intent(context,RecitationService::class.java).setAction("SEEK").putExtra("delta",delta))}) { Text(label) } } }
                TextButton(enabled=playingId==row.str("id"),onClick={context.startService(Intent(context,RecitationService::class.java).setAction("TOGGLE"))}) { Text("Pause / reprendre") }
                if(invocation) Text(row.obj("invocation_snapshot").str("arabic_text")) else {
                    VerseRange(row.num("start_verse_id"),row.num("end_verse_id")).ids.forEach { id ->
                        FilterChip(selected=id in comments,onClick={comments=if(id in comments) comments-id else comments+(id to "")},label={Text("${vm.repo.quran.verse(id).ayah} · ${vm.repo.quran.verse(id).text}")})
                        if(id in comments) OutlinedTextField(comments[id]?:"",{comments=comments+(id to it)},label={Text("Commentaire sur ce verset")})
                    }
                    OutlinedTextField(general,{general=it},label={Text("Commentaire général facultatif")})
                    key(row.str("id"),request) { AdminVoicePanel(vm,row.str("id")) { voice=it } }
                    Button(enabled=!busy&&(comments.isNotEmpty()||general.isNotBlank()||voice!=null),onClick={act { vm.repo.rpc("finalize_recitation_correction",recitationCorrectionPayload(row,request,comments,general,voice));request=UUID.randomUUID().toString();comments=emptyMap();general="";voice=null;previous=vm.repo.query("recitation_corrections",mapOf("recitation_id" to row.str("id")),size=300)+vm.repo.query("recitation_feedback",mapOf("recitation_id" to row.str("id")),size=100);load();vm.repo.feedback("Correction validée pour cet élève") }}) { Text("Valider la correction") }
                }
                previous.forEach { c -> Text("${c.num("verse_id")} · ${c.str("comment")} · ${c.str("created_at")}") }
            }
        }
    }
}
