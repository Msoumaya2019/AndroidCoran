package com.msoumaya.androidcoran.audio

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.*
import com.msoumaya.androidcoran.CoranApplication
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class RecitationService: MediaSessionService() {
    private lateinit var player: ExoPlayer;private var session: MediaSession?=null
    private val handler=Handler(Looper.getMainLooper());private var generation=0
    private var standalone=false
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var timeline: ChapterAudio?=null;private var chapterEnd=0;private var clipStart=0L
    private val follow=object: Runnable { override fun run() { val times=timeline;if(times!=null) { var id=position.verseId;while(id<chapterEnd&&(times.timings[id+1]?.first?:Long.MAX_VALUE)<=player.currentPosition+clipStart) id++;if(id!=position.verseId) { position=position.copy(verseId=id);_current.value=position };handler.postDelayed(this,80) } } }
    private var range=VerseRange(1,7);private var position=AudioPosition(1);private var mode=RepeatMode.PASSAGE;private var count: Int?=1;private var reciter=reciters[3]
    companion object { private val _current=MutableStateFlow<AudioPosition?>(null);val current=_current.asStateFlow();private val _playing=MutableStateFlow(false);val playing=_playing.asStateFlow() }
    override fun onCreate() {
        super.onCreate();player=ExoPlayer.Builder(this).setMediaSourceFactory(QuranAudioCache.factory(this)).build();player.setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),true);player.setHandleAudioBecomingNoisy(true)
        player.addListener(object: Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _playing.value=isPlaying }
            override fun onPlaybackStateChanged(state: Int) { if(state==Player.STATE_ENDED) { if(timeline!=null&&!standalone) { position=position.copy(verseId=chapterEnd);_current.value=position };val next=if(standalone) null else nextAudioPosition(range,position,mode,count);if(next==null) { player.pause();_playing.value=false;handler.removeCallbacks(follow) } else { val expected=generation;handler.postDelayed({ if(expected==generation&&player.playWhenReady) { position=next;startAudio() } },200) } } }
            override fun onPlayerError(error: PlaybackException) { (application as CoranApplication).repository.feedback("Lecture audio interrompue : ${error.errorCodeName}");_playing.value=false }
        });session=MediaSession.Builder(this,player).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        when(intent?.action) {
            "PLAY_URL" -> { val url=intent.getStringExtra("url")?:return super.onStartCommand(intent,flags,startId);if(!url.startsWith("https://")) return super.onStartCommand(intent,flags,startId);generation++;handler.removeCallbacksAndMessages(null);standalone=true;timeline=null;_current.value=null;player.setMediaSource(DefaultMediaSourceFactory(this).createMediaSource(MediaItem.fromUri(url)));player.prepare();player.play() }
            "PLAY_RANGE" -> { val start=intent.getIntExtra("start",1);val end=intent.getIntExtra("end",7);if(start !in 1..6236||end !in start..6236) return super.onStartCommand(intent,flags,startId);generation++;handler.removeCallbacksAndMessages(null);player.stop();range=VerseRange(start,end);position=AudioPosition(range.start);reciter=reciters.firstOrNull { it.id==intent.getStringExtra("reciter") }?:reciters[3];count=intent.getIntExtra("count",1).let { if(it==0) null else it };mode=if(intent.getBooleanExtra("each",false)) RepeatMode.EACH_VERSE else RepeatMode.PASSAGE;startAudio() }
            "TOGGLE" -> { generation++;handler.removeCallbacksAndMessages(null);if(player.isPlaying) player.pause() else if(player.playbackState==Player.STATE_ENDED) { if(standalone) { player.seekTo(0);player.play() } else { position=position.copy(repetition=1);startAudio() } } else { player.play();if(timeline!=null) handler.post(follow) } }
            "STOP" -> { generation++;handler.removeCallbacksAndMessages(null);player.stop();_current.value=null;stopSelf() }
        }
        return super.onStartCommand(intent,flags,startId)
    }
    private fun playVerse() { timeline=null;handler.removeCallbacks(follow);standalone=false;val q=(application as CoranApplication).repository.quran;val verse=q.verse(position.verseId);_current.value=position;player.setMediaItem(MediaItem.Builder().setMediaId(verse.id.toString()).setUri(verseAudioUrl(verse,reciter)).setMediaMetadata(MediaMetadata.Builder().setTitle("${q.surah(verse.id).name} · ${verse.ayah}").setArtist(reciter.name).build()).build());player.prepare();player.play() }
    override fun onDestroy() { scope.cancel();generation++;handler.removeCallbacksAndMessages(null);session?.release();player.release();_current.value=null;_playing.value=false;super.onDestroy() }
    private fun startAudio() {
        timeline=null;handler.removeCallbacks(follow)
        if(mode==RepeatMode.EACH_VERSE||range.start==range.end) { playVerse();return }
        val expected=generation;val q=(application as CoranApplication).repository.quran
        scope.launch {
            val chapter=chapterAudio(this@RecitationService,q,position.verseId,reciter)
            if(expected!=generation) return@launch
            if(chapter==null) { playVerse();return@launch }
            standalone=false;timeline=chapter;val start=position.verseId;chapterEnd=minOf(range.end,q.surah(start).range.end);clipStart=chapter.timings[start]!!.first
            val clip=MediaItem.ClippingConfiguration.Builder().setStartPositionMs(chapter.timings[start]!!.first).setEndPositionMs(chapter.timings[chapterEnd]!!.second).build()
            player.setMediaItem(MediaItem.Builder().setUri(chapter.url).setClippingConfiguration(clip).setMediaMetadata(MediaMetadata.Builder().setTitle(q.surah(start).name).setArtist(reciter.name).build()).build());_current.value=position;player.prepare();player.play();handler.post(follow)
        }
    }
}
