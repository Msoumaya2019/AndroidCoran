package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class RecitationCorrectionTest {
    private val row=json("id" to "recording","recording_type" to "quran","start_verse_id" to 10,"end_verse_id" to 12)
    @Test fun verseBoundsAndInvocationTypeAreEnforced() {
        assertTrue(runCatching { recitationCorrectionPayload(row,"r",mapOf(13 to "x"),"",null) }.isFailure)
        assertTrue(runCatching { recitationCorrectionPayload(row.with("recording_type" to JsonPrimitive("invocation")),"r",emptyMap(),"Texte",null) }.isFailure)
    }
    @Test fun retryIdAndDefaultCommentsMatchSourceContract() {
        val p=recitationCorrectionPayload(row,"same-id",mapOf(11 to " ")," Observation ","feedback/admin/file.m4a")
        assertEquals("same-id",p.str("p_request_id"));assertEquals("À retravailler",p.arr("p_verses").first().jsonObject.str("comment"));assertEquals("Observation",p.str("p_general_comment"));assertEquals(11,p.arr("p_verses").first().jsonObject.num("verseId"))
    }
    @Test fun voiceOnlyCorrectionAllowedButEmptyCorrectionRejected() {
        assertTrue(runCatching { recitationCorrectionPayload(row,"r",emptyMap(),"",null) }.isFailure)
        assertTrue(recitationCorrectionPayload(row,"r",emptyMap(),"","feedback/path").arr("p_verses").isEmpty())
    }
}
