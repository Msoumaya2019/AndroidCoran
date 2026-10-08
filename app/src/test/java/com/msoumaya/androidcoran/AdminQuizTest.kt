package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class AdminQuizTest {
    private fun question()=freshAdminQuestion().with("question" to JsonPrimitive("Question"),"sourceTitle" to JsonPrimitive("Source"),"answers" to JsonArray(listOf("A","B","C","D").map { json("id" to it,"text" to if(it=="D") "" else it) }))
    @Test fun optionalFourthAnswerIsRemovedAndMetadataPreserved() { val q=question().with("arabic" to JsonPrimitive("نص"));val payload=adminQuestionPayload(q);assertEquals(3,payload.arr("answers").size);assertEquals("نص",payload.str("arabic")) }
    @Test fun invalidSolutionDuplicateAnswersAndMissingSourceAreRejected() {
        assertTrue(runCatching { adminQuestionPayload(question().with("correctAnswerId" to JsonPrimitive("D"))) }.isFailure)
        assertTrue(runCatching { adminQuestionPayload(question().with("sourceTitle" to JsonPrimitive(""))) }.isFailure)
        assertTrue(runCatching { adminQuestionPayload(question().with("answers" to JsonArray(List(3) { json("id" to "A","text" to "x") }))) }.isFailure)
    }
    @Test fun themedSetRequiresExactlyTenDistinctQuestions() { val q=json("title" to "Thème","questionIds" to (1..10).map { "$it" });assertEquals(q,adminSetPayload(q));assertTrue(runCatching { adminSetPayload(q.with("questionIds" to JsonArray(List(10) { JsonPrimitive("1") }))) }.isFailure) }
    @Test fun notificationKeepsRetryIdentityTrimsTextAndChecksLengths() {
        val p=adminNotificationPayload(null," Titre "," Texte du rappel ","stable-request")
        assertEquals("stable-request",p.str("p_request_id"));assertEquals("Titre",p.str("p_title"));assertEquals(JsonNull,p["p_target"])
        assertTrue(runCatching { adminNotificationPayload(null,"ab","texte","id") }.isFailure)
        assertTrue(runCatching { adminNotificationPayload(null,"titre","x".repeat(501),"id") }.isFailure)
    }
}
