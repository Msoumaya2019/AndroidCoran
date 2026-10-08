package com.msoumaya.androidcoran.domain

import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.*
import kotlin.math.abs

data class ReviewTask(val id: String,val range: VerseRange,val category: String,val scheduledDate: String,val consolidationOffset: Int?=null)
data class ConsolidationStep(val offset: Int,val due: String,val completed: String?)
data class ConsolidationRow(val range: VerseRange,val learnedAt: String,val steps: List<ConsolidationStep>)
class Review(private val q: Quran,private val program: Program) {
    private val offsets=listOf(1,3,7)
    private fun weight(id: Int) = q.weights[id-1].toDouble()/q.volume(q.pages[q.page(id)-1].ids).coerceAtLeast(1)
    fun partition(corpus: List<Int>,length: Int): List<List<Int>> {
        require(length>0);val ordered=corpus.distinct().sorted();val set=ordered.toSet()
        for(units in listOf(q.hizbs,q.halves,q.quarters)) { val complete=units.filter { set.containsAll(it.ids) };if(complete.size>=length&&complete.size%length==0&&complete.sumOf { it.ids.size }==ordered.size) return complete.chunked(complete.size/length).map { it.flatMap { r -> r.ids } } }
        val prefix=mutableListOf(0.0);ordered.forEach { prefix+=prefix.last()+weight(it) };val total=prefix.last();var cursor=0;val days=mutableListOf<List<Int>>()
        val boundaries=listOf(q.hizbs,q.halves,q.quarters).map { u -> val ends=u.map { it.end }.toSet();ordered.mapIndexedNotNull { i,id -> if(id in ends) i+1 else null } }
        for(day in 1..length) { var end=cursor;if(day==length) end=ordered.size else { val target=total*day/length;while(end<ordered.size&&prefix[end+1]<=target) end++;if(end<ordered.size&&abs(prefix[end+1]-target)<abs(prefix[end]-target)) end++;for(indexes in boundaries) { val candidates=indexes.filter { it>cursor&&abs(prefix[it]-target)<=total/length*.15 };if(candidates.isNotEmpty()) { end=candidates.minBy { abs(prefix[it]-target) };break } } };days+=ordered.subList(cursor,end).toList();cursor=end }
        return days
    }
    fun partitionQuantity(corpus: List<Int>,quantity: String): List<List<Int>> { val units=when(quantity) { "nisf" -> q.halves;"hizb" -> q.hizbs;else -> q.juzs };val groups=units.map { r -> corpus.filter { it in r.start..r.end } }.filter { it.isNotEmpty() };return if(quantity=="juz2") groups.chunked(2).map { it.flatten() } else groups }
    private fun consolidation(s: JsonObject,id: Int): JsonObject? {
        val date=s.obj("memorizedAt").str(id.toString());if(date.isEmpty()) return null
        val stored=s.obj("reviewConsolidations").obj(id.toString());val learned=LocalDate.parse(date)
        val dates=json(*offsets.map { it.toString() to learned.plusDays(it.toLong()).toString() }.toTypedArray())
        if(stored.str("learnedAt")==date) return if(stored["scheduledDates"]!=null) stored else stored.with("scheduledDates" to dates)
        val completed=mutableMapOf<String,JsonElement>();var previous=""
        val events=s.arr("reviewHistory").map { it.jsonObject }.filter { id in it.num("start")..it.num("end") && it.str("date")>=date }.sortedBy { it.str("date") }
        for(offset in offsets) { val event=events.firstOrNull { it.str("date")>=learned.plusDays(offset.toLong()).toString()&&it.str("date")>previous } ?: break;previous=event.str("date");completed[offset.toString()]=JsonPrimitive(previous) }
        return json("learnedAt" to date,"scheduledDates" to dates,"completed" to JsonObject(completed))
    }
    fun consolidations(s: JsonObject): List<ConsolidationRow> {
        if(!s.obj("reviewSettings").flag("enabled",true)) return emptyList()
        val rows=mutableListOf<ConsolidationRow>()
        knownIds(s).sorted().forEach { id ->
            val c=consolidation(s,id) ?: return@forEach
            if(c.obj("completed")["7"]!=null) return@forEach
            val steps=offsets.map { ConsolidationStep(it,c.obj("scheduledDates").str(it.toString()),c.obj("completed").str(it.toString()).ifEmpty { null }) }
            val last=rows.lastOrNull()
            if(last!=null&&last.range.end+1==id&&q.verse(last.range.start).surah==q.verse(id).surah&&last.learnedAt==c.str("learnedAt")&&last.steps==steps) rows[rows.lastIndex]=last.copy(range=VerseRange(last.range.start,id))
            else rows+=ConsolidationRow(VerseRange(id,id),c.str("learnedAt"),steps)
        }
        return rows
    }
    fun completeConsolidation(original: JsonObject,range: VerseRange,at: LocalDate=LocalDate.now(),completedAt: String=Instant.now().toString(),targetOffset: Int?=null): JsonObject {
        require(targetOffset==null||targetOffset in offsets)
        val s=prepare(original,at);val records=s.obj("reviewConsolidations").toMutableMap();val events=s.arr("consolidationHistory").toMutableList()
        range.ids.filter { known(s,it) }.forEach { id ->
            val c=consolidation(s,id) ?: return@forEach
            val offset=offsets.firstOrNull { c.obj("completed")[it.toString()]==null } ?: return@forEach
            if(targetOffset!=null&&targetOffset!=offset) return@forEach
            records[id.toString()]=c.with("completed" to c.obj("completed").with(offset.toString() to JsonPrimitive(at.toString())),"completedAt" to c.obj("completedAt").with(offset.toString() to JsonPrimitive(completedAt)))
            events+=json("id" to "$id-${c.str("learnedAt") }-$offset","verseId" to id,"offset" to offset,"learnedAt" to c.str("learnedAt"),"scheduledDate" to c.obj("scheduledDates").str(offset.toString()),"completedAt" to completedAt)
        }
        return if(events==s.arr("consolidationHistory")) s else touch(s.with("reviewConsolidations" to JsonObject(records),"consolidationHistory" to element(events)))
    }
    private fun createCycle(s: JsonObject,at: LocalDate,index: Int): JsonObject {
        val start=s.str("reviewModelStartedAt",at.toString())
        val corpus=knownIds(s).filter { id -> val date=s.obj("memorizedAt").str(id.toString());date.isEmpty()||(date<start&&ChronoUnit.DAYS.between(LocalDate.parse(date),LocalDate.parse(start))>=7)||s.obj("reviewConsolidations").obj(id.toString()).obj("completed")["7"]!=null }
        val settings=s.obj("reviewSettings");val quantity=settings.str("mode")=="quantity";val days=if(quantity) partitionQuantity(corpus,settings.str("dailyQuantity","hizb")) else partition(corpus,settings.num("cycleDays",7))
        return json("index" to index,"startDate" to at.toString(),"lengthDays" to if(quantity) days.size.coerceAtLeast(1) else settings.num("cycleDays",7),"corpus" to corpus,"days" to days,"completed" to emptyList<Any>(),"assignments" to json())
    }
    fun setEnabled(s: JsonObject,enabled: Boolean,at: LocalDate=LocalDate.now()): JsonObject {
        if(s.obj("reviewSettings").flag("enabled",true)==enabled) return s
        var settings=s.obj("reviewSettings").with("enabled" to JsonPrimitive(enabled),"cycleDays" to JsonPrimitive(s.obj("reviewSettings").num("cycleDays",7)))
        if(enabled) settings=settings.with("resumedAt" to JsonPrimitive(at.toString()))
        return touch(s.with("reviewSettings" to settings))
    }
    private fun replaceCycle(s: JsonObject,settings: JsonObject,at: LocalDate): JsonObject {
        val old=s.obj("reviewCycle");var next=s.with("reviewSettings" to settings)
        if(old.isNotEmpty()) next=next.with("reviewCycleHistory" to element(s.arr("reviewCycleHistory")+old))
        return touch(next.with("reviewCycle" to createCycle(next,at,old.num("index")+1)))
    }
    fun setCycle(s: JsonObject,days: Int,at: LocalDate=LocalDate.now()): JsonObject {
        require(days in listOf(7,14,21,30))
        val settings=s.obj("reviewSettings")
        if(settings.num("cycleDays",7)==days&&settings.str("mode")!="quantity") return s
        return replaceCycle(s,settings.with("enabled" to JsonPrimitive(settings.flag("enabled",true)),"cycleDays" to JsonPrimitive(days),"mode" to JsonPrimitive("cycle")),at)
    }
    fun setQuantity(s: JsonObject,quantity: String,at: LocalDate=LocalDate.now()): JsonObject {
        require(quantity in listOf("nisf","hizb","juz","juz2"))
        val settings=s.obj("reviewSettings")
        return replaceCycle(s,settings.with("enabled" to JsonPrimitive(settings.flag("enabled",true)),"cycleDays" to JsonPrimitive(settings.num("cycleDays",7)),"mode" to JsonPrimitive("quantity"),"dailyQuantity" to JsonPrimitive(quantity)),at)
    }
    fun prepare(original: JsonObject,at: LocalDate=LocalDate.now()): JsonObject {
        if(!original.obj("reviewSettings").flag("enabled",true)) return original
        val old=original.obj("reviewCycle");var cycle=old
        if(cycle.isEmpty()||(original.obj("reviewSettings").str("mode")!="quantity"&&cycle.num("lengthDays")!=original.obj("reviewSettings").num("cycleDays",7))) cycle=createCycle(original,at,old.num("index")+1)
        val done=cycle.arr("completed").map { it.jsonPrimitive.int }.toSet()
        if(cycle.arr("corpus").all { it.jsonPrimitive.int in done||!known(original,it.jsonPrimitive.int) }&&at>=LocalDate.parse(cycle.str("startDate")).plusDays(cycle.num("lengthDays").toLong())) cycle=createCycle(original,at,cycle.num("index")+1)
        if(cycle.obj("assignments")[at.toString()]==null) { val start=LocalDate.parse(cycle.str("startDate"));val completed=cycle.arr("completed").map { it.jsonPrimitive.int }.toSet();val i=cycle.arr("days").indexOfFirst { d -> val idx=cycle.arr("days").indexOf(d);start.plusDays(idx.toLong())<=at && d.jsonArray.any { known(original,it.jsonPrimitive.int)&&it.jsonPrimitive.int !in completed } };cycle=cycle.with("assignments" to cycle.obj("assignments").with(at.toString() to JsonPrimitive(i))) }
        val consolidations=original.obj("reviewConsolidations").toMutableMap();knownIds(original).forEach { consolidation(original,it)?.let { c -> consolidations[it.toString()]=c } }
        var result=original.with("reviewModelStartedAt" to JsonPrimitive(original.str("reviewModelStartedAt",at.toString())),"reviewCycle" to cycle,"reviewConsolidations" to JsonObject(consolidations))
        if(old.isNotEmpty()&&cycle.num("index")!=old.num("index")) result=result.with("reviewCycleHistory" to element(original.arr("reviewCycleHistory")+old))
        return if(result==original) original else touch(result)
    }
    fun tasks(original: JsonObject,at: LocalDate=LocalDate.now()): List<ReviewTask> {
        val s=prepare(original,at)
        if(!s.obj("reviewSettings").flag("enabled",true)) return emptyList()
        val today=at.toString();val reviewed=s.arr("reviewHistory").map { it.jsonObject }.filter { it.str("date")==today }.flatMap { range(it).ids }.toSet();val seen=reviewed.toMutableSet();val tasks=mutableListOf<ReviewTask>()
        fun add(ids: List<Int>,category: String,date: String=today,id: String?=null) { program.split(ids.distinct().sorted().filter { known(s,it)&&seen.add(it) }).forEach { r -> tasks+=ReviewTask(id?:"$category-${r.start}-${r.end}",r,category,date) } }
        s.obj("studyProgress").values.map { it.jsonObject }.filter { it.str("mode")=="revision"&&it.str("status")=="partial" }.forEach { add((it.num("through")+1..it.num("end")).toList(),it.str("category","habitual"),id=it.str("id")) }
        knownIds(s).groupBy { s.obj("reviewConsolidations").obj(it.toString()) }.forEach { (c,ids) -> val offset=offsets.firstOrNull { c.obj("completed")[it.toString()]==null };if(c.isNotEmpty()&&offset!=null) { val due=c.obj("scheduledDates").str(offset.toString(),LocalDate.parse(c.str("learnedAt")).plusDays(offset.toLong()).toString());if(due<=today) add(ids,"recent",due) } }
        add(knownIds(s).filter { s.obj("difficultyMarkers").obj(it.toString()).isNotEmpty()&&s.obj("reviewPriorityDue").str(it.toString(),today)<=today },"priority")
        val cycle=s.obj("reviewCycle");val idx=cycle.obj("assignments").num(today,-1);val completed=cycle.arr("completed").map { it.jsonPrimitive.int }.toSet();if(idx>=0) add(cycle.arr("days").getOrNull(idx)?.jsonArray?.map { it.jsonPrimitive.int }?.filter { it !in completed } ?: emptyList(),"habitual",LocalDate.parse(cycle.str("startDate")).plusDays(idx.toLong()).toString())
        return tasks
    }
    fun grade(original: JsonObject,task: ReviewTask,through: Int,grade: String,at: LocalDate=LocalDate.now()): JsonObject {
        require(grade in listOf("perfect","hesitant","rework"));if(!original.obj("reviewSettings").flag("enabled",true)||through !in task.range.start..task.range.end) return original
        val s=prepare(original,at);val today=at.toString();val reviewed=s.arr("reviewHistory").map { it.jsonObject }.filter { it.str("date")==today }.flatMap { range(it).ids }.toSet();val ids=(task.range.start..through).filter { known(s,it)&&it !in reviewed };if(ids.isEmpty()) return s
        val now=Instant.now().toString();val markers=s.obj("difficultyMarkers").toMutableMap();val due=s.obj("reviewPriorityDue").toMutableMap();val history=s.arr("difficultyHistory").toMutableList();val consolidations=s.obj("reviewConsolidations").toMutableMap();val cycle=s.obj("reviewCycle");val completed=cycle.arr("completed").map { it.jsonPrimitive.int }.toMutableSet();val assigned=cycle.arr("days").getOrNull(cycle.obj("assignments").num(today,-1))?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
        ids.forEach { id -> val key=id.toString();if(id in assigned || task.category=="habitual"&&cycle.arr("corpus").any { it.jsonPrimitive.int==id }) completed+=id
            val c=s.obj("reviewConsolidations").obj(key);if(c.isNotEmpty()) { val offset=offsets.firstOrNull { c.obj("completed")[it.toString()]==null };if(offset!=null&&LocalDate.parse(c.str("learnedAt")).plusDays(offset.toLong())<=at) consolidations[key]=c.with("completed" to c.obj("completed").with(offset.toString() to JsonPrimitive(today)),"completedAt" to c.obj("completedAt").with(offset.toString() to JsonPrimitive(now))) }
            val m=(markers[key] as? JsonObject)?:json();if(grade!="perfect") { if(m["user"]==null) { markers[key]=m.with("user" to json("createdAt" to today));history+=json("verseId" to id,"date" to today,"origin" to "user","action" to "marked") };due[key]=JsonPrimitive(at.plusDays(if(grade=="rework") 1 else 2).toString()) } else if(m.isNotEmpty()) due[key]=JsonPrimitive(at.plusDays(s.obj("reviewSettings").num("cycleDays",7).toLong()).toString()) else due.remove(key)
        }
        val event=json("id" to "$today-${task.category}-${task.range.start}-$through-${System.currentTimeMillis()}","date" to today,"scheduledDate" to task.scheduledDate,"completedAt" to now,"start" to task.range.start,"end" to through,"category" to task.category,"grade" to grade)
        val old=s.obj("studyProgress").obj("revision:${task.id}");val record=json("id" to task.id,"mode" to "revision","start" to old.num("start",task.range.start),"end" to task.range.end,"category" to task.category,"through" to through,"page" to q.page(through),"source" to s.obj("reader").str("mushaf"),"updatedAt" to now,"status" to if(through==task.range.end) "completed" else "partial","validations" to old.arr("validations")+json("start" to ids.first(),"end" to through,"date" to today,"validatedAt" to now))
        return touch(s.with("reviewCycle" to cycle.with("completed" to element(completed.sorted())),"reviewConsolidations" to JsonObject(consolidations),"reviewPriorityDue" to JsonObject(due),"difficultyMarkers" to JsonObject(markers),"difficultyHistory" to element(history),"reviewHistory" to element(s.arr("reviewHistory")+event),"studyProgress" to s.obj("studyProgress").with("revision:${task.id}" to record)))
    }
}
