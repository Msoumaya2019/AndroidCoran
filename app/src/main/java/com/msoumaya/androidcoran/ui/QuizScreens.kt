package com.msoumaya.androidcoran.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.time.*

@Composable fun QuizScreen(vm: CoranViewModel) {
    val owner by vm.repo.user.collectAsStateWithLifecycle();val stored by vm.repo.quiz.snapshot.collectAsStateWithLifecycle()
    val snapshot=if(vm.repo.quiz.cacheOwner==owner) stored else emptyQuiz()
    var view by rememberSaveable(owner) { mutableStateOf("Accueil") };var historyDay by rememberSaveable(owner) { mutableStateOf<String?>(null) };var challengeId by rememberSaveable(owner) { mutableStateOf<String?>(null) }
    var friends by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) };var profiles by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) };var opponent by rememberSaveable(owner) { mutableStateOf("") };var setId by rememberSaveable(owner) { mutableStateOf<String?>(null) };var count by rememberSaveable { mutableIntStateOf(10) };var busy by remember { mutableStateOf(false) };var now by remember { mutableStateOf(Instant.now()) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    fun refresh() { vm.action { vm.repo.quiz.refresh() } }
    LaunchedEffect(owner) {
        vm.repo.quiz.loadCached()
        if(owner!=null) vm.action {
            friends=vm.repo.query("friend_links",eq=mapOf("status" to "accepted"))
            val ids=friends.map { if(it.str("requester_id")==owner) it.str("recipient_id") else it.str("requester_id") }
            profiles=vm.repo.query("friend_profiles",ids="id" to ids,orderBy="display_name",ascending=true,size=300)
        }
    }
    LaunchedEffect(owner,lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if(owner!=null) try { vm.repo.quiz.refresh() } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback("Quiz chargé depuis cet appareil ; synchronisation en attente") }
            while(isActive) { now=Instant.now();delay(60_000) }
        }
    }
    val day=now.atZone(ZoneId.systemDefault()).toLocalDate().toString()
    val response=snapshot.arr("responses").map { it.jsonObject }.firstOrNull { it.str("day")== (historyDay?:day) }
    val daily=if(historyDay!=null) response?.obj("question") else (snapshot["daily"] as? JsonObject)
    val challenge=snapshot.arr("challenges").map { it.jsonObject }.firstOrNull { it.str("id")==challengeId }
    PageList {
        Row { if(view!="Accueil") TextButton(onClick={view="Accueil";historyDay=null}) { Text("Retour au Quiz") };TextButton(onClick={refresh()}) { Text("Actualiser") } }
        if(owner==null) Text("Connecte-toi pour enregistrer tes réponses et défier tes amis")
        when(view) {
            "Accueil" -> {
                Text("Teste tes connaissances",style=MaterialTheme.typography.headlineSmall)
                Panel("Question du jour",if(snapshot.arr("responses").any { it.jsonObject.str("day")==day }) "Terminée aujourd’hui" else if(daily==null) "Aucune question publiée aujourd’hui" else "Disponible",{historyDay=null;view="Question"})
                Panel("Défis entre amis","5 ou 10 questions · durée de 48 heures",{view="Défis"})
                TextButton(onClick={view="Créer"}) { Text("Nouveau défi") };TextButton(onClick={view="Historique"}) { Text("Historique des réponses") }
                QuizStatsCard(snapshot,owner?:"")
                Row { Checkbox(snapshot.flag("notificationsEnabled",true),{ enabled -> vm.action { vm.repo.rpc("quiz_set_notifications",json("p_enabled" to enabled,"p_timezone" to ZoneId.systemDefault().id));vm.repo.quiz.refresh() } },enabled=owner!=null);Text("Notifications Quiz") }
            }
            "Question" -> if(daily==null||daily.isEmpty()) Text("Aucune question disponible pour ce jour") else {
                Text(daily.str("category"));Text(daily.str("question"),style=MaterialTheme.typography.titleLarge)
                QuizAnswers(daily,response?.str("selectedAnswerId"),enabled=owner!=null&&response==null) { answer -> vm.action { vm.repo.quiz.answer(daily,answer);try { vm.repo.quiz.refresh() } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback("Réponse sauvegardée sur cet appareil, en attente de synchronisation") } } }
                if(response?.flag("pending")==true) Text("Réponse enregistrée. La correction sera disponible après synchronisation.")
                else if(response!=null) QuizCorrection(daily,response)
            }
            "Historique" -> {
                if(snapshot.arr("responses").isEmpty()) Text("Aucune réponse enregistrée")
                snapshot.arr("responses").forEach { raw -> val row=raw.jsonObject;Panel(row.str("day"),if(row.flag("pending")) "Synchronisation en attente" else if(row.flag("isCorrect")) "Bonne réponse" else "Mauvaise réponse",{historyDay=row.str("day");view="Question"}) }
            }
            "Défis" -> {
                TextButton(onClick={view="Créer"}) { Text("Nouveau défi") }
                snapshot.arr("challenges").forEach { raw -> val row=raw.jsonObject;Panel("${row.str("creatorName")} · ${row.str("opponentName")}",challengeStatus(row,owner?:"",now),{challengeId=row.str("id");view="Défi"}) }
                if(snapshot.arr("challenges").isEmpty()) Text("Aucun défi pour le moment")
            }
            "Créer" -> {
                Text("Choisir un ami",style=MaterialTheme.typography.titleLarge)
                friends.forEach { row -> val id=if(row.str("requester_id")==owner) row.str("recipient_id") else row.str("requester_id");val profile=profiles.firstOrNull { it.str("id")==id };FilterChip(selected=opponent==id,onClick={opponent=id},label={Text(profile?.str("display_name")?:"Ami")}) }
                if(friends.isEmpty()) Text("Ajoute un ami dans l’espace Amis pour lancer un défi")
                FilterChip(selected=setId==null,onClick={setId=null},label={Text("Questions aléatoires")})
                snapshot.arr("quizSets").forEach { raw -> val row=raw.jsonObject;FilterChip(selected=setId==row.str("id"),onClick={setId=row.str("id");count=10},label={Text("${row.str("title")} · ${row.str("category")}")}) }
                if(setId==null) Row { listOf(5,10).forEach { number -> FilterChip(selected=count==number,onClick={count=number},label={Text("$number questions")}) } }
                Text("Les mêmes questions pour vous deux · durée de 48 heures")
                Button(enabled=owner!=null&&opponent.isNotBlank()&&!busy,onClick={vm.action { busy=true;try { challengeId=vm.repo.quiz.challenge(opponent,count,setId);view="Défi" } finally { busy=false } }}) { Text(if(busy) "Création…" else "Lancer le défi") }
            }
            "Défi" -> if(challenge==null) Text("Défi indisponible dans le cache. Actualise pour le récupérer.") else {
                Text("${challenge.str("creatorName")} · ${challenge.str("opponentName")}",style=MaterialTheme.typography.titleLarge)
                val status=challengeStatus(challenge,owner?:"",now)
                val own=challenge.arr("answers").map { it.jsonObject }.filter { it.str("userId")==owner }
                val next=challenge.arr("questions").map { it.jsonObject }.firstOrNull { q -> own.none { it.str("questionId")==q.str("id") } }
                when(status) {
                    "Terminé" -> {
                        Text("Défi terminé")
                        val creator=challenge.arr("answers").count { it.jsonObject.str("userId")==challenge.str("creatorId")&&it.jsonObject.flag("isCorrect") }
                        val opponentScore=challenge.arr("answers").count { it.jsonObject.str("userId")==challenge.str("opponentId")&&it.jsonObject.flag("isCorrect") }
                        Text("${challenge.str("creatorName")} : $creator / ${challenge.num("questionCount")}");Text("${challenge.str("opponentName")} : $opponentScore / ${challenge.num("questionCount")}")
                        Text(if(creator==opponentScore) "Égalité" else "${challenge.str(if(creator>opponentScore) "creatorName" else "opponentName")} remporte le défi")
                        challenge.arr("questions").forEach { raw -> val question=raw.jsonObject;val answer=own.firstOrNull { it.str("questionId")==question.str("id") };Text(question.str("question"));QuizAnswers(question,answer?.str("selectedAnswerId"),false) {};answer?.let { QuizCorrection(question,it) } }
                    }
                    "Expiré" -> Text("Défi expiré. Aucun vainqueur n’est comptabilisé.")
                    else -> if(next==null) Text("Tes réponses sont enregistrées. En attente de ton ami.") else {
                        Text("${own.size+1} / ${challenge.num("questionCount")} · ${((Instant.parse(challenge.str("expiresAt")).toEpochMilli()-now.toEpochMilli()+3_599_999)/3_600_000).coerceAtLeast(0)} h restantes")
                        Text(next.str("category"));Text(next.str("question"),style=MaterialTheme.typography.titleLarge)
                        QuizAnswers(next,null,!busy) { answer -> vm.action { busy=true;try { vm.repo.quiz.challengeAnswer(challenge.str("id"),next.str("id"),answer) } finally { busy=false } } }
                        Text("La correction sera révélée quand vous aurez tous les deux terminé")
                    }
                }
            }
        }
    }
}
@Composable private fun QuizAnswers(question: JsonObject,selected: String?,enabled: Boolean,onAnswer: (String)->Unit) {
    question.arr("answers").forEach { raw -> val answer=raw.jsonObject;OutlinedButton(enabled=enabled,onClick={onAnswer(answer.str("id"))},modifier=Modifier.fillMaxWidth()) { Text("${if(answer.str("id")==selected) "✓ " else ""}${answer.str("text")}") } }
}
@Composable private fun QuizCorrection(question: JsonObject,response: JsonObject) {
    val context=LocalContext.current
    Panel(if(response.flag("isCorrect")) "Bonne réponse" else "Mauvaise réponse") {
        val correct=question.arr("answers").map { it.jsonObject }.firstOrNull { it.str("id")==question.str("correctAnswerId") }
        correct?.let { Text("Réponse : ${it.str("text")}") }
        if(question.str("arabic").isNotBlank()) Text(question.str("arabic"),style=MaterialTheme.typography.headlineSmall)
        if(question.str("translation").isNotBlank()) Text(question.str("translation"))
        if(question.str("explanation").isNotBlank()) Text(question.str("explanation"))
        Text("${question.str("sourceTitle")} ${question.str("sourceReference")}")
        if(question.str("sourceUrl").startsWith("https://")) TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(question.str("sourceUrl"))))}) { Text("Consulter la source") }
    }
}
@Composable fun QuizStatsCard(snapshot: JsonObject,owner: String) {
    val stats=quizStatistics(snapshot,owner)
    Panel("Statistiques Quiz") { Text("${stats.correct} / ${stats.total} bonnes réponses · ${stats.rate} %");Text("${stats.played} défis joués · ${stats.wins} victoires · ${stats.ties} égalités") }
}
