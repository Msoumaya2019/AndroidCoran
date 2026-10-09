package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class RecitationSharingTest {
 private val link=json("id" to "friend-link","requester_id" to "me","recipient_id" to "friend","status" to "accepted")
 private val clip=json("id" to "clip","user_id" to "me","storage_path" to "me/clip.m4a","recording_type" to "quran","created_at" to "2026-10-09")
 @Test fun shareMatchesSourcePrivateMessageContract() {
  val result=recitationSharePayload("me",link,clip,"Récitation vocale · Al Fâtiha")
  assertEquals("friend-link",result.str("link_id"));assertEquals(JsonNull,result["group_id"]);assertEquals("me",result.str("sender_id"));assertEquals("recitation",result.str("kind"));assertEquals("clip",result.str("recitation_id"));assertEquals("Récitation vocale · Al Fâtiha",result.str("body"))
 }
 @Test fun acceptedFriendsBelongToCurrentAccountInBothDirections() {
  val reverse=link.with("id" to JsonPrimitive("reverse"),"requester_id" to JsonPrimitive("friend2"),"recipient_id" to JsonPrimitive("me"))
  val people=listOf(json("id" to "friend","display_name" to "Samira"),json("id" to "friend2","display_name" to "Nora"))
  val result=acceptedRecitationFriends("me",listOf(link,reverse,link.with("status" to JsonPrimitive("pending")),link.with("requester_id" to JsonPrimitive("other"))),people)
  assertEquals(listOf("Samira","Nora"),result.map { it.name });assertEquals(listOf("friend-link","reverse"),result.map { it.id })
 }
 @Test fun unavailableProfileUsesFriendFallback() { assertEquals("Ami",acceptedRecitationFriends("me",listOf(link),emptyList()).single().name) }
 @Test fun staleFriendAndForeignOwnerCannotShare() {
  listOf("pending","rejected","blocked").forEach { status -> assertThrows(IllegalArgumentException::class.java) { recitationSharePayload("me",link.with("status" to JsonPrimitive(status)),clip,"Description") } }
  assertThrows(IllegalArgumentException::class.java) { recitationSharePayload("other",link,clip,"Description") }
  assertThrows(IllegalArgumentException::class.java) { recitationSharePayload("me",link.with("requester_id" to JsonPrimitive("other")),clip,"Description") }
 }
 @Test fun localDraftAndInvocationAreNotShareableAsQuran() {
  assertThrows(IllegalArgumentException::class.java) { recitationSharePayload("me",link,clip.with("storage_path" to JsonPrimitive("")),"Description") }
  assertThrows(IllegalArgumentException::class.java) { recitationSharePayload("me",link,clip.with("recording_type" to JsonPrimitive("invocation")),"Description") }
 }
 @Test fun descriptionKeepsSourceLimitOfTwoThousandCharacters() { assertEquals(2000,recitationSharePayload("me",link,clip,"a".repeat(2100)).str("body").length) }
 @Test fun libraryCombinesSyncedAndOfflineRecordingsWithoutDuplicates() {
  val local=clip.with("storage_path" to JsonNull,"local_path" to JsonPrimitive("/owned/clip.m4a"),"synced" to JsonPrimitive(false))
  val pending=local.with("id" to JsonPrimitive("pending"),"created_at" to JsonPrimitive("2026-10-10"))
  val result=recitationLibrary("me",listOf(local,pending),listOf(clip))
  assertEquals(listOf("pending","clip"),result.map { it.str("id") });assertFalse(result[0].flag("synced"));assertTrue(result[1].flag("synced"));assertEquals("/owned/clip.m4a",result[1].str("local_path"))
 }
 @Test fun libraryNeverDisplaysRecordingsOfAnotherAccountOrGuest() {
  val foreign=clip.with("user_id" to JsonPrimitive("other"));assertTrue(recitationLibrary("me",listOf(foreign),listOf(foreign)).isEmpty());assertTrue(recitationLibrary(null,listOf(clip),listOf(clip)).isEmpty())
 }
}
