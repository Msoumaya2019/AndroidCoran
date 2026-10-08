package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
const val TECHNICAL_AYAH_GAP_MS=200L
val repeatCounts=listOf(1,2,3,5,10)
val repeatGaps=listOf(0,2,5,10)
val audioSpeeds=listOf(0.75f,1f,1.25f)
data class RepeatPreferences(val countChoice: String="3",val customCount: String="20",val mode: RepeatMode=RepeatMode.PASSAGE,val gap: Int=0,val speed: Float=1f,val autoStop: Boolean=true) {
    val count: Int? get()=when(countChoice) { "continuous"->null;"custom"->customCount.toIntOrNull()?.takeIf { it>0 }?:1;else->countChoice.toIntOrNull()?.takeIf { it>0 }?:1 }
    fun json()=json("countChoice" to (countChoice.toIntOrNull()?.let { JsonPrimitive(it) }?:JsonPrimitive(countChoice)),"customCount" to customCount,"repeatMode" to if(mode==RepeatMode.EACH_VERSE) "each-verse" else "passage","gap" to gap,"speed" to speed,"autoStop" to autoStop)
    companion object { fun from(data: JsonObject): RepeatPreferences {
        val choice=data.str("countChoice","3").takeIf { it in repeatCounts.map(Int::toString)+listOf("custom","continuous") }?:"3"
        val speed=(data["speed"] as? JsonPrimitive)?.floatOrNull?.takeIf { it in audioSpeeds }?:1f
        return RepeatPreferences(choice,data.str("customCount","20"),if(data.str("repeatMode")=="each-verse") RepeatMode.EACH_VERSE else RepeatMode.PASSAGE,data.num("gap").takeIf { it in repeatGaps }?:0,speed,data.flag("autoStop",true))
    } }
}
fun repetitionDelay(range: VerseRange,current: AudioPosition,next: AudioPosition,mode: RepeatMode,gapSeconds: Int): Long {
    val restart=next.verseId==range.start&&current.verseId==range.end
    val repeated=mode==RepeatMode.EACH_VERSE&&next.verseId==current.verseId&&next.repetition>current.repetition
    return maxOf(TECHNICAL_AYAH_GAP_MS,if(restart||repeated) gapSeconds.coerceAtLeast(0)*1000L else 0L)
}
/** A paused repeat retains both the next verse and the remaining wait. Clock supplied by caller. */
class RepeatPause {
    var next: AudioPosition?=null;private set
    private var deadline: Long?=null
    private var remaining=0L
    val active get()=next!=null&&deadline!=null
    fun start(position: AudioPosition,wait: Long,now: Long) { next=position;remaining=wait.coerceAtLeast(0);deadline=now+remaining }
    fun pause(now: Long) { deadline?.let { remaining=(it-now).coerceAtLeast(0) };deadline=null }
    fun resume(now: Long): Long { if(next==null) return 0;if(deadline==null) deadline=now+remaining;return (deadline!!-now).coerceAtLeast(0) }
    fun takeDue(now: Long): AudioPosition? { if(deadline==null||now<deadline!!) return null;return next.also { clear() } }
    fun clear() { next=null;deadline=null;remaining=0 }
}
