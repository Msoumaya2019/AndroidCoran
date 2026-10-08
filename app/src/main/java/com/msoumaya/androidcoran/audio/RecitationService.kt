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
    private var loading=false;private var requestedPlaying=false
    private var standalone=false
    private var gap=0;private var speed=1f;private var autoStop=true
    private val repeatPause=RepeatPause()
    private fun now()=android.os.SystemClock.elapsedRealtime()
    private val transition=Runnable { repeatPause.takeDue(now())?.let { _waiting.value=false;position=it;startAudio() } }
    private fun clearPending() { handler.removeCallbacks(transition);repeatPause.clear();_waiting.value=false }
    private fun pausePlayback() { requestedPlaying=false;generation++;handler.removeCallbacks(follow); handler.removeCallbacks(transition);repeatPause.pause(now());player.pause();_playing.value=false }
    private fun resumePlayback() { requestedPlaying=true;if(repeatPause.next!=null) { handler.postDelayed(transition,repeatPause.resume(now()));_playing.value=true } else if(loading) { generation++;startAudio() } else if(player.playbackState==Player.STATE_ENDED) { if(standalone) { player.seekTo(0);player.play() } else { position=AudioPosition(range.start);startAudio() } } else { player.play();if(timeline!=null) handler.post(follow) } }
    private fun applyPreferences(prefs: RepeatPreferences) { count=prefs.count;mode=prefs.mode;gap=prefs.gap;speed=prefs.speed;autoStop=prefs.autoStop;player.setPlaybackSpeed(speed);_activePreferences.value=prefs }
    private fun finishBoundary() {
        if(repeatPause.next!=null) return
        updateProgress()
        val next=if(standalone) null else nextAudioPosition(range,position,mode,count,autoStop)
        handler.removeCallbacks(follow)
        if(next==null) { requestedPlaying=false;player.pause();_playing.value=false }
        else { val wait=repetitionDelay(range,position,next,mode,gap);player.pause();repeatPause.start(next,wait,now());_waiting.value=true;_playing.value=true;handler.removeCallbacks(transition);handler.postDelayed(transition,wait) }
    }
    private fun updateProgress() { _passageProgress.value=if(standalone||_current.value==null) null else audioProgress(range,position,player.currentPosition,player.duration) }
    private val progress=object: Runnable { override fun run() { if(player.isPlaying) { _elapsed.value=player.currentPosition.coerceAtLeast(0);updateProgress();handler.postDelayed(this,250) } } }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var timeline: ChapterAudio?=null;private var chapterEnd=0;private var clipStart=0L
    private val follow=object: Runnable { override fun run() {
        val times=timeline?:return
        val clock=player.currentPosition+clipStart
        if(mode==RepeatMode.EACH_VERSE&&clock>=(times.timings[position.verseId]?.second?:Long.MAX_VALUE)) { finishBoundary();return }
        var id=position.verseId
        while(id<chapterEnd&&(times.timings[id+1]?.first?:Long.MAX_VALUE)<=clock) id++
        if(id!=position.verseId) { position=position.copy(verseId=id);_current.value=position;updateProgress() }
        handler.postDelayed(this,80)
    } }

    private val prefetchJobs=mutableMapOf<String,Job>()
    private fun preloadVerses(q: Quran) {
        for(id in upcomingAudioVerses(range,position.verseId)) {
            val url=verseAudioUrl(q.verse(id),reciter)
            if(prefetchJobs.containsKey(url)) continue
            prefetchJobs[url]=scope.launch { try { QuranAudioCache.prefetch(this@RecitationService,url) } catch(e: Exception) { if(e is CancellationException) throw e } finally { prefetchJobs.remove(url) } }
        }
    }
    private var range=VerseRange(1,7);private var position=AudioPosition(1);private var mode=RepeatMode.PASSAGE;private var count: Int?=1;private var reciter=reciters[3]
    companion object { private val _activeRange=MutableStateFlow<VerseRange?>(null);val activeRange=_activeRange.asStateFlow(); private val _activePreferences=MutableStateFlow<RepeatPreferences?>(null);val activePreferences=_activePreferences.asStateFlow(); private val _passageProgress=MutableStateFlow<AudioProgress?>(null);val passageProgress=_passageProgress.asStateFlow(); private val _waiting=MutableStateFlow(false);val waiting=_waiting.asStateFlow(); private val _elapsed=MutableStateFlow(0L);val elapsed=_elapsed.asStateFlow();private val _current=MutableStateFlow<AudioPosition?>(null);val current=_current.asStateFlow();private val _playing=MutableStateFlow(false);val playing=_playing.asStateFlow() }
    override fun onCreate() {
        super.onCreate();player=ExoPlayer.Builder(this).setMediaSourceFactory(QuranAudioCache.factory(this)).build();player.setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),true);player.setHandleAudioBecomingNoisy(true)
        player.addListener(object: Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _playing.value=requestedPlaying&&(isPlaying||repeatPause.active||loading);handler.removeCallbacks(progress);_elapsed.value=player.currentPosition.coerceAtLeast(0);updateProgress();if(isPlaying) handler.post(progress) }
            override fun onPlaybackStateChanged(state: Int) { if(state==Player.STATE_ENDED) { if(timeline!=null&&!standalone) { position=position.copy(verseId=chapterEnd);_current.value=position };finishBoundary() } }
            override fun onPlayerError(error: PlaybackException) { requestedPlaying=false;loading=false;generation++;clearPending();handler.removeCallbacks(follow); (application as CoranApplication).repository.feedback("Lecture audio interrompue : ${error.errorCodeName}");_playing.value=false }
        });session=MediaSession.Builder(this,object: ForwardingPlayer(player) {
            override fun play()=resumePlayback()
            override fun pause()=pausePlayback()
            override fun setPlayWhenReady(playWhenReady: Boolean) { if(playWhenReady) resumePlayback() else pausePlayback() }
            override fun getPlayWhenReady()=if(repeatPause.next!=null) repeatPause.active else player.playWhenReady
        }).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        when(intent?.action) {
            "PLAY_LOCAL" -> { val owner=(application as CoranApplication).repository.user.value?:return super.onStartCommand(intent,flags,startId);val uri=intent.getStringExtra("url")?:return super.onStartCommand(intent,flags,startId);val file=localRecordingFile(java.io.File(filesDir,"recordings/$owner"),uri)?:return super.onStartCommand(intent,flags,startId);generation++;clearPending();handler.removeCallbacksAndMessages(null);standalone=true;loading=false;requestedPlaying=true;player.setPlaybackSpeed(1f);timeline=null;_current.value=null;_passageProgress.value=null;_activePreferences.value=null;_activeRange.value=null;_elapsed.value=0;player.setMediaSource(DefaultMediaSourceFactory(this).createMediaSource(MediaItem.fromUri(android.net.Uri.fromFile(file))));player.prepare();player.play() }
            "UPDATE_SETTINGS" -> if(!standalone&&_current.value!=null) { val raw=intent.getStringExtra("settings");if(raw!=null) try { (kotlinx.serialization.json.Json.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonObject)?.let { applyPreferences(RepeatPreferences.from(it)) } } catch(_: IllegalArgumentException) { } }
            "SEEK" -> if(standalone) { val upper=player.duration.takeIf { it>0 }?:Long.MAX_VALUE;player.seekTo((player.currentPosition+intent.getLongExtra("delta",0L)).coerceIn(0L,upper));_elapsed.value=player.currentPosition.coerceAtLeast(0) }
            "PLAY_URL" -> { val url=intent.getStringExtra("url")?:return super.onStartCommand(intent,flags,startId);if(!url.startsWith("https://")) return super.onStartCommand(intent,flags,startId);generation++;clearPending();handler.removeCallbacksAndMessages(null);standalone=true;loading=false;requestedPlaying=true;player.setPlaybackSpeed(1f);timeline=null;_current.value=null;_passageProgress.value=null;_activePreferences.value=null;_activeRange.value=null;_elapsed.value=0;player.setMediaSource(DefaultMediaSourceFactory(this).createMediaSource(MediaItem.fromUri(url)));player.prepare();player.play() }
            "PLAY_RANGE" -> { val start=intent.getIntExtra("start",1);val end=intent.getIntExtra("end",7);if(start !in 1..6236||end !in start..6236) return super.onStartCommand(intent,flags,startId);generation++;clearPending();handler.removeCallbacksAndMessages(null);player.stop();range=VerseRange(start,end);position=AudioPosition(range.start);reciter=reciters.firstOrNull { it.id==intent.getStringExtra("reciter") }?:reciters[3];count=intent.getIntExtra("count",3).let { if(it==0) null else it.coerceAtLeast(1) };gap=intent.getIntExtra("gap",0).takeIf { it in repeatGaps }?:0;speed=intent.getFloatExtra("speed",1f).takeIf { it in audioSpeeds }?:1f;autoStop=intent.getBooleanExtra("autoStop",true);player.setPlaybackSpeed(speed);mode=if(intent.getBooleanExtra("each",false)) RepeatMode.EACH_VERSE else RepeatMode.PASSAGE;_activePreferences.value=RepeatPreferences(count?.toString()?:"continuous",count?.toString()?:"20",mode,gap,speed,autoStop);startAudio() }
            "PREVIOUS_VERSE", "NEXT_VERSE", "RESTART" -> if(!standalone&&_current.value!=null) { generation++;clearPending();handler.removeCallbacksAndMessages(null);player.pause();position=AudioPosition(if(intent.action=="RESTART") range.start else (position.verseId+if(intent.action=="NEXT_VERSE") 1 else -1).coerceIn(range.start,range.end));startAudio() }
            "TOGGLE" -> { if(requestedPlaying&&(player.isPlaying||repeatPause.active||loading)) pausePlayback() else resumePlayback() }
            "STOP" -> { requestedPlaying=false;loading=false; generation++;clearPending();handler.removeCallbacksAndMessages(null);player.stop();_current.value=null;_passageProgress.value=null;_activePreferences.value=null;_activeRange.value=null;stopSelf() }
        }
        return super.onStartCommand(intent,flags,startId)
    }
    private fun playVerse() { loading=false;requestedPlaying=true;timeline=null;handler.removeCallbacks(follow);standalone=false;val q=(application as CoranApplication).repository.quran;val verse=q.verse(position.verseId);_current.value=position;player.setMediaItem(MediaItem.Builder().setMediaId(verse.id.toString()).setUri(verseAudioUrl(verse,reciter)).setMediaMetadata(MediaMetadata.Builder().setTitle("${q.surah(verse.id).name} · ${verse.ayah}").setArtist(reciter.name).build()).build());player.prepare();player.play();preloadVerses(q) }
    override fun onDestroy() { scope.cancel();requestedPlaying=false;loading=false;generation++;clearPending();handler.removeCallbacksAndMessages(null);session?.release();player.release();_current.value=null;_passageProgress.value=null;_activePreferences.value=null;_activeRange.value=null;_playing.value=false;_elapsed.value=0;super.onDestroy() }
    private fun startAudio() {
        _activeRange.value=range;standalone=false;loading=true;requestedPlaying=true;_playing.value=true;_current.value=position;_passageProgress.value=audioProgress(range,position,0,0);timeline=null;handler.removeCallbacks(follow)
        if(mode==RepeatMode.EACH_VERSE||range.start==range.end) { playVerse();return }
        val expected=generation;val q=(application as CoranApplication).repository.quran
        scope.launch {
            val chapter=chapterAudio(this@RecitationService,q,position.verseId,reciter)
            if(expected!=generation) return@launch
            loading=false
            if(chapter==null) { playVerse();return@launch }
            standalone=false;timeline=chapter;val start=position.verseId;chapterEnd=minOf(range.end,q.surah(start).range.end);clipStart=chapter.timings[start]!!.first
            val clip=MediaItem.ClippingConfiguration.Builder().setStartPositionMs(chapter.timings[start]!!.first).setEndPositionMs(chapter.timings[chapterEnd]!!.second).build()
            player.setMediaItem(MediaItem.Builder().setUri(chapter.url).setClippingConfiguration(clip).setMediaMetadata(MediaMetadata.Builder().setTitle(q.surah(start).name).setArtist(reciter.name).build()).build());_current.value=position;player.prepare();player.play();handler.post(follow)
        }
    }
}
