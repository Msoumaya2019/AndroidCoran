package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.data.recitationLibrary
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
class RecordingRemovalTest {
 private val row=json("id" to "clip","user_id" to "owner","storage_path" to "owner/clip.m4a")
 @Test fun onlyOwnerCanPlanRemovalForEitherRecordingType() {
  assertEquals(RecordingRemoval("clip","owner/clip.m4a"),recordingRemoval("owner",row))
  assertEquals("clip",recordingRemoval("owner",row.with("recording_type" to JsonPrimitive("invocation"))).id)
  assertThrows(IllegalArgumentException::class.java) { recordingRemoval("foreign",row) }
 }
 @Test fun foreignAndTraversingStoragePathsAreRejectedBeforeDeletion() {
  listOf("foreign/clip.m4a","owner/../foreign/clip.m4a","owner//clip.m4a","owner/./clip.m4a","https://example.test/clip.m4a","owner\\clip.m4a").forEach { path -> assertThrows(IllegalArgumentException::class.java) { recordingRemoval("owner",row.with("storage_path" to JsonPrimitive(path))) } }
 }
 @Test fun offlineRecordingNeedsNoRemotePathAndInvalidIdsAreRejected() {
  assertNull(recordingRemoval("owner",row.with("storage_path" to JsonNull)).storagePath)
  listOf("","..","a/b","a\\b").forEach { id -> assertThrows(IllegalArgumentException::class.java) { recordingRemoval("owner",row.with("id" to JsonPrimitive(id))) } }
 }
 @Test fun localRemovalOnlyTargetsMatchingFileWithinAccountDirectory() {
  val root=Files.createTempDirectory("recording-removal").toFile();val owner=File(root,"owner").apply { mkdir() }
  val file=File(owner,"clip.m4a").apply { writeBytes(byteArrayOf(1)) };val other=File(owner,"other.m4a").apply { writeBytes(byteArrayOf(1)) };val foreign=File(root,"clip.m4a").apply { writeBytes(byteArrayOf(1)) }
  try {
   assertEquals(file.canonicalFile,removableRecordingFile(owner,row.with("local_path" to JsonPrimitive(file.path))))
   assertThrows(IllegalStateException::class.java) { removableRecordingFile(owner,row.with("local_path" to JsonPrimitive(other.path))) }
   assertThrows(IllegalStateException::class.java) { removableRecordingFile(owner,row.with("local_path" to JsonPrimitive(foreign.path))) }
   assertTrue(file.isFile);assertTrue(other.isFile);assertTrue(foreign.isFile)
  } finally { file.delete();other.delete();foreign.delete();owner.delete();root.delete() }
 }
 @Test fun missingLocalFileDoesNotBlockCacheCleanup() { assertNull(removableRecordingFile(File("not-present-owner"),row));assertNull(removableRecordingFile(File("not-present-owner"),row.with("local_path" to JsonPrimitive("not-present-owner/clip.m4a")))) }
 @Test fun removedRecordingCannotReappearFromStaleRemoteCache() {
  val keep=row.with("id" to JsonPrimitive("keep"),"created_at" to JsonPrimitive("2026-10-09"))
  assertEquals(listOf("keep"),recitationLibrary("owner",listOf(row,keep),listOf(row,keep),setOf("clip")).map { it.str("id") })
 }
}
