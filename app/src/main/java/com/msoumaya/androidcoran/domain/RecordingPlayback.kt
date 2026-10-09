package com.msoumaya.androidcoran.domain
fun recordingPlaybackKey(owner: String,id: String)="recitation:$owner:$id"
fun seekRecording(current: Long,delta: Long,duration: Long): Long {
 val base=current.coerceAtLeast(0)
 val shifted=if(delta>0&&base>Long.MAX_VALUE-delta) Long.MAX_VALUE else if(delta<0&&base<Long.MIN_VALUE-delta) Long.MIN_VALUE else base+delta
 return shifted.coerceIn(0,duration.takeIf { it>0 }?:Long.MAX_VALUE)
}
fun recordingPlaybackProgress(elapsed: Long,duration: Long): Float = if(duration<=0) 0f else elapsed.coerceIn(0,duration).toFloat()/duration
fun recordingTime(ms: Long): String { val seconds=ms.coerceAtLeast(0)/1000;return "%d:%02d".format(seconds/60,seconds%60) }
