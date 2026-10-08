package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CommunityContractsTest {
    @Test fun groupMessageNeverTargetsPrivateConversation() {
        val row=messagePayload(ChatRoom("group",true),"owner"," Bonjour ")
        assertEquals(JsonNull,row["link_id"]);assertEquals("group",row.str("group_id"));assertEquals("Bonjour",row.str("body"))
        val private=messagePayload(ChatRoom("link"),"owner","Courage","encouragement")
        assertEquals(JsonNull,private["group_id"]);assertEquals("link",private.str("link_id"))
    }
    @Test fun sharedRecordingRequiresAnIdentifierAndMessageLimitIsEnforced() {
        assertTrue(runCatching { messagePayload(ChatRoom("link"),"owner","Récitation","recitation") }.isFailure)
        assertTrue(runCatching { messagePayload(ChatRoom("link"),"owner","x".repeat(2001)) }.isFailure)
        assertEquals("recording",messagePayload(ChatRoom("link"),"owner","Récitation","recitation","recording").str("recitation_id"))
    }
    @Test fun invocationUploadPreservesSnapshotAndDoesNotInventQuranVerses() {
        val snapshot=json("id" to "invocation","arabic_text" to "دعاء","french_text" to "Invocation")
        val local=json("id" to "recording","user_id" to "owner","duration_ms" to 1000,"recording_type" to "invocation","invocation_snapshot" to snapshot)
        val remote=recordingPayload(local,"owner","owner/recording.m4a")
        assertEquals(JsonNull,remote["start_verse_id"]);assertEquals(JsonNull,remote["end_verse_id"]);assertEquals(snapshot,remote["invocation_snapshot"])
        assertTrue(runCatching { recordingPayload(local,"other","other/recording.m4a") }.isFailure)
    }
    @Test fun quranUploadRejectsInvalidRange() {
        val local=json("id" to "recording","user_id" to "owner","duration_ms" to 1000,"start_verse_id" to 6231,"end_verse_id" to 6236)
        assertEquals(6231,recordingPayload(local,"owner","owner/recording.m4a").num("start_verse_id"))
        assertTrue(runCatching { recordingPayload(local.with("end_verse_id" to JsonPrimitive(7000)),"owner","owner/recording.m4a") }.isFailure)
    }
}
