package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class ChapterPlaybackTest {
 private val times=mapOf(10 to (1000L to 3000L),11 to (3500L to 7500L),12 to (7500L to 9000L))
 @Test fun seekingBackRestoresEarlierVerse() { val clip=VerseRange(10,12);assertEquals(12,chapterVerseAt(times,clip,8000));assertEquals(10,chapterVerseAt(times,clip,1500)) }
 @Test fun clipNeverSelectsVersesOutsideSelectedPassage() { val clip=VerseRange(11,12);assertEquals(11,chapterVerseAt(times,clip,0));assertEquals(12,chapterVerseAt(times,clip,Long.MAX_VALUE)) }
 @Test fun exactBoundaryAndSilenceKeepStableHighlight() { val clip=VerseRange(10,12);assertEquals(10,chapterVerseAt(times,clip,3200));assertEquals(11,chapterVerseAt(times,clip,3500));assertEquals(12,chapterVerseAt(times,clip,7500)) }
 @Test fun progressUsesCurrentVerseDurationNotWholeChapter() { val p=chapterProgress(times,VerseRange(10,12),AudioPosition(11,2),5500);assertEquals(2000L,p.elapsedMs);assertEquals(2,p.verseIndex);assertEquals(0.5f,p.fraction,0.0001f) }
 @Test fun gapAndEndClampProgress() { assertEquals(1f/3,chapterProgress(times,VerseRange(10,12),AudioPosition(10),3200).fraction,0.0001f);assertEquals(1f,chapterProgress(times,VerseRange(10,12),AudioPosition(12),10000).fraction,0.0001f) }
}
