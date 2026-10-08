package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DomainTest {
    private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
    private val p=Program(q);private val review=Review(q,p)
    @Test fun canonicalDatasetHasEveryVerseAndPage() { assertEquals(6236,q.verses.size);assertEquals(114,q.surahs.size);assertEquals(604,q.pages.size);assertEquals(60,q.hizbs.size);assertEquals(1,q.page(1));assertEquals(604,q.page(6236));assertEquals(6236,q.id(114,6));assertTrue(q.french(1).contains("Allah")) }
    @Test fun passageRepetitionStopsExactlyAtLimit() { val range=VerseRange(1,3);var pos: AudioPosition?=AudioPosition(1);val played=mutableListOf<Int>();while(pos!=null) { played+=pos.verseId;pos=nextAudioPosition(range,pos,RepeatMode.PASSAGE,3) };assertEquals(listOf(1,2,3,1,2,3,1,2,3),played) }
    @Test fun eachVerseRepetitionDoesNotSkipVerses() { val range=VerseRange(1,3);var pos: AudioPosition?=AudioPosition(1);val played=mutableListOf<Int>();while(pos!=null) { played+=pos.verseId;pos=nextAudioPosition(range,pos,RepeatMode.EACH_VERSE,3) };assertEquals(listOf(1,1,1,2,2,2,3,3,3),played) }
    @Test fun continuousEachVerseNeverAdvances() { var pos=AudioPosition(1);repeat(100) { pos=nextAudioPosition(VerseRange(1,7),pos,RepeatMode.EACH_VERSE,null)!! };assertEquals(1,pos.verseId);assertEquals(101,pos.repetition) }
    @Test fun fromNasKeepsAyatInAscendingOrderWithinSurah() { val s=defaultState().with("goal" to json("ranges" to listOf(json("start" to 6226,"end" to 6236)),"direction" to "fromNas"));assertEquals((6231..6236).toList()+(6226..6230).toList(),p.order(s)) }
    @Test fun shortPlanUsesSelectedWeekdaysAndRetainsScheduleOnRegeneration() { val date=LocalDate.of(2026,10,8);val s=defaultState().with("goal" to json("ranges" to listOf(json("start" to 6231,"end" to 6236))),"pace" to JsonPrimitive("verse3"),"learningDays" to element(listOf(1,3,5)));val plan=p.generate(s,date);assertEquals("2026-10-09",plan.arr("sessions")[0].jsonObject.str("date"));assertEquals("2026-10-12",plan.arr("sessions")[1].jsonObject.str("date")) }
    @Test fun partialValidationSurvivesRegenerationAndRejectsStaleEndpoint() { val s=defaultState().with("goal" to json("ranges" to listOf(json("start" to 6231,"end" to 6236))),"pace" to JsonPrimitive("verse5"));val plan=p.generate(s,LocalDate.of(2026,10,8));val session=plan.arr("sessions").first().jsonObject;val done=p.complete(plan,session.str("id"),6232,LocalDate.of(2026,10,8));assertTrue(known(done,6231));assertFalse(known(done,6233));assertEquals("partial",done.obj("studyProgress").obj("learning:${session.str("id")}").str("status"));assertEquals(done,p.complete(done,session.str("id"),6231));val regenerated=p.generate(done,LocalDate.of(2026,10,9));assertTrue(regenerated.arr("sessions").any { it.jsonObject.str("id")==session.str("id") }) }
    @Test fun unknownJsonFieldsSurviveNativeEdits() { val s=defaultState().with("futureExpoField" to json("value" to "keep"));assertEquals(s["futureExpoField"],markKnowledge(s,VerseRange(1,1),"perfect")["futureExpoField"]) }
    @Test fun reviewPartitionPreservesEveryVerseExactlyOnce() { val corpus=(1..6236).filter { it%7!=0 };val days=review.partition(corpus,14);assertEquals(14,days.size);assertEquals(corpus,days.flatten());assertEquals(corpus.size,days.flatten().distinct().size) }
    @Test fun sevenWholeHizbOverFourteenDaysBecomeNisf() { val corpus=q.hizbs.take(7).flatMap { it.ids };val days=review.partition(corpus,14);assertEquals(q.halves.take(14).map { it.ids },days) }
    @Test fun newLearningIsConsolidatedBeforeJoiningHabitualCycle() { val at=LocalDate.of(2026,10,8);val s=markKnowledge(defaultState(),VerseRange(1,7),"perfect").with("memorizedAt" to json(*(1..7).map { it.toString() to at.toString() }.toTypedArray()));val prepared=review.prepare(s,at.plusDays(1));assertTrue(prepared.obj("reviewCycle").arr("corpus").isEmpty());assertEquals(7,review.tasks(prepared,at.plusDays(1)).sumOf { it.range.ids.size });val t=review.tasks(prepared,at.plusDays(1)).first();val done=review.grade(prepared,t,t.range.end,"perfect",at.plusDays(1));assertEquals("2026-10-09",done.obj("reviewConsolidations").obj("1").obj("completed").str("1"));assertTrue(review.tasks(done,at.plusDays(1)).isEmpty()) }
}
