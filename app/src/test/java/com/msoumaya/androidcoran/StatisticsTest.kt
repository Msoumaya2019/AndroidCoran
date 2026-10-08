package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class StatisticsTest {
    private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
    @Test fun partialAndLegacyEventsAreCountedWithoutDuplicatingCompletedSession() {
        val at=LocalDate.of(2026,10,8)
        val sessions=listOf(json("id" to "tracked","start" to 1,"end" to 7,"status" to "done","completedAt" to "2026-10-08T12:00:00Z"),json("id" to "legacy","start" to 8,"end" to 10,"status" to "done","completedAt" to "2026-10-07T12:00:00Z"))
        val tracked=json("id" to "tracked","mode" to "learning","status" to "completed","validations" to listOf(json("start" to 1,"end" to 3,"date" to "2026-10-07"),json("start" to 4,"end" to 7,"date" to "2026-10-08")))
        val state=defaultState().with("sessions" to element(sessions),"studyProgress" to json("learning:tracked" to tracked))
        val stats=statistics(state,q,at)
        assertEquals(4,stats.today);assertEquals(10,stats.week);assertEquals(10,stats.month);assertEquals(2,stats.weeklySessions)
        assertEquals(10,progressSeries(state,"Semaine",at).sumOf { it.count });assertEquals(2,activityStreak(state,at))
    }
    @Test fun weekUsesOriginalScheduleAcrossDaylightSavingBoundary() {
        val state=defaultState().with("sessions" to element(listOf(json("id" to "x","scheduledDate" to "2026-10-25","date" to "2026-10-27","status" to "done"))))
        val week=weeklyProgress(state,LocalDate.of(2026,10,25));assertEquals(LocalDate.of(2026,10,19),week.start);assertEquals(1,week.total);assertEquals(1f,week.ratio)
    }
    @Test fun shortLegacyPlanExtendsWithoutMovingExistingDates() {
        val p=Program(q);val first=json("id" to "existing","start" to 6231,"end" to 6232,"scheduledDate" to "2026-10-08","date" to "2026-10-08","status" to "todo")
        val state=defaultState().with("goal" to json("ranges" to listOf(json("start" to 6231,"end" to 6236))),"sessions" to element(listOf(first)),"learningDays" to element(listOf(0,1,2,3,4,5,6)))
        val extended=p.extend(state,LocalDate.of(2026,10,8));assertEquals(first,extended.arr("sessions").first());assertEquals(6233,extended.arr("sessions")[1].jsonObject.num("start"));assertEquals("2026-10-09",extended.arr("sessions")[1].jsonObject.str("date"))
        assertEquals(extended,p.extend(extended))
    }
    @Test fun newlyDeclaredKnowledgeStartsConsolidationOnlyAfterOnboarding() {
        val at=LocalDate.of(2026,10,8);assertTrue(markKnowledge(defaultState(),VerseRange(1,1),"perfect",at).obj("memorizedAt").isEmpty())
        val declared=markKnowledge(defaultState().with("onboardingDone" to JsonPrimitive(true)),VerseRange(1,1),"review",at)
        assertEquals("2026-10-08",declared.obj("memorizedAt").str("1"));assertTrue(markKnowledge(declared,VerseRange(1,1),"learning",at).obj("memorizedAt").isEmpty())
    }
    @Test fun pageValidationDoesNotFinishAVerseThatContinuesOnNextPage() {
        // This edition has no split-page ayah; exercise the source rule with an index fixture.
        val split=Quran { path -> val raw=Json.parseToJsonElement(File("src/main/assets/$path").readText());if(path=="qcf/verse-index.json") raw.jsonObject.with("1:1" to raw.jsonObject.obj("1:1").with("pages" to element(listOf(1,2)))) else raw }
        assertEquals(0,split.studyEndpoint(1,VerseRange(1,1),"coranTest"))
        assertEquals(1,split.studyEndpoint(2,VerseRange(1,1),"coranTest"))
    }
}
