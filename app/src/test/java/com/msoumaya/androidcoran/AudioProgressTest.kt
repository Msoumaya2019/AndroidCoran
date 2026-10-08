package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class AudioProgressTest {
 @Test fun passageCombinesVerseIndexAndCurrentFileFraction() { val p=audioProgress(VerseRange(10,13),AudioPosition(11),3000,6000);assertEquals(3000L,p.elapsedMs);assertEquals(2,p.verseIndex);assertEquals(4,p.verseCount);assertEquals(0.375f,p.fraction,0.0001f) }
 @Test fun unknownDurationAndRestartKeepCorrectVerseCount() { assertEquals(0.5f,audioProgress(VerseRange(10,13),AudioPosition(12),1000,-1).fraction,0.0001f);assertEquals(0f,audioProgress(VerseRange(10,13),AudioPosition(10,2),0,6000).fraction,0.0001f) }
 @Test fun progressClampsNegativeTimeAndCompletedFile() { assertEquals(0L,audioProgress(VerseRange(6236,6236),AudioPosition(6236),-3,1000).elapsedMs);assertEquals(1f,audioProgress(VerseRange(6236,6236),AudioPosition(6236),1500,1000).fraction,0.0001f) }
}
