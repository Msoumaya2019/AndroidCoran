package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import androidx.compose.ui.Modifier
import com.msoumaya.androidcoran.domain.*
@Composable fun RecitationPlaybackControls(active: Boolean,playing: Boolean,elapsed: Long,duration: Long,onPlay: ()->Unit,onToggle: ()->Unit,onSeek: (Long)->Unit) {
 TextButton(onClick={if(active) onToggle() else onPlay()}) { Text(if(active&&playing) "Pause" else if(active&&elapsed>0) "Reprendre" else "Réécouter") }
 Row { TextButton(enabled=active,onClick={onSeek(-10000)}) { Text("− 10 s") };TextButton(enabled=active,onClick={onSeek(10000)}) { Text("+ 10 s") } }
 val position=if(active) elapsed.coerceAtLeast(0) else 0L
 LinearProgressIndicator(progress={recordingPlaybackProgress(position,duration)},modifier=Modifier.fillMaxWidth())
 Text("Position : "+recordingTime(position)+" / "+recordingTime(duration))
}

@Composable fun LiveRecitationPlaybackControls(key: String,duration: Long,onPlay: ()->Unit,onToggle: ()->Unit,onSeek: (Long)->Unit) {
 val activeKey by RecitationService.standaloneKey.collectAsStateWithLifecycle()
 if(activeKey==key) {
  val playing by RecitationService.playing.collectAsStateWithLifecycle()
  val elapsed by RecitationService.elapsed.collectAsStateWithLifecycle()
  RecitationPlaybackControls(true,playing,elapsed,duration,onPlay,onToggle,onSeek)
 } else RecitationPlaybackControls(false,false,0,duration,onPlay,onToggle,onSeek)
}
