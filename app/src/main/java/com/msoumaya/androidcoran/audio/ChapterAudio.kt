package com.msoumaya.androidcoran.audio

import android.content.Context
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL

data class ChapterAudio(val url: String,val timings: Map<Int,Pair<Long,Long>>)
suspend fun chapterAudio(context: Context,q: Quran,id: Int,reciter: Reciter): ChapterAudio? = withContext(Dispatchers.IO) {
    val resource=mapOf("ar.husary" to 6,"ar.alafasy" to 7,"ar.minshawi" to 9,"ar.shaatree" to 4)[reciter.id]?:return@withContext null
    val chapter=q.verse(id).surah;val cache=File(context.cacheDir,"chapter-$resource-$chapter.json")
    fun parse(data: JsonObject): ChapterAudio {
        val file=data.obj("audio_file");val url=file.str("audio_url");require(url.startsWith("https://"));var previous=0L;val timings=linkedMapOf<Int,Pair<Long,Long>>()
        file.arr("timestamps").forEach { raw -> val row=raw.jsonObject;val key=row.str("verse_key").split(':').map { it.toInt() };require(key[0]==chapter);val verse=q.id(key[0],key[1]);val start=row["timestamp_from"]!!.jsonPrimitive.long;val end=row["timestamp_to"]!!.jsonPrimitive.long;require(start>=previous&&end>start&&verse !in timings);timings[verse]=start to end;previous=end }
        require(timings.size==q.surahs[chapter-1].range.ids.size);return ChapterAudio(url,timings)
    }
    try { if(cache.isFile) return@withContext parse(Json.parseToJsonElement(cache.readText()).jsonObject)
        val connection=(URL("https://api.quran.com/api/v4/chapter_recitations/$resource/$chapter?segments=true").openConnection() as java.net.HttpURLConnection).apply { connectTimeout=10000;readTimeout=10000 }
        try { check(connection.responseCode==200);val text=connection.inputStream.bufferedReader().use { it.readText() };val parsed=parse(Json.parseToJsonElement(text).jsonObject);cache.writeText(text);parsed } finally { connection.disconnect() }
    } catch(e: Exception) { null }
}
