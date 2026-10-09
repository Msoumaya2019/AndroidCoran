package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class RecordingPlaybackTest {
 @Test fun seekingCannotGoBeforeStartOrAfterKnownEnd() { assertEquals(0L,seekRecording(3000,-10000,20000));assertEquals(20000L,seekRecording(18000,10000,20000));assertEquals(11000L,seekRecording(1000,10000,20000)) }
 @Test fun unknownDurationDoesNotBlockForwardSeeking() { assertEquals(12000L,seekRecording(2000,10000,-1));assertEquals(12000L,seekRecording(2000,10000,0));assertEquals(0L,seekRecording(-10,-10000,0)) }
 @Test fun malformedIntentCannotOverflowPosition() { assertEquals(Long.MAX_VALUE,seekRecording(10000,Long.MAX_VALUE,0));assertEquals(0L,seekRecording(10000,Long.MIN_VALUE,0));assertEquals(20000L,seekRecording(10000,Long.MAX_VALUE,20000)) }
 @Test fun progressHandlesUnknownDurationAndBounds() { assertEquals(0f,recordingPlaybackProgress(100,0),0f);assertEquals(0f,recordingPlaybackProgress(-1,100),0f);assertEquals(.5f,recordingPlaybackProgress(50,100),0f);assertEquals(1f,recordingPlaybackProgress(150,100),0f) }
 @Test fun timeMatchesSourceMinuteAndSecondFormat() { assertEquals("0:00",recordingTime(-100));assertEquals("0:00",recordingTime(0));assertEquals("1:05",recordingTime(65000));assertEquals("120:05",recordingTime(7205000)) }
 @Test fun activeRecordingIdentitySeparatesAccountsAndClips() { assertNotEquals(recordingPlaybackKey("a","x"),recordingPlaybackKey("b","x"));assertNotEquals(recordingPlaybackKey("a","x"),recordingPlaybackKey("a","y")) }
}
