package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BookmarksTest {
    private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
    private val bookmarks=Bookmarks(q)
    @Test fun translationRetainsOriginalFootnotes() {
        assertTrue(q.frenchNotes(6234).contains("insouciant"))
        assertEquals("",q.frenchNotes(6236))
    }
    @Test fun savingUpdatesResumeAndPreservesEditionPages() {
        val saved=bookmarks.save(defaultState(),6236,"coranTest",604,"2026-10-08T10:00:00Z")
        val again=bookmarks.save(saved,6236,"coran_1441",604,"2026-10-08T11:00:00Z")
        val row=again.obj("bookmarks").obj("6236")
        assertEquals("2026-10-08T10:00:00Z",row.str("createdAt"))
        assertEquals(2,row.obj("sourcePages").size)
        assertEquals(6236,again.obj("lastRead").num("verseId"))
    }
    @Test fun usingBookmarkOrdersByLastUseAndDeletionSurvivesMerge() {
        val first=bookmarks.save(defaultState(),1,"coranTest",1,"2026-10-08T10:00:00Z")
        val second=bookmarks.save(first,6236,"coranTest",604,"2026-10-08T11:00:00Z")
        val used=bookmarks.use(second,1,1,"2026-10-08T12:00:00Z")
        assertEquals(1,bookmarks.visible(used).first().num("verseId"))
        val deleted=bookmarks.delete(used,1,"2026-10-08T13:00:00Z")
        val merged=used.with("bookmarks" to mergeBookmarks(deleted.obj("bookmarks"),second.obj("bookmarks")))
        assertEquals(listOf(6236),bookmarks.visible(merged).map { it.num("verseId") })
        assertEquals(deleted,bookmarks.use(deleted,1,1))
        val restored=bookmarks.save(deleted,1,"coranTest",1,"2026-10-08T14:00:00Z")
        assertEquals(2,bookmarks.visible(restored).size)
    }
}
