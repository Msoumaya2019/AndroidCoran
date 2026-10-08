package com.msoumaya.androidcoran.domain

data class Reciter(val id: String,val name: String,val bitrate: Int=128,val folder: String?=null)
val reciters = listOf(
    Reciter("ar.husary","Mahmoud Khalil Al-Husary"),Reciter("ar.alafasy","Mishary Rashid Alafasy"),
    Reciter("ar.minshawi","Mohammed Siddiq Al-Minshawi"),Reciter("ar.shaatree","Abu Bakr Shatri"),
    Reciter("ar.ghamidi","Saad Al Ghamidi",40,"Ghamadi_40kbps"),Reciter("ar.dussary","Yasser Al Dosari",128,"Yasser_Ad-Dussary_128kbps"),
    Reciter("ar.qatami","Nasser Al Qatami",128,"Nasser_Alqatami_128kbps"))
data class AudioPosition(val verseId: Int,val repetition: Int=1)
enum class RepeatMode { PASSAGE, EACH_VERSE }
fun nextAudioPosition(range: VerseRange,current: AudioPosition,mode: RepeatMode,count: Int?,autoStop: Boolean=true): AudioPosition? {
    require(current.verseId in range.start..range.end && current.repetition>=1)
    val unlimited=count==null || !autoStop;val limit=(count?:1).coerceAtLeast(1)
    if(mode==RepeatMode.EACH_VERSE) {
        if(count==null || current.repetition<limit) return current.copy(repetition=current.repetition+1)
        if(current.verseId<range.end) return AudioPosition(current.verseId+1)
        return if(unlimited) AudioPosition(range.start) else null
    }
    if(current.verseId<range.end) return current.copy(verseId=current.verseId+1)
    return if(unlimited || current.repetition<limit) AudioPosition(range.start,current.repetition+1) else null
}
fun verseAudioUrl(v: Verse,r: Reciter) = r.folder?.let { "https://everyayah.com/data/$it/${v.surah.toString().padStart(3,'0')}${v.ayah.toString().padStart(3,'0')}.mp3" }
    ?: "https://cdn.islamic.network/quran/audio/${r.bitrate}/${r.id}/${v.id}.mp3"

fun upcomingAudioVerses(range: VerseRange,current: Int): List<Int> { require(current in range.start..range.end);return if(current==range.end) emptyList() else ((current+1)..minOf(range.end,current+3)).toList() }
