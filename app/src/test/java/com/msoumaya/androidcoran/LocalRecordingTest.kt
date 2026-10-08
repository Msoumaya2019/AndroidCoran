package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File
class LocalRecordingTest {
    @Test fun onlyExistingFilesInsideOwnedRecordingDirectoryCanPlay() {
        val root=Files.createTempDirectory("native-recording-test").toFile()
        try { val owner=File(root,"owner").apply { mkdir() };val a=File(owner,"clip.m4a").apply { writeBytes(byteArrayOf(1)) };val foreign=File(root,"foreign.m4a").apply { writeBytes(byteArrayOf(1)) };assertEquals(a.canonicalFile,localRecordingFile(owner,a.toURI().toString()));assertNull(localRecordingFile(owner,foreign.toURI().toString()));assertNull(localRecordingFile(owner,File(owner,"missing.m4a").toURI().toString()));assertNull(localRecordingFile(owner,"https://example.test/audio")) } finally { File(root,"owner/clip.m4a").delete();File(root,"foreign.m4a").delete();File(root,"owner").delete();root.delete() }
    }
}
