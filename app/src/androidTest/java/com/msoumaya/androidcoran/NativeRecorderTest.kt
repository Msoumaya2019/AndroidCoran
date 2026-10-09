package com.msoumaya.androidcoran
import android.Manifest
import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.audio.NativeRecorder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
class NativeRecorderTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Before fun microphonePermission() { val i=InstrumentationRegistry.getInstrumentation();i.uiAutomation.grantRuntimePermission(i.targetContext.packageName,Manifest.permission.RECORD_AUDIO) }
 private fun recorder()=NativeRecorder(compose.activity,File(compose.activity.filesDir,"recordings/native-recorder-tests"))
 @Test fun pauseResumeProducesDecodableMonoAudioWithoutPausedTime() {
  val r=recorder();var output:File?=null
  try {
   compose.runOnIdle { r.start() };Thread.sleep(5000)
   compose.runOnIdle { r.pause() };val paused=r.duration;Thread.sleep(800);assertEquals(paused,r.duration)
   compose.runOnIdle { r.resume() };Thread.sleep(5000)
   compose.runOnIdle { output=r.finish() };assertEquals(NativeRecorder.Phase.Preview,r.phase)
   assertTrue(output!!.length()>0);val extractor=MediaExtractor()
   try { extractor.setDataSource(output!!.path);val format=(0 until extractor.trackCount).map { extractor.getTrackFormat(it) }.single { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true };assertEquals(1,format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));assertEquals("audio/mp4a-latm",format.getString(MediaFormat.KEY_MIME)) } finally { extractor.release() }
   val metadata=MediaMetadataRetriever()
   try { metadata.setDataSource(output!!.path);assertEquals("yes",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));val duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong();assertTrue(duration>=1500);assertTrue(kotlin.math.abs(duration-r.duration)<800) } finally { metadata.release() }
  } finally { compose.runOnIdle { r.close() } }
  assertFalse(output!!.exists())
 }
 @Test fun cancellingPausedCaptureDeletesOnlyItsDraft() {
  val r=recorder();var output:File?=null
  try { compose.runOnIdle { r.start();output=r.file };Thread.sleep(800);compose.runOnIdle { r.pause();r.restart() };assertEquals(NativeRecorder.Phase.Idle,r.phase);assertEquals(0L,r.duration);assertFalse(output!!.exists()) } finally { compose.runOnIdle { r.close() } }
 }
 @Test fun transferredRecordingSurvivesControllerClosureAndRestart() {
  val r=recorder();var output:File?=null
  try { compose.runOnIdle { r.start() };Thread.sleep(5000);compose.runOnIdle { output=r.finish();r.saved();r.restart() };assertEquals(NativeRecorder.Phase.Idle,r.phase);assertTrue(output!!.isFile);compose.runOnIdle { r.close() };assertTrue(output!!.isFile) } finally { compose.runOnIdle { r.close() };output?.delete() }
 }

 @Test fun closureDuringSavePreservesFileUntilTransferCompletes() {
  val r=recorder();var output:File?=null
  try { compose.runOnIdle { r.start() };Thread.sleep(5000);compose.runOnIdle { output=r.finish();r.beginSave();r.close() };assertTrue(output!!.isFile);assertEquals(NativeRecorder.Phase.Saving,r.phase);compose.runOnIdle { r.saved();r.close() };assertEquals(NativeRecorder.Phase.Saved,r.phase);assertTrue(output!!.isFile) } finally { compose.runOnIdle { r.close() };output?.delete() }
 }
 @Test fun failedSaveAfterClosureReleasesItsUnsavedFile() {
  val r=recorder();var output:File?=null
  try { compose.runOnIdle { r.start() };Thread.sleep(5000);compose.runOnIdle { output=r.finish();r.beginSave();r.close();r.saveFailed() };assertEquals(NativeRecorder.Phase.Idle,r.phase);assertFalse(output!!.exists()) } finally { compose.runOnIdle { r.close() };output?.delete() }
 }

 @Test fun capturedFileCanBeDecodedByMedia3BeforeSaving() {
  val r=recorder();var player:androidx.media3.exoplayer.ExoPlayer?=null
  val ended=java.util.concurrent.atomic.AtomicBoolean(false);val error=java.util.concurrent.atomic.AtomicReference<androidx.media3.common.PlaybackException?>()
  try {
   compose.runOnIdle { r.start() };Thread.sleep(5000)
   compose.runOnIdle {
    val file=r.finish();player=androidx.media3.exoplayer.ExoPlayer.Builder(compose.activity).build().apply {
     volume=0f;addListener(object:androidx.media3.common.Player.Listener {
      override fun onPlaybackStateChanged(state: Int) { if(state==androidx.media3.common.Player.STATE_ENDED) ended.set(true) }
      override fun onPlayerError(e: androidx.media3.common.PlaybackException) { error.set(e) }
     });setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(file)));prepare();play()
    }
   }
   compose.waitUntil(45000) { ended.get()||error.get()!=null };assertNull(error.get());assertTrue(ended.get())
  } finally { compose.runOnIdle { player?.release();r.close() } }
 }
}
