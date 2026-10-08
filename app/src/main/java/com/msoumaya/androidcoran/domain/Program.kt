package com.msoumaya.androidcoran.domain

import java.time.LocalDate
import java.time.Instant
import kotlinx.serialization.json.*

val paceLabels = linkedMapOf("verse1" to "1 verset","verse2" to "2 versets","verse3" to "3 versets","verse4" to "4 versets","verse5" to "5 versets","halfPage" to "½ page","page" to "1 page","page2" to "2 pages","quarter" to "1 rub‘","halfHizb" to "1 nisf","hizb" to "1 hizb")
fun defaultState() = json("schema" to 1,"onboardingDone" to false,"knowledge" to json(),"goal" to json("label" to "Juz’ ‘Amma","ranges" to listOf(json("start" to 5673,"end" to 6236))),"pace" to "verse3","learningDays" to listOf(1,2,3,4,5),"sessions" to listOf<Any>(),"revisions" to listOf<Any>(),"memorizedAt" to json(),"reviewSettings" to json("enabled" to true,"cycleDays" to 7),"reviewHistory" to listOf<Any>(),"reviewDue" to json(),"difficultyMarkers" to json(),"difficultyHistory" to listOf<Any>(),"reviewCycle" to null,"reviewConsolidations" to json(),"reviewPriorityDue" to json(),"theme" to "white","notifications" to json("messages" to true,"learning" to false),"reader" to json("mushaf" to "coranTest","followAudio" to true),"studyProgress" to json(),"updatedAt" to "1970-01-01T00:00:00.000Z")
fun touch(s: JsonObject) = s.with("updatedAt" to JsonPrimitive(Instant.now().toString()))
fun known(s: JsonObject,id: Int) = s.obj("knowledge").str(id.toString()) in listOf("perfect","review")
fun knownIds(s: JsonObject) = s.obj("knowledge").keys.mapNotNull { it.toIntOrNull() }.filter { known(s,it) && it in 1..6236 }.sorted()
fun range(o: JsonObject) = VerseRange(o.num("start"),o.num("end"))
fun markKnowledge(s: JsonObject,r: VerseRange,mastery: String): JsonObject {
    require(mastery in listOf("perfect","review","learning"))
    val k=s.obj("knowledge").toMutableMap();val dates=s.obj("memorizedAt").toMutableMap();val due=s.obj("reviewDue").toMutableMap()
    r.ids.forEach { val key=it.toString();k[key]=JsonPrimitive(mastery);if(mastery=="learning") { dates.remove(key);due.remove(key) } }
    return touch(s.with("knowledge" to JsonObject(k),"memorizedAt" to JsonObject(dates),"reviewDue" to JsonObject(due)))
}
class Program(private val q: Quran) {
    fun goalIds(s: JsonObject) = s.obj("goal").arr("ranges").flatMap { range(it.jsonObject).ids }.distinct().sorted()
    fun order(s: JsonObject): List<Int> = goalIds(s).let { ids -> if(s.obj("goal").str("direction")=="fromNas") ids.sortedWith(compareByDescending<Int> { q.verse(it).surah }.thenBy { it }) else ids }
    fun progress(s: JsonObject): Pair<Float,Float> { val known=knownIds(s);val target=goalIds(s);return q.volume(known).toFloat()/q.totalVolume to if(target.isEmpty()) 0f else q.volume(target.filter { it in known }).toFloat()/q.volume(target) }
    fun validGoal(ranges: List<VerseRange>): Boolean { val ids=ranges.flatMap { it.ids }.distinct();return q.hizbs.any { ids.containsAll(it.ids) } || q.volume(ids)>=q.totalVolume/60.0 }
    fun chunks(ids: List<Int>,pace: String): List<Int> {
        if(ids.isEmpty()) return emptyList()
        if(pace.startsWith("verse")) return ids.take(pace.removePrefix("verse").toInt().coerceIn(1,5))
        val first=ids.first();val p=q.page(first)
        if(pace=="page2") { val pages=mutableSetOf<Int>();return ids.takeWhile { pages.add(q.page(it));pages.size<=2 } }
        if(pace=="page" || pace=="halfPage") { val within=ids.takeWhile { it in q.pages[p-1].start..q.pages[p-1].end };if(pace=="page") return within;val target=q.volume(q.pages[p-1].ids)/2.0;var sum=0;return within.takeWhile { val include=sum<target;sum+=q.weights[it-1];include } }
        val units=when(pace) { "quarter" -> q.quarters;"halfHizb" -> q.halves;"hizb" -> q.hizbs;else -> error("Unité non vérifiée : $pace") };val r=units.first { first in it.start..it.end };return ids.takeWhile { it in r.start..r.end }
    }
    fun split(ids: List<Int>): List<VerseRange> { val out=mutableListOf<VerseRange>();ids.forEach { id -> val last=out.lastOrNull();if(last!=null && id==last.end+1 && q.verse(id).surah==q.verse(last.start).surah) out[out.lastIndex]=last.copy(end=id) else out+=VerseRange(id,id) };return out }
    fun generate(s: JsonObject,from: LocalDate=LocalDate.now()): JsonObject {
        val partial: (JsonObject)->Boolean = { s.obj("studyProgress").obj("learning:${it.str("id")}").str("status")=="partial" }
        val old=s.arr("sessions").map { it.jsonObject }.filter { partial(it)||it.str("status")!="todo"||it.str("date")<from.toString() }.map { if(it.str("status")=="todo"&&!partial(it)) it.with("status" to JsonPrimitive("postponed")) else it }
        val covered=old.filter { it.str("status")=="done" || partial(it) }.flatMap { range(it).ids }.toSet()
        var remaining=order(s).filter { !known(s,it)&&it !in covered };val sessions=mutableListOf<JsonObject>();var serial=0
        val days=s.arr("learningDays").map { it.jsonPrimitive.int };require(days.isNotEmpty()) { "Choisis au moins un jour" }
        for(offset in 0 until 20000) { if(remaining.isEmpty()) break;val date=from.plusDays(offset.toLong());if(date.dayOfWeek.value%7 !in days) continue;val chunk=chunks(remaining,s.str("pace","verse3"));remaining=remaining.drop(chunk.size);split(chunk).forEach { r -> sessions+=json("id" to "$date-${r.start}-${serial++}","date" to date.toString(),"scheduledDate" to date.toString(),"start" to r.start,"end" to r.end,"unit" to s.str("pace"),"status" to "todo") } }
        return touch(s.with("sessions" to element(old+sessions)))
    }
    fun complete(original: JsonObject,id: String,through: Int,at: LocalDate=LocalDate.now()): JsonObject {
        val session=original.arr("sessions").map { it.jsonObject }.firstOrNull { it.str("id")==id } ?: return original
        val r=range(session);val old=original.obj("studyProgress").obj("learning:$id");val start=maxOf(r.start,old.num("through",r.start-1)+1)
        if(through !in start..r.end || session.str("status")=="done") return original
        val now=Instant.now().toString();val dates=original.obj("memorizedAt").toMutableMap()
        (start..through).forEach { if(!known(original,it)&&it.toString() !in dates) dates[it.toString()]=JsonPrimitive(at.toString()) }
        var s=markKnowledge(original.with("memorizedAt" to JsonObject(dates)),VerseRange(start,through),"perfect")
        val revisionId="r-$start-$through";val revisions=s.arr("revisions").toMutableList();if(revisions.none { it.jsonObject.str("id")==revisionId }) revisions+=json("id" to revisionId,"start" to start,"end" to through,"due" to at.plusDays(1).toString(),"interval" to 1,"streak" to 0,"completedCount" to 0)
        val record=json("id" to id,"mode" to "learning","start" to r.start,"end" to r.end,"through" to through,"page" to q.sourcePage(through,s.obj("reader").str("mushaf","traditional")),"source" to s.obj("reader").str("mushaf","traditional"),"updatedAt" to now,"status" to if(through==r.end) "completed" else "partial","validations" to old.arr("validations")+json("start" to start,"end" to through,"date" to at.toString(),"validatedAt" to now))
        val sessions=s.arr("sessions").map { val v=it.jsonObject;if(v.str("id")==id&&through==r.end) v.with("status" to JsonPrimitive("done"),"completedAt" to JsonPrimitive(now),"completedDate" to JsonPrimitive(at.toString())) else v }
        s=s.with("revisions" to element(revisions),"sessions" to element(sessions),"studyProgress" to s.obj("studyProgress").with("learning:$id" to record))
        return touch(s)
    }
}
