package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
class DifficultyTest {
 private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
 private val review=Review(q,Program(q));private val at=LocalDate.of(2026,10,9)
 @Test fun markedKnownVerseBecomesPriorityWithoutChangingKnowledge() {
  val initial=markKnowledge(defaultState(),VerseRange(1,3),"perfect",at)
  val marked=review.toggleDifficulty(initial,2,at)
  assertEquals(initial.obj("knowledge"),marked.obj("knowledge"))
  assertEquals("2026-10-09",marked.obj("reviewPriorityDue").str("2"))
  assertEquals("marked",marked.arr("difficultyHistory").last().jsonObject.str("action"))
  assertEquals(listOf(2),review.tasks(marked,at).filter { it.category=="priority" }.flatMap { it.range.ids })
  val removed=review.toggleDifficulty(marked,2,at)
  assertTrue(removed.obj("difficultyMarkers").obj("2").isEmpty())
  assertNull(removed.obj("reviewPriorityDue")["2"])
  assertEquals("resolved",removed.arr("difficultyHistory").last().jsonObject.str("action"))
 }
 @Test fun removingOwnMarkerPreservesAdministratorMarkerAndMetadata() {
  val admin=json("createdAt" to "2026-10-01","reason" to "correction")
  val initial=defaultState().with("difficultyMarkers" to json("1" to json("admin" to admin,"futureField" to "retained")))
  val marked=review.toggleDifficulty(initial,1,at)
  val removed=review.toggleDifficulty(marked,1,at)
  assertEquals(initial.obj("difficultyMarkers"),removed.obj("difficultyMarkers"))
  assertEquals(admin,removed.obj("difficultyMarkers").obj("1").obj("admin"))
 }
 @Test fun unknownVerseIsMarkedWithoutInventingLearningOrReviewTask() {
  val marked=review.toggleDifficulty(defaultState(),6236,at)
  assertFalse(known(marked,6236));assertTrue(marked.obj("memorizedAt").isEmpty())
  assertTrue(review.tasks(marked,at).isEmpty())
  assertEquals(defaultState(),review.toggleDifficulty(defaultState(),0,at))
  assertEquals(defaultState(),review.toggleDifficulty(defaultState(),6237,at))
 }
 @Test fun hesitantPartialGradeSchedulesTwoDaysAndRetainsResumeEndpoint() {
  val initial=markKnowledge(defaultState(),VerseRange(1,3),"perfect",at)
  val task=ReviewTask("review-1-3",VerseRange(1,3),"priority",at.toString())
  val graded=review.grade(initial,task,2,"hesitant",at)
  assertEquals("2026-10-11",graded.obj("reviewPriorityDue").str("1"))
  assertEquals("2026-10-11",graded.obj("reviewPriorityDue").str("2"))
  assertNull(graded.obj("reviewPriorityDue")["3"])
  assertEquals("hesitant",graded.arr("reviewHistory").last().jsonObject.str("grade"))
  assertEquals(2,graded.obj("studyProgress").obj("revision:review-1-3").num("through"))
  assertEquals("partial",graded.obj("studyProgress").obj("revision:review-1-3").str("status"))
 }
}
