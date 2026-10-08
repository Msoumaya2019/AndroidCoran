package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class ContentContractsTest {
    private val reminder=json("type" to "reminder","category_id" to "category","french_text" to "Texte","source" to "Source")
    @Test fun invocationRequiresArabicAndPhoneticText() { assertEquals(reminder,validateAdminContent(reminder,""));assertTrue(runCatching { validateAdminContent(reminder.with("type" to JsonPrimitive("invocation")),"") }.isFailure) }
    @Test fun calendarDatesAndHttpsMediaAreValidated() { assertTrue(runCatching { validateAdminContent(reminder,"2026-02-30") }.isFailure);assertTrue(runCatching { validateAdminContent(reminder.with("image_url" to JsonPrimitive("http://example.test/photo")),"") }.isFailure);assertEquals(reminder,validateAdminContent(reminder,"2028-02-29")) }
    @Test fun externalFilesCannotBeSelectedForBucketCleanup() { val root="https://backend.test";assertEquals("owner/file.jpg",contentMediaPath("$root/storage/v1/object/public/daily-content-media/owner/file.jpg",root));assertNull(contentMediaPath("https://backend.test.evil/storage/v1/object/public/daily-content-media/file",root));assertNull(contentMediaPath("https://other.test/file.jpg",root)) }
}
