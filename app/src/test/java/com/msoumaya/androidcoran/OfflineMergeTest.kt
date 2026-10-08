package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class OfflineMergeTest {
    private val now=Instant.parse("2026-10-08T14:00:00Z")
    private val base=defaultState().with("userId" to JsonPrimitive("a"),"updatedAt" to JsonPrimitive("2026-10-08T10:00:00Z"))
    @Test fun independentEditsAndUnknownFieldsArePreserved() {
        val local=base.with("profile" to json("firstName" to "Android"))
        val remote=base.with("theme" to JsonPrimitive("lilac"),"futureField" to json("keep" to true))
        val merged=mergeOfflineState(base,local,remote,now)
        assertEquals("Android",merged.obj("profile").str("firstName"));assertEquals("lilac",merged.str("theme"));assertEquals(remote["futureField"],merged["futureField"])
        assertEquals("2026-10-08T14:00:00.000Z",merged.str("updatedAt"))
    }
    @Test fun progressUsesFurthestEndpointAndUnionsHistory() {
        val b=base.with("studyProgress" to json("learning:x" to json("through" to 1,"end" to 7,"status" to "partial")),"reviewHistory" to element(listOf(json("date" to "old"))))
        val local=b.with("studyProgress" to json("learning:x" to json("through" to 3,"end" to 7,"status" to "partial")),"reviewHistory" to element(listOf(json("date" to "old"),json("date" to "local"))),"readPages" to element(listOf(2,1)))
        val remote=b.with("studyProgress" to json("learning:x" to json("through" to 7,"end" to 7,"status" to "completed")),"reviewHistory" to element(listOf(json("date" to "old"),json("date" to "remote"))),"readPages" to element(listOf(3,1)))
        val merged=mergeOfflineState(b,local,remote,now)
        assertEquals(7,merged.obj("studyProgress").obj("learning:x").num("through"))
        assertEquals("completed",merged.obj("studyProgress").obj("learning:x").str("status"))
        assertEquals(3,merged.arr("reviewHistory").size);assertEquals(element(listOf(1,2,3)),merged["readPages"])
    }
    @Test fun completedSessionSurvivesLocalRemovalAndLocalDeletionWins() {
        val b=base.with("sessions" to element(listOf(json("id" to "x","status" to "pending"))),"profile" to json("firstName" to "old"))
        val local=JsonObject(b.toMutableMap().apply { remove("profile");put("sessions",JsonArray(emptyList())) })
        val remote=b.with("sessions" to element(listOf(json("id" to "x","status" to "done"))),"profile" to json("firstName" to "remote"))
        val merged=mergeOfflineState(b,local,remote,now)
        assertEquals("done",merged.arr("sessions").first().jsonObject.str("status"))
        assertTrue(merged.obj("profile").isEmpty())
    }
    @Test fun explicitResetAndOtherAccountsAreNeverMerged() {
        val initialized=base.with("onboardingDone" to JsonPrimitive(true))
        val reset=base.with("theme" to JsonPrimitive("white"))
        assertEquals(reset,mergeOfflineState(initialized,reset,initialized.with("theme" to JsonPrimitive("lilac")),now))
        assertEquals(base,mergeOfflineState(base,base,base.with("userId" to JsonPrimitive("b")),now))
    }
}
