package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class QuizTest {
    private val question=json("id" to "q","publicationDate" to "2026-10-08","answers" to listOf(json("id" to "a","text" to "A"),json("id" to "b","text" to "B")))
    @Test fun offlineAnswerIsRecordedOnceAndDoesNotRevealCorrectness() {
        val first=recordDailyAnswer(emptyQuiz("2026-10-08"),question,"a","2026-10-08","2026-10-08T12:00:00Z")
        assertTrue(first.arr("responses").first().jsonObject.flag("pending"));assertNull(first.arr("responses").first().jsonObject["isCorrect"])
        assertEquals(first,recordDailyAnswer(first,question,"b","2026-10-08","2026-10-08T13:00:00Z"))
    }
    @Test fun invalidAnswerAndWrongPublicationDateDoNotProduceAResponse() {
        assertTrue(runCatching { recordDailyAnswer(emptyQuiz(),question,"unknown","2026-10-08","now") }.isFailure)
        assertTrue(runCatching { recordDailyAnswer(emptyQuiz(),question,"a","2026-10-09","now") }.isFailure)
    }
    @Test fun serverConfirmationReplacesPendingResponseAndOtherDaysSurvive() {
        val local=recordDailyAnswer(emptyQuiz(),question,"a","2026-10-08","2026-10-08T12:00:00Z")
        val confirmed=json("day" to "2026-10-08","selectedAnswerId" to "a","isCorrect" to true)
        val remote=emptyQuiz().with("responses" to element(listOf(confirmed,json("day" to "2026-10-07","isCorrect" to false))))
        val merged=mergeQuizSnapshot(remote,local)
        assertEquals(2,merged.arr("responses").size);assertEquals(confirmed,merged.arr("responses").first());assertFalse(merged.arr("responses").first().jsonObject.flag("pending"))
        assertEquals(1,mergeQuizSnapshot(emptyQuiz(),local).arr("responses").size)
    }
    @Test fun expiredChallengeNeverCountsAsAWinAndPendingAnswersAreExcludedFromStats() {
        val answers=listOf(json("userId" to "me","isCorrect" to true),json("userId" to "friend","isCorrect" to false))
        val completed=json("status" to "completed","answers" to answers)
        val expired=json("status" to "expired","answers" to answers,"expiresAt" to "2026-10-08T11:00:00Z","questionCount" to 5)
        val snapshot=emptyQuiz().with("responses" to element(listOf(json("pending" to true,"isCorrect" to true),json("isCorrect" to true),json("isCorrect" to false))),"challenges" to element(listOf(completed,expired)))
        val stats=quizStatistics(snapshot,"me");assertEquals(2,stats.total);assertEquals(50,stats.rate);assertEquals(1,stats.played);assertEquals(1,stats.wins)
        assertEquals("Expiré",challengeStatus(expired,"me",Instant.parse("2026-10-08T12:00:00Z")))
    }
    @Test fun challengeBecomesWaitingAfterAllOwnAnswers() {
        val challenge=json("status" to "pending","expiresAt" to "2026-10-10T12:00:00Z","questionCount" to 5,"answers" to (1..5).map { json("userId" to "me","questionId" to it.toString()) })
        assertEquals("En attente de l’ami",challengeStatus(challenge,"me",Instant.parse("2026-10-08T12:00:00Z")))
    }
}
