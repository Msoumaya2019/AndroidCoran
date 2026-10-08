package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.LocalDate

data class LearningStatistics(val today: Int,val week: Int,val month: Int,val days: Int,val revisions: Int,val hizbs: Int,val weeklySessions: Int)
data class WeeklyProgress(val start: LocalDate,val end: LocalDate,val total: Int,val done: Int) { val ratio get()=if(total==0) 0f else done.toFloat()/total }
fun learningEvents(state: JsonObject): List<JsonObject> {
    val tracked=state.obj("studyProgress").values.map { it.jsonObject }.filter { it.str("mode")=="learning" }
    return state.arr("sessions").map { it.jsonObject }.filter { row -> row.str("status")=="done"&&tracked.none { it.str("id")==row.str("id") } }.map { it.with("date" to JsonPrimitive(it.str("completedDate",it.str("completedAt",it.str("date")).take(10)))) }+tracked.flatMap { it.arr("validations").map { v->v.jsonObject } }
}
fun activityDates(state: JsonObject): Set<String> = (state.arr("sessions").map { it.jsonObject }.filter { it.str("status")=="done" }.map { it.str("completedDate",it.str("completedAt",it.str("date")).take(10)) }+state.obj("studyProgress").values.map { it.jsonObject }.filter { it.str("mode")=="learning" }.flatMap { it.arr("validations").map { v->v.jsonObject.str("date") } }).toSet()
fun activityStreak(state: JsonObject,at: LocalDate=LocalDate.now()): Int {
    val dates=activityDates(state);var day=if(at.toString() in dates) at else at.minusDays(1);var count=0
    while(day.toString() in dates) { count++;day=day.minusDays(1) };return count
}
data class ProgressBar(val label: String,val count: Int)
fun progressSeries(state: JsonObject,period: String,at: LocalDate=LocalDate.now()): List<ProgressBar> {
    val events=learningEvents(state);val first=when(period) { "Mois" -> at.withDayOfMonth(1);"Jour" -> at.minusDays(6);else -> at.minusDays((at.dayOfWeek.value-1).toLong()) }
    return (0 until if(period=="Mois") 5 else 7).map { i -> val date=first.plusDays((if(period=="Mois") i*7 else i).toLong());val end=if(period=="Mois") date.plusDays(6) else date
        ProgressBar(if(period=="Mois") "S${i+1}" else date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT,java.util.Locale.FRENCH),events.filter { it.str("date") in date.toString()..end.toString() }.sumOf { it.num("end")-it.num("start")+1 }) }
}
fun weeklyProgress(state: JsonObject,at: LocalDate=LocalDate.now()): WeeklyProgress {
    val start=at.minusDays((at.dayOfWeek.value-1).toLong());val end=start.plusDays(6)
    val rows=state.arr("sessions").map { it.jsonObject }.filter { it.str("scheduledDate",it.str("date")) in start.toString()..end.toString() }
    return WeeklyProgress(start,end,rows.size,rows.count { it.str("status")=="done" })
}
fun sessionStatus(state: JsonObject,session: JsonObject): String = when {
    session.str("status")=="done" -> "Terminée"
    session.str("status")=="postponed" -> "Reportée"
    state.obj("studyProgress").obj("learning:${session.str("id")}").str("status")=="partial" -> "Partiellement terminée"
    else -> "À apprendre"
}
fun statistics(state: JsonObject,quran: Quran,at: LocalDate=LocalDate.now()): LearningStatistics {
    val tracked=state.obj("studyProgress").values.map { it.jsonObject }.filter { it.str("mode")=="learning" }
    val done=state.arr("sessions").map { it.jsonObject }.filter { row -> row.str("status")=="done"&&row.str("completedAt").isNotBlank()&&tracked.none { it.str("id")==row.str("id") } }
    val date=at.toString();val week=at.minusDays((at.dayOfWeek.value-1).toLong()).toString();val month=at.withDayOfMonth(1).toString()
    fun dateOf(row: JsonObject)=row.str("completedDate",row.str("completedAt").take(10))
    val validations=tracked.flatMap { it.arr("validations") }.map { it.jsonObject }
    fun count(start: String)=done.filter { dateOf(it) in start..date }.sumOf { it.num("end")-it.num("start")+1 }+validations.filter { it.str("date") in start..date }.sumOf { it.num("end")-it.num("start")+1 }
    val known=knownIds(state).toSet()
    val weeklySessions=done.count { dateOf(it)>=week }+tracked.count { it.str("status")=="completed"&&it.arr("validations").lastOrNull()?.jsonObject?.str("date")?.let { day->day in week..date }==true }
    return LearningStatistics(count(date),count(week),count(month),(done.map(::dateOf)+validations.map { it.str("date") }).distinct().size,state.arr("revisions").sumOf { it.jsonObject.num("completedCount") },quran.hizbs.count { known.containsAll(it.ids) },weeklySessions)
}
