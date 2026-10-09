package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
class StudyProgressTest {
 private fun asset(path: String)=Json.parseToJsonElement(File("src/main/assets/$path").readText())
 private val q=Quran(::asset);private val program=Program(q);private val review=Review(q,program);private val study=StudyProgress(q,program,review)
 private val at=LocalDate.of(2026,10,9)
 private fun learning()=defaultState().with("sessions" to element(listOf(json("id" to "x","start" to 1,"end" to 7,"date" to at.toString(),"status" to "todo"))))
 @Test fun verseAndPageMetricsMatchSourcePartialRules() {
  val short=study.metrics(VerseRange(1,7),3,"traditional")
  assertFalse(short.pages);assertEquals(7,short.total);assertEquals(3,short.done);assertEquals(4,short.remaining);assertEquals(3f/7,short.ratio,0.0001f)
  val range=VerseRange(1,q.pages[2].end);val pages=study.metrics(range,7,"traditional")
  assertTrue(pages.pages);assertEquals(listOf(1,2,3),pages.pageList);assertEquals(1,pages.done);assertEquals(1f/3,pages.ratio,0.0001f)
  assertEquals(1f,study.metrics(range,range.end,"traditional").ratio,0.0001f)
 }
 @Test fun pageChoicesExcludePageThatOnlyBeginsASplitVerse() {
  val split=Quran { path -> val raw=asset(path);if(path=="qcf/verse-index.json") raw.jsonObject.with("1:1" to raw.jsonObject.obj("1:1").with("pages" to element(listOf(1,2)))) else raw }
  val s=StudyProgress(split,Program(split),Review(split,Program(split)))
  assertEquals(listOf(2),s.pages(VerseRange(1,1),"coranTest"))
  assertEquals(1,split.studyEndpoint(2,VerseRange(1,1),"coranTest"))
 }
 @Test fun learningPartialThenCompleteKeepsExactHistoryAndRemainingRevision() {
  val first=study.validate(learning(),"learning","x",VerseRange(1,7),3,"coranTest",at=at)
  assertEquals(VerseRange(4,7),study.remaining(VerseRange(1,7),3))
  assertFalse(known(first,4));assertEquals("partial",first.obj("studyProgress").obj("learning:x").str("status"))
  val done=study.validate(first,"learning","x",VerseRange(1,7),7,"traditional",at=at.plusDays(1))
  val record=done.obj("studyProgress").obj("learning:x")
  assertEquals("completed",record.str("status"));assertEquals(2,record.arr("validations").size)
  assertEquals(4,record.arr("validations").last().jsonObject.num("start"));assertTrue(known(done,7))
  assertEquals(listOf("r-1-3","r-4-7"),done.arr("revisions").map { it.jsonObject.str("id") })
  assertNull(study.remaining(VerseRange(1,7),7))
 }
 @Test fun staleBoundsAndAlreadyValidatedEndpointsNeverAdvanceProgress() {
  val first=study.validate(learning(),"learning","x",VerseRange(1,7),3,"coranTest",at=at)
  assertEquals(first,study.validate(first,"learning","x",VerseRange(1,7),2,"coranTest",at=at))
  assertEquals(first,study.validate(first,"learning","x",VerseRange(2,7),6,"coranTest",at=at))
  assertEquals(first,study.validate(first,"learning","missing",VerseRange(1,7),7,"coranTest",at=at))
  assertEquals(first,study.validate(first,"learning","x",VerseRange(1,7),8,"coranTest",at=at))
 }
 @Test fun revisionResumePreservesOriginalRangeAndUsesSelectedSourcePage() {
  val base=markKnowledge(defaultState(),VerseRange(1,7),"perfect",at)
  val partial=study.validate(base,"revision","r",VerseRange(1,7),3,"coranTest",grade="hesitant",at=at)
  val done=study.validate(partial,"revision","r",VerseRange(1,7),7,"traditional",at=at.plusDays(1))
  val record=done.obj("studyProgress").obj("revision:r")
  assertEquals(1,record.num("start"));assertEquals(7,record.num("end"));assertEquals("traditional",record.str("source"));assertEquals(q.sourcePage(7,"traditional"),record.num("page"))
  assertEquals(4,record.arr("validations").last().jsonObject.num("start"))
  assertEquals(4,done.arr("reviewHistory").last().jsonObject.num("start"))
  assertEquals("completed",record.str("status"))
  assertEquals(partial,study.validate(partial,"revision","r",VerseRange(4,7),7,"coranTest",at=at.plusDays(1)))
 }
 @Test fun sourceSpecificPageIsStoredAndUnknownStateFieldsSurvive() {
  val changed=Quran { path -> val raw=asset(path);if(path=="qcf/verse-index.json") raw.jsonObject.with("1:3" to raw.jsonObject.obj("1:3").with("pages" to element(listOf(2)))) else raw }
  val p=Program(changed);val r=Review(changed,p);val s=StudyProgress(changed,p,r)
  val base=markKnowledge(defaultState(),VerseRange(1,7),"perfect",at).with("futureField" to json("preserved" to true))
  val done=s.validate(base,"revision","r",VerseRange(1,7),3,"coranTest",at=at)
  assertEquals(2,done.obj("studyProgress").obj("revision:r").num("page"));assertEquals(base.obj("futureField"),done.obj("futureField"))
 }
 @Test fun initialEndpointAndPageChoicesStayInsideRemainingRange() {
  assertEquals(7,study.initialEndpoint(VerseRange(1,7),4,604,"traditional"));assertEquals(8,study.initialEndpoint(VerseRange(1,10),7,1,"traditional"));assertNull(study.initialEndpoint(VerseRange(1,7),7,1,"traditional"));assertEquals(listOf(2),study.pages(VerseRange(8,10),"traditional"))
 }
}
