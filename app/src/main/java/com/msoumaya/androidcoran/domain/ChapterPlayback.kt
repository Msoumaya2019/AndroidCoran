package com.msoumaya.androidcoran.domain

/** Media3 positions are relative to the clipped chapter, timestamps are absolute. */
fun chapterVerseAt(timings: Map<Int,Pair<Long,Long>>, clip: VerseRange, absoluteMs: Long): Int {
    var verse=clip.start
    for (id in clip.start..clip.end) {
        val start=timings[id]?.first ?: continue
        if (start>absoluteMs) break
        verse=id
    }
    return verse
}

fun chapterProgress(timings: Map<Int,Pair<Long,Long>>, range: VerseRange, position: AudioPosition, absoluteMs: Long): AudioProgress {
    val timing=requireNotNull(timings[position.verseId])
    return audioProgress(range,position,(absoluteMs-timing.first).coerceIn(0L,timing.second-timing.first),timing.second-timing.first)
}
