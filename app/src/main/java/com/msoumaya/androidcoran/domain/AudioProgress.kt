package com.msoumaya.androidcoran.domain

/** Matches the source passage progress: completed verses plus the current file fraction. */
data class AudioProgress(val elapsedMs: Long,val verseIndex: Int,val verseCount: Int,val fraction: Float)
fun audioProgress(range: VerseRange,position: AudioPosition,elapsedMs: Long,durationMs: Long): AudioProgress {
    require(position.verseId in range.start..range.end)
    val elapsed=elapsedMs.coerceAtLeast(0)
    val currentFraction=if(durationMs>0) (elapsed.toDouble()/durationMs).coerceIn(0.0,1.0) else 0.0
    val total=range.end-range.start+1
    return AudioProgress(elapsed,position.verseId-range.start+1,total,((position.verseId-range.start+currentFraction)/total).toFloat().coerceIn(0f,1f))
}
