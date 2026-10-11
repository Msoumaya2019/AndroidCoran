package com.msoumaya.androidcoran

import android.content.ComponentName
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.audio.QuranAudioCache
import com.msoumaya.androidcoran.audio.RecitationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ChapterAudioServiceTest {
 @Test fun pausedSystemSeeksUpdateClippedVerseAndProgress() {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    val url="https://androidcoran-test.invalid/chapter-${System.nanoTime()}.wav"
    // Synthetic silent PCM, cached locally: no Quran CDN or backend writes.
    val samples=16000*14;val bytes=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN)
    bytes.put("RIFF".toByteArray()).putInt(36+samples*2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1.toShort()).putShort(1.toShort()).putInt(16000).putInt(32000).putShort(2.toShort()).putShort(16.toShort()).put("data".toByteArray()).putInt(samples*2)
    val source=QuranAudioCache.dataSourceFactory(context).setUpstreamDataSourceFactory { ByteArrayDataSource(bytes.array()) }.createDataSource()
    try { source.open(DataSpec.Builder().setUri(url).build());val buffer=ByteArray(8192);while(source.read(buffer,0,buffer.size)!=-1) { } } finally { source.close() }
    val cache=File(context.cacheDir,"chapter-7-1.json");val previous=if(cache.exists()) cache.readBytes() else null
    val stamps=(1..7).joinToString(",") { id->"{\"verse_key\":\"1:$id\",\"timestamp_from\":${(id-1)*2000},\"timestamp_to\":${id*2000}}" }
    cache.writeText("{\"audio_file\":{\"audio_url\":\"$url\",\"timestamps\":[$stamps]}}")
    try {
      ActivityScenario.launch(MainActivity::class.java).use { scenario->
        var controller: MediaController?=null
        try {
          scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("PLAY_RANGE").putExtra("start",2).putExtra("end",4).putExtra("reciter","ar.alafasy").putExtra("count",1)) }
          lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
          scenario.onActivity { future=MediaController.Builder(it,SessionToken(it,ComponentName(it,RecitationService::class.java))).buildAsync() }
          val active=future.get(10,TimeUnit.SECONDS);controller=active
          runBlocking { withTimeout(15000) { while(true) { var ready=false;scenario.onActivity { ready=active.playbackState==Player.STATE_READY&&active.isPlaying };if(ready) break;delay(50) } } }
          scenario.onActivity { active.pause() }
          runBlocking { withTimeout(5000) { RecitationService.playing.first { !it } } }
          scenario.onActivity { active.seekTo(4500) }
          runBlocking { withTimeout(5000) { RecitationService.current.first { it?.verseId==4 } } }
          scenario.onActivity { active.seekTo(1000) }
          runBlocking { withTimeout(5000) { RecitationService.current.first { it?.verseId==2 } } }
          assertFalse(RecitationService.playing.value)
          assertEquals(1f/6,RecitationService.passageProgress.value!!.fraction,0.01f)
          assertEquals(1000L,RecitationService.passageProgress.value!!.elapsedMs)
          scenario.onActivity { active.play() }
          runBlocking { withTimeout(5000) { RecitationService.playing.first { it } } }
        } finally { scenario.onActivity { controller?.release();it.startService(Intent(it,RecitationService::class.java).setAction("STOP")) };runBlocking { withTimeout(5000) { RecitationService.current.first { it==null } } } }
      }
    } finally { if(previous==null) cache.delete() else cache.writeBytes(previous) }
 }
}

