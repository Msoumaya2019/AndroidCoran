package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
class ConsolidationTest {
 private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
 private val review=Review(q,Program(q))
 private val at=LocalDate.of(2026,10,9)
 private fun state()=markKnowledge(defaultState().with("onboardingDone" to JsonPrimitive(true)),VerseRange(1,3),"perfect",at)
 @Test fun earlyValidationPreservesAnchoredDatesAndHistory() {
  val first=review.completeConsolidation(state(),VerseRange(1,3),at,"2026-10-09T12:00:00Z",1)
  assertEquals(3,first.arr("consolidationHistory").size)
  assertEquals("1-2026-10-09-1",first.arr("consolidationHistory")[0].jsonObject.str("id"))
  assertEquals("2026-10-12",first.obj("reviewConsolidations").obj("1").obj("scheduledDates").str("3"))
  assertEquals("2026-10-09",first.obj("reviewConsolidations").obj("1").obj("completed").str("1"))
  assertTrue(first.arr("reviewHistory").isEmpty())
  val retry=review.completeConsolidation(first,VerseRange(1,3),at,targetOffset=1)
  assertEquals(first,retry)
  assertEquals(3,review.consolidations(first).single().steps.first { it.completed==null }.offset)
 }
 @Test fun completionRemovesOnlyFullyConsolidatedRows() {
  var s=state()
  for(offset in listOf(1,3,7)) s=review.completeConsolidation(s,VerseRange(1,2),at,targetOffset=offset)
  assertEquals(6,s.arr("consolidationHistory").size)
  assertEquals(VerseRange(3,3),review.consolidations(s).single().range)
 }
 @Test fun disabledReviewsStillAllowExplicitConsolidationAndPreserveUnknownFields() {
  val initial=state().with("customField" to json("retained" to true),"reviewSettings" to json("enabled" to false))
  val result=review.completeConsolidation(initial,VerseRange(1,4),at,targetOffset=1)
  assertEquals(3,result.arr("consolidationHistory").size)
  assertEquals(initial.obj("customField"),result.obj("customField"))
  assertTrue(review.consolidations(result).isEmpty())
 }
}
