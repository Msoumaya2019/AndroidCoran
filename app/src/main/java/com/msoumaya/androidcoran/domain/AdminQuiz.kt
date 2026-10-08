package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.LocalDate

fun freshAdminQuestion()=json("id" to "","category" to "Coran","question" to "","answers" to listOf("A","B","C","D").map { json("id" to it,"text" to "") },"correctAnswerId" to "A","sourceTitle" to "","publicationDate" to LocalDate.now().toString(),"isDailyQuestion" to false,"availableForChallenges" to true,"isActive" to true)
fun adminQuestionPayload(d: JsonObject): JsonObject {
    val answers=d.arr("answers").map { it.jsonObject }.filter { it.str("text").isNotBlank() }
    require(d.str("question").isNotBlank()&&d.str("sourceTitle").isNotBlank())
    require(answers.map { it.str("id") }.distinct().size==answers.size&&answers.all { it.str("id").isNotBlank() })
    require(answers.size in 3..4&&answers.any { it.str("id")==d.str("correctAnswerId") })
    require(!d.flag("isDailyQuestion")||Regex("\\d{4}-\\d{2}-\\d{2}").matches(d.str("publicationDate")))
    return d.with("answers" to JsonArray(answers))
}
fun adminSetPayload(d: JsonObject): JsonObject {
    require(d.str("title").isNotBlank()&&d.arr("questionIds").size==10&&d.arr("questionIds").distinct().size==10)
    return d
}

fun adminNotificationPayload(target: String?,title: String,body: String,request: String): JsonObject {
    require(title.trim().length in 3..80&&body.trim().length in 3..500&&request.isNotBlank())
    return json("p_target" to target,"p_title" to title.trim(),"p_body" to body.trim(),"p_request_id" to request)
}
