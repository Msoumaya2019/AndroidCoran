package com.msoumaya.androidcoran.domain
import java.time.LocalDate
import java.time.Instant
import kotlinx.serialization.json.*
data class StudyMetrics(val first: Int,val last: Int,val pages: Boolean,val pageList: List<Int>,val total: Int,val done: Int,val remaining: Int,val ratio: Float,val label: String) { val unit get()=if(pages) "pages" else "versets" }
class StudyProgress(private val q: Quran,private val program: Program,private val review: Review) {
 fun remaining(range: VerseRange,through: Int): VerseRange? { val next=maxOf(range.start,through+1);return if(next<=range.end) VerseRange(next,range.end) else null }
 fun metrics(range: VerseRange,through: Int,source: String): StudyMetrics {
  val first=q.sourcePage(range.start,source);val last=q.sourceLastPage(range.end,source);val pages=last>first;val list=(first..last).toList()
  val total=if(pages) list.size else range.ids.size
  val done=if(pages) list.count { minOf(range.end,q.sourceRange(it,source).end)<=through } else (through-range.start+1).coerceIn(0,total)
  val ratio=if(pages) list.sumOf { p -> val r=q.sourceRange(p,source);val start=maxOf(range.start,r.start);val end=minOf(range.end,r.end);((through-start+1).toDouble()/(end-start+1)).coerceIn(0.0,1.0) }.toFloat()/total else ((through-range.start+1).toFloat()/total).coerceIn(0f,1f)
  return StudyMetrics(first,last,pages,list,total,done,total-done,ratio,if(pages) "Pages $first à $last" else q.reference(range))
 }
 fun pages(allowed: VerseRange,source: String): List<Int> = (q.sourcePage(allowed.start,source)..q.sourceLastPage(allowed.end,source)).filter { q.studyEndpoint(it,allowed,source)>=allowed.start }
 fun initialEndpoint(range: VerseRange,through: Int,currentPage: Int,source: String): Int? = remaining(range,through)?.let { maxOf(it.start,minOf(it.end,q.sourceRange(currentPage,source).end)) }
 fun validate(s: JsonObject,mode: String,id: String,planned: VerseRange,through: Int,source: String,category: String="habitual",grade: String="perfect",at: LocalDate=LocalDate.now()): JsonObject {
  require(mode in listOf("learning","revision"));require(grade in listOf("perfect","hesitant","rework"))
  val key="$mode:$id";val old=s.obj("studyProgress").obj(key)
  if(through !in planned.start..planned.end||old.isNotEmpty()&&(old.num("start")!=planned.start||old.num("end")!=planned.end)) return s
  val start=maxOf(planned.start,old.num("through",planned.start-1)+1);if(through<start) return s
  val next=if(mode=="learning") {
   val session=s.arr("sessions").map { it.jsonObject }.firstOrNull { it.str("id")==id } ?: return s
   if(session.num("start")!=planned.start||session.num("end")!=planned.end||session.str("status")=="done") return s
   program.complete(s,id,through,at)
  } else review.grade(s,ReviewTask(id,VerseRange(start,planned.end),category,at.toString()),through,grade,at)
  val now=Instant.now().toString()
  val record=json("id" to id,"mode" to mode,"start" to planned.start,"end" to planned.end,"through" to through,"page" to q.sourcePage(through,source),"source" to source,"updatedAt" to now,"status" to if(through==planned.end) "completed" else "partial","validations" to old.arr("validations")+json("start" to start,"end" to through,"date" to at.toString(),"validatedAt" to now))
  return touch(next.with("studyProgress" to next.obj("studyProgress").with(key to if(mode=="revision") record.with("category" to JsonPrimitive(category)) else record)))
 }
}
