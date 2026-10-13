package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.archiveResumeOffset
import org.junit.Assert.*
import org.junit.Test
class ArchiveResumeTest {
 @Test fun fullResponseRestartsInsteadOfAppending() { assertEquals(0L,archiveResumeOffset(200,null,40,100)) }
 @Test fun exactPartialResponsePreservesExistingBytes() { assertEquals(40L,archiveResumeOffset(206,"bytes 40-99/100",40,100)) }
 @Test(expected=IllegalArgumentException::class) fun wrongStartIsRejected() { archiveResumeOffset(206,"bytes 39-99/100",40,100) }
 @Test(expected=IllegalArgumentException::class) fun changedArchiveSizeIsRejected() { archiveResumeOffset(206,"bytes 40-100/101",40,100) }
 @Test(expected=IllegalArgumentException::class) fun overflowingEndIsRejected() { archiveResumeOffset(206,"bytes 40-100/100",40,100) }
 @Test(expected=IllegalArgumentException::class) fun unknownTotalCannotBeAppended() { archiveResumeOffset(206,"bytes 40-99/*",40,100) }
 @Test(expected=IllegalArgumentException::class) fun errorResponseIsRejected() { archiveResumeOffset(416,null,40,100) }
}
