package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.Instant

@Composable fun QuizScreen(vm: CoranViewModel) { var snapshot by remember { mutableStateOf<JsonObject?>(null) };var opponent by rememberSaveable { mutableStateOf("") };var active by remember { mutableStateOf<JsonObject?>(null) }
    fun load() { vm.action { snapshot=vm.repo.cachedRpc("quiz_snapshot",json("p_day" to LocalDate.now().toString())).jsonObject } }
    LaunchedEffect(vm.repo.user.value) { if(vm.repo.user.value!=null) load() }
    PageList { Button(onClick={load()}) { Text("Actualiser le quiz") };val daily=snapshot?.get("daily") as? JsonObject
        if(daily!=null) { Text("Question du jour",style=MaterialTheme.typography.titleLarge);Text(daily.str("question"));val answered=snapshot!!.arr("responses").any { it.jsonObject.str("day")==LocalDate.now().toString() };daily.arr("answers").forEach { raw -> val a=raw.jsonObject;Button(enabled=!answered,onClick={vm.action { vm.repo.rpc("quiz_answer_daily",json("p_question" to daily.str("id"),"p_answer" to a.str("id"),"p_day" to LocalDate.now().toString(),"p_answered_at" to Instant.now().toString()));load() }}) { Text(a.str("text")) } };if(answered) Text("Réponse enregistrée") }
        Text("Défis entre amis",style=MaterialTheme.typography.titleLarge);OutlinedTextField(opponent,{opponent=it},label={Text("Identifiant de l’ami")});Row { listOf(5,10).forEach { count -> TextButton(onClick={vm.action { vm.repo.rpc("quiz_create_challenge",json("p_opponent" to opponent.trim(),"p_count" to count,"p_set" to null));load() }}) { Text("$count questions") } } }
        snapshot?.arr("challenges")?.forEach { raw -> val c=raw.jsonObject;Panel("${c.str("creatorName")} / ${c.str("opponentName")}","${c.str("status")} · expire ${c.str("expiresAt")}",{active=c}) }
        active?.let { c -> Text("Défi en cours");c.arr("questions").forEach { raw -> val question=raw.jsonObject;val answered=c.arr("answers").any { it.jsonObject.str("userId")==vm.repo.user.value&&it.jsonObject.str("questionId")==question.str("id") };Text(question.str("question"));question.arr("answers").forEach { a -> TextButton(enabled=!answered&&c.str("status")=="pending"&&Instant.parse(c.str("expiresAt"))>Instant.now(),onClick={vm.action { vm.repo.rpc("quiz_answer_challenge",json("p_challenge" to c.str("id"),"p_question" to question.str("id"),"p_answer" to a.jsonObject.str("id")));active=null;load() }}) { Text(a.jsonObject.str("text")) } } } }
    }
}
@Composable fun RecitationsScreen(vm: CoranViewModel) { var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var corrections by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var selected by remember { mutableStateOf<JsonObject?>(null) };val context=androidx.compose.ui.platform.LocalContext.current
    fun load() { vm.action { rows=vm.repo.rows("recitations",mapOf("user_id" to (vm.repo.user.value?:error("Connexion nécessaire")))) } }
    LaunchedEffect(vm.repo.user.value) { if(vm.repo.user.value!=null) load() }
    PageList { RecorderPanel(vm);Button(onClick={vm.action { vm.repo.uploadRecordings();load() }}) { Text("Envoyer les enregistrements sauvegardés") };Text("${vm.repo.recordings().count { !it.flag("synced") }} enregistrement(s) en attente");Button(onClick={load()}) { Text("Actualiser les récitations") };rows.forEach { r -> Panel(vm.repo.quran.reference(VerseRange(r.num("start_verse_id"),r.num("end_verse_id"))),r.str("created_at"),{selected=r;vm.action { corrections=vm.repo.rows("recitation_corrections",mapOf("recitation_id" to r.str("id"))) }}) { TextButton(onClick={vm.action { val url=vm.repo.signedRecitation(r.str("storage_path"));withContextMain { context.startService(android.content.Intent(context,com.msoumaya.androidcoran.audio.RecitationService::class.java).setAction("PLAY_URL").putExtra("url",url)) } }}) { Text("Écouter l’enregistrement") } } };selected?.let { Text("Corrections");corrections.forEach { c -> Panel("Verset ${c.num("verse_id")}",c.str("comment")) } };if(rows.isEmpty()) Text("Aucune récitation chargée") }
}
private suspend fun withContextMain(block: ()->Unit)=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { block() }
@Composable fun ReportScreen(vm: CoranViewModel) { var description by rememberSaveable { mutableStateOf("") };var type by rememberSaveable { mutableStateOf("Bug") };PageList { Text("Signaler un dysfonctionnement");listOf("Bug","Affichage","Audio","Notification","Autre").forEach { t->FilterChip(selected=t==type,onClick={type=t},label={Text(t)}) };OutlinedTextField(description,{description=it},label={Text("Description")},minLines=4);Button(onClick={vm.action { require(description.trim().length>=10);vm.repo.insert("app_problem_reports",json("id" to java.util.UUID.randomUUID().toString(),"user_id" to vm.repo.user.value,"type" to type,"description" to description.trim(),"screenshot_path" to null,"app_version" to "0.1.0","platform" to "android","status" to "open"));description="";vm.repo.feedback("Signalement envoyé") }}) { Text("Envoyer le signalement") } } }
