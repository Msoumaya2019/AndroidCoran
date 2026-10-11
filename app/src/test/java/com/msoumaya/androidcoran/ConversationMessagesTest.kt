package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test

class ConversationMessagesTest {
    @Test fun latestSnapshotReplacesDeletedMessageWithoutDroppingOlderHistory() {
        val older=json("id" to "older","created_at" to "2026-10-10T10:00:00Z","body" to "older")
        val original=json("id" to "latest","created_at" to "2026-10-11T10:00:00Z","body" to "original")
        val deleted=original.with("deleted_at" to kotlinx.serialization.json.JsonPrimitive("2026-10-11T11:00:00Z"))
        val rows=mergeConversationMessages(listOf(older,original),listOf(deleted))
        assertEquals(listOf("older","latest"),rows.map { it.str("id") });assertEquals(deleted,rows.last())
    }
    @Test fun olderPageIsSortedAndOverlappingRowsRemainUnique() {
        val latest=json("id" to "2","created_at" to "2026-10-11T10:00:00Z")
        val older=json("id" to "1","created_at" to "2026-10-10T10:00:00Z")
        assertEquals(listOf(older,latest),mergeConversationMessages(listOf(latest),listOf(latest,older)))
    }
    @Test fun emptyRefreshPreservesLoadedHistoryAndInvalidIdsAreDiscarded() {
        val row=json("id" to "1","created_at" to "2026-10-11T10:00:00Z")
        assertEquals(listOf(row),mergeConversationMessages(listOf(row),emptyList()))
        assertTrue(mergeConversationMessages(emptyList(),listOf(json("body" to "invalid"))).isEmpty())
    }
}
