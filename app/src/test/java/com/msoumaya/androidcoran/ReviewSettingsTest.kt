package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
class ReviewSettingsTest {
 private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
 private val review=Review(q,Program(q));private val at=LocalDate.of(2026,10,9)
 private fun state()=review.prepare(markKnowledge(defaultState(),VerseRange(1,6236),"perfect",at),at)
 @Test fun quantitiesUseActualDivisionsWithoutLossOrDuplicateVerses() {
  for((quantity,count) in listOf("nisf" to 120,"hizb" to 60,"juz" to 30,"juz2" to 15)) {
   val s=review.setQuantity(state(),quantity,at);val cycle=s.obj("reviewCycle")
   assertEquals(count,cycle.num("lengthDays"));assertEquals((1..6236).toList(),cycle.arr("days").flatMap { it.jsonArray.map { v->v.jsonPrimitive.int } })
   assertEquals(1,s.arr("reviewCycleHistory").size)
  }
 }
 @Test fun switchingQuantityToSameLengthCycleStillCreatesNewSnapshot() {
  val original=review.setQuantity(state(),"juz",at)
  val changed=review.setCycle(original,30,at.plusDays(1))
  assertEquals("cycle",changed.obj("reviewSettings").str("mode"))
  assertEquals(original.obj("reviewCycle").num("index")+1,changed.obj("reviewCycle").num("index"))
  assertEquals(original.obj("reviewCycle"),changed.arr("reviewCycleHistory").last())
  assertEquals("2026-10-10",changed.obj("reviewCycle").str("startDate"))
  assertEquals(changed,review.setCycle(changed,30,at.plusDays(2)))
 }
 @Test fun disablingAndResumingPreservesPendingCycleAndUnknownSettings() {
  val initial=state().with("reviewSettings" to state().obj("reviewSettings").with("futureField" to JsonPrimitive("preserved")))
  val disabled=review.setEnabled(initial,false,at)
  val resumed=review.setEnabled(disabled,true,at.plusDays(2))
  assertEquals(initial.obj("reviewCycle"),resumed.obj("reviewCycle"))
  assertEquals("2026-10-11",resumed.obj("reviewSettings").str("resumedAt"))
  assertEquals("preserved",resumed.obj("reviewSettings").str("futureField"))
  assertEquals(disabled,review.setEnabled(disabled,false,at.plusDays(1)))
 }
 @Test fun changingQuantityWhileDisabledDoesNotReenableReviews() {
  val s=review.setQuantity(review.setEnabled(state(),false,at),"hizb",at)
  assertFalse(s.obj("reviewSettings").flag("enabled",true));assertTrue(review.tasks(s,at).isEmpty())
 }
 @Test fun newCycleTasksAreAvailableBeforeReopeningScreen() {
  val changed=review.setQuantity(state(),"hizb",at)
  assertTrue(changed.obj("reviewCycle").obj("assignments").isEmpty())
  val tasks=review.tasks(changed,at).filter { it.category=="habitual" }
  assertEquals(q.hizbs.first().ids,tasks.flatMap { it.range.ids })
 }
}
