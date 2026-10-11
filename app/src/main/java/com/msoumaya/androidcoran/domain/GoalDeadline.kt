package com.msoumaya.androidcoran.domain
import java.time.LocalDate
import kotlinx.serialization.json.*
fun validGoalDeadline(value: String): Boolean = value.isEmpty() || (Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value) && runCatching { LocalDate.parse(value) }.isSuccess)
fun withGoalDeadline(goal: JsonObject,value: String): JsonObject {
    require(validGoalDeadline(value)) { "Indique une date valide au format AAAA-MM-JJ." }
    return if(value.isEmpty()) JsonObject(goal.filterKeys { it!="deadline" }) else goal.with("deadline" to JsonPrimitive(value))
}
