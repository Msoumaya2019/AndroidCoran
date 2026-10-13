package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.MushafArchive
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
class MushafArchiveTest {
 private fun fixture(block: (File,File)->Unit) {
  val root=Files.createTempDirectory("mushaf-install").toFile()
  try { block(root,File(root,"archive.zip")) } finally { root.deleteRecursively() }
 }
 private fun zip(file: File,vararg names: String) { ZipOutputStream(file.outputStream()).use { out -> names.forEach { out.putNextEntry(ZipEntry(it));out.write(byteArrayOf(1,2,3));out.closeEntry() } } }
 @Test fun invalidImageDoesNotReplaceExistingLineOrPublishReadiness() = fixture { root,archive ->
  val line=File(root,"001-01.png");line.writeText("previous")
  zip(archive,"width_1440/1/1.png")
  try { MushafArchive(root).install(archive,{false});fail("Invalid image accepted") } catch(_: IllegalArgumentException) {}
  assertEquals("previous",line.readText());assertFalse(File(root,"ready-v1").exists());assertFalse(File(root,"001-01.png.tmp").exists())
 }
 @Test fun cancellationKeepsArchiveAndCleansTemporaryLine() = fixture { root,archive ->
  zip(archive,"width_1440/1/1.png");var calls=0
  try { MushafArchive(root).install(archive,{true},{if(++calls==2) throw CancellationException()});fail("Cancellation swallowed") } catch(_: CancellationException) {}
  assertTrue(archive.exists());assertFalse(File(root,"ready-v1").exists());assertFalse(File(root,"001-01.png.tmp").exists())
 }
 @Test fun completeArchivePublishesReadinessAndIgnoresUnrelatedPaths() = fixture { root,archive ->
  val entries=(1..604).flatMap { page -> (1..15).map { line -> "width_1440/"+page+"/"+line+".png" } }
  zip(archive,*(entries+"../../escape.png").toTypedArray())
  MushafArchive(root).install(archive,{it.readBytes().contentEquals(byteArrayOf(1,2,3))})
  assertEquals("9060",File(root,"ready-v1").readText());assertEquals(9060,root.listFiles()!!.count { it.extension=="png" })
 }
}
