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

 @Test fun metadataOnlyAndNullMarkersDoNotCreatePriorityAndPerfectClearsTheirDue() {
  val base=markKnowledge(defaultState(),VerseRange(1,3),"perfect",at).with("difficultyMarkers" to json("1" to json("note" to "retained"),"2" to json("user" to null,"admin" to null),"3" to json("admin" to json("createdAt" to at.toString()))),"reviewPriorityDue" to json("1" to at.toString(),"2" to at.toString(),"3" to at.toString()))
  assertEquals(listOf(3),review.tasks(base,at).filter { it.category=="priority" }.flatMap { it.range.ids })
  val graded=review.grade(base,ReviewTask("priority-test",VerseRange(1,3),"priority",at.toString()),3,"perfect",at)
  assertNull(graded.obj("reviewPriorityDue")["1"]);assertNull(graded.obj("reviewPriorityDue")["2"])
  assertEquals("2026-10-16",graded.obj("reviewPriorityDue").str("3"));assertEquals(base.obj("difficultyMarkers"),graded.obj("difficultyMarkers"))
 }
 @Test fun hesitantGradeReplacesNullUserMarkerAndPreservesMetadata() {
  val base=markKnowledge(defaultState(),VerseRange(1,1),"perfect",at).with("difficultyMarkers" to json("1" to json("user" to null,"note" to "retained")))
  val graded=review.grade(base,ReviewTask("x",VerseRange(1,1),"priority",at.toString()),1,"hesitant",at)
  assertEquals(at.toString(),graded.obj("difficultyMarkers").obj("1").obj("user").str("createdAt"))
  assertEquals("retained",graded.obj("difficultyMarkers").obj("1").str("note"));assertEquals(1,graded.arr("difficultyHistory").size)
 }

 @Test fun reworkIncludesFutureMarkedPassagesGroupsBySurahAndHonorsDisabledReviews() {
  val base=markKnowledge(defaultState(),VerseRange(1,9),"perfect",at)
  val ids=listOf(2,3,7,8,9)
  val marked=ids.fold(base) { state,id -> review.toggleDifficulty(state,id,at) }.with("reviewPriorityDue" to json(*ids.map { it.toString() to at.plusDays(2).toString() }.toTypedArray()))
  assertTrue(review.tasks(marked,at).none { it.category=="priority" })
  assertEquals(listOf(VerseRange(2,3),VerseRange(7,7),VerseRange(8,9)),review.rework(marked,at).map { it.range })
  assertTrue(review.rework(review.setEnabled(marked,false,at),at).isEmpty())
 }

 @Test fun overduePriorityRetainsOriginalScheduledDateInTaskAndHistory() {
  val base=markKnowledge(defaultState(),VerseRange(1,1),"perfect",at)
  val marked=review.toggleDifficulty(base,1,at.minusDays(2))
  val task=review.tasks(marked,at).single { it.category=="priority" }
  assertEquals(at.minusDays(2).toString(),task.scheduledDate)
  val done=review.grade(marked,task,1,"perfect",at)
  assertEquals(at.minusDays(2).toString(),done.arr("reviewHistory").last().jsonObject.str("scheduledDate"))
  assertEquals(at.toString(),done.arr("reviewHistory").last().jsonObject.str("date"))
 }
}
