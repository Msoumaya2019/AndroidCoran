package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate

val quizCategories=listOf("Coran","Tajwid","Prophètes","Sîra","Vocabulaire coranique","Connaissances générales")
fun emptyQuiz(day: String=LocalDate.now().toString())=json("day" to day,"daily" to null,"responses" to listOf<Any>(),"challenges" to listOf<Any>())
fun recordDailyAnswer(snapshot: JsonObject,question: JsonObject,answer: String,day: String,at: String): JsonObject {
    if(snapshot.arr("responses").any { it.jsonObject.str("day")==day }) return snapshot
    require(question.str("publicationDate")==day&&question.arr("answers").any { it.jsonObject.str("id")==answer }) { "Réponse ou date invalide" }
    val response=json("questionId" to question.str("id"),"day" to day,"selectedAnswerId" to answer,"answeredAt" to at,"question" to question,"pending" to true)
    return snapshot.with("responses" to JsonArray(listOf(response)+snapshot.arr("responses")))
}
fun mergeQuizSnapshot(remote: JsonObject,local: JsonObject): JsonObject {
    val byDay=remote.arr("responses").associate { it.jsonObject.str("day") to it }.toMutableMap()
    local.arr("responses").forEach { row -> if(row.jsonObject.flag("pending")&&row.jsonObject.str("day") !in byDay) byDay[row.jsonObject.str("day")]=row }
    return remote.with("responses" to JsonArray(byDay.values.sortedByDescending { it.jsonObject.str("day") }))
}
fun challengeStatus(challenge: JsonObject,user: String,now: Instant=Instant.now()): String = when {
    challenge.str("status")=="completed" -> "Terminé"
    challenge.str("status")=="expired"||Instant.parse(challenge.str("expiresAt"))<=now -> "Expiré"
    challenge.arr("answers").count { it.jsonObject.str("userId")==user }==challenge.num("questionCount") -> "En attente de l’ami"
    else -> "À toi de jouer"
}
data class QuizStatistics(val correct: Int,val total: Int,val rate: Int,val played: Int,val wins: Int,val ties: Int)
fun quizStatistics(snapshot: JsonObject,user: String): QuizStatistics {
    val confirmed=snapshot.arr("responses").map { it.jsonObject }.filter { !it.flag("pending")&&(it["isCorrect"] as? JsonPrimitive)?.booleanOrNull!=null }
    val correct=confirmed.count { it.flag("isCorrect") };val finished=snapshot.arr("challenges").map { it.jsonObject }.filter { it.str("status")=="completed" }
    var wins=0;var ties=0
    finished.forEach { c -> val own=c.arr("answers").count { it.jsonObject.str("userId")==user&&it.jsonObject.flag("isCorrect") };val other=c.arr("answers").count { it.jsonObject.str("userId")!=user&&it.jsonObject.flag("isCorrect") };if(own>other) wins++;if(own==other) ties++ }
    return QuizStatistics(correct,confirmed.size,if(confirmed.isEmpty()) 0 else kotlin.math.floor(correct.toDouble()/confirmed.size*100+0.5).toInt(),finished.size,wins,ties)
}
