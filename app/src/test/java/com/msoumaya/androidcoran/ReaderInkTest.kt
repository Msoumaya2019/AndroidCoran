package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
class ReaderInkTest {
 private val stroke=InkStroke(listOf(InkPoint(.1f,.2f,.5f),InkPoint(.2f,.3f,1f),InkPoint(.1f,.2f,.8f)))
 @Test fun outlineAndHandwritingRoundTripWithoutTextConversion() {
  val ink=newReaderInk(1,stroke,listOf(1,1,2),"2026-11-18T10:00:00Z").copy(note=listOf(stroke))
  val state=saveReaderInk(defaultState(),ink);val restored=Json.parseToJsonElement(state.toString()).jsonObject
  assertEquals(ink,readerInk(restored,1).single());assertTrue(readerInk(restored,2).isEmpty())
 }
 @Test fun delayedSaveCannotEraseNewerHandwriting() {
  val ink=newReaderInk(1,stroke,listOf(1),"2026-11-18T10:00:00Z")
  val newer=ink.copy(note=listOf(stroke),updatedAt="2026-11-18T10:00:00.001Z")
  val saved=saveReaderInk(defaultState(),newer)
  assertEquals(saved,saveReaderInk(saved,ink))
 }
 @Test fun olderClientWithoutInkCannotRemoveLocalAnnotations() {
  val ink=newReaderInk(1,stroke,listOf(1));val saved=json(ink.id to ink.toJson())
  assertEquals(saved,mergeReaderInkCopies(saved,json()))
 }
 @Test fun durableCopyRetainsExplicitDeletionAgainstRemoteInk() {
  val ink=newReaderInk(1,stroke,listOf(1));val remote=json(ink.id to ink.toJson())
  val deleted=deleteReaderInk(saveReaderInk(defaultState(),ink),ink.id).obj("readerInk")
  assertEquals(deleted,mergeReaderInkCopies(deleted,remote))
 }
 @Test fun deletionSurvivesStaleNoteSave() {
  val ink=newReaderInk(1,stroke,listOf(1));val saved=saveReaderInk(defaultState(),ink)
  val deleted=deleteReaderInk(saved,ink.id);assertEquals(deleted,saveReaderInk(deleted,ink.copy(note=listOf(stroke))))
  assertTrue(readerInk(deleted,1).isEmpty());assertTrue(deleted.obj("readerInk").obj(ink.id).str("deletedAt").isNotEmpty())
 }
 @Test fun simultaneousAnnotationsAreBothPreserved() {
  val base=defaultState().with("userId" to JsonPrimitive("owner"));val a=newReaderInk(1,stroke,listOf(1));val b=newReaderInk(1,stroke,listOf(2))
  val merged=mergeOfflineState(base,saveReaderInk(base,a),saveReaderInk(base,b));assertEquals(setOf(a.id,b.id),readerInk(merged,1).map { it.id }.toSet())
 }
 @Test fun legacyDeletionIsRetainedWhileCanonicalAnnotationsFollowSourceTimestampRules() {
  val ink=newReaderInk(1,stroke,listOf(1),"2026-11-18T10:00:00Z")
  val base=saveReaderInk(defaultState().with("userId" to JsonPrimitive("owner")),ink)
  val edited=saveReaderInk(base,ink.copy(note=listOf(stroke),updatedAt="2026-11-18T10:02:00Z"))
  val deleted=deleteReaderInk(base,ink.id,"2026-11-18T10:01:00Z")
  val merged=mergeOfflineState(base,edited,deleted)
  assertTrue(merged.obj("readerInk").obj(ink.id).str("deletedAt").isNotBlank())
  assertEquals(listOf(stroke),readerInk(merged,1).single().note)
  // The canonical Expo map deliberately uses the newest timestamp, including an explicit undo.
 }
}
