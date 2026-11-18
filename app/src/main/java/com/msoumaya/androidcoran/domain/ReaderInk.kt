package com.msoumaya.androidcoran.domain
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID

data class InkPoint(val x: Float,val y: Float,val pressure: Float=1f)
data class InkStroke(val points: List<InkPoint>)
data class ReaderInk(val id: String,val page: Int,val outlines: List<InkStroke>,val note: List<InkStroke>,val verses: List<Int>,val updatedAt: String,val deletedAt: String?=null)
private fun inkJson(strokes: List<InkStroke>)=JsonArray(strokes.map { stroke -> JsonArray(stroke.points.map { point -> element(listOf(point.x,point.y,point.pressure)) }) })
private fun inkStrokes(value: JsonArray)=value.mapNotNull { raw ->
    val points=(raw as? JsonArray)?.mapNotNull { p ->
        val a=p as? JsonArray?:return@mapNotNull null
        val x=a.getOrNull(0)?.jsonPrimitive?.floatOrNull;val y=a.getOrNull(1)?.jsonPrimitive?.floatOrNull
        val pressure=a.getOrNull(2)?.jsonPrimitive?.floatOrNull?:1f
        if(x==null||y==null||!x.isFinite()||!y.isFinite()||!pressure.isFinite()) null else InkPoint(x.coerceIn(0f,1f),y.coerceIn(0f,1f),pressure.coerceIn(.1f,1f))
    }?:emptyList()
    if(points.isEmpty()) null else InkStroke(points.take(12000))
}
fun ReaderInk.toJson()=json("id" to id,"page" to page,"outlines" to inkJson(outlines),"note" to inkJson(note),"verses" to verses,"updatedAt" to updatedAt,"deletedAt" to deletedAt)
fun readerInk(s: JsonObject,page: Int): List<ReaderInk> = s.obj("readerInk").values.mapNotNull { raw ->
    val r=raw as? JsonObject?:return@mapNotNull null
    if(r.num("page")!=page||r.str("deletedAt").isNotBlank()||r.str("id").isBlank()) return@mapNotNull null
    ReaderInk(r.str("id"),page,inkStrokes(r.arr("outlines")),inkStrokes(r.arr("note")),r.arr("verses").mapNotNull { it.jsonPrimitive.intOrNull },r.str("updatedAt"))
}
fun newReaderInk(page: Int,stroke: InkStroke,verses: List<Int>,now: String=Instant.now().toString()): ReaderInk {
    require(page in 1..604&&stroke.points.isNotEmpty())
    return ReaderInk(UUID.randomUUID().toString(),page,listOf(stroke),emptyList(),verses.distinct().filter { it in 1..6236 },now)
}
fun saveReaderInk(s: JsonObject,ink: ReaderInk): JsonObject {
    val existing=s.obj("readerInk").obj(ink.id)
    if(existing.str("deletedAt").isNotBlank()) return s
    if(runCatching { Instant.parse(existing.str("updatedAt")).isAfter(Instant.parse(ink.updatedAt)) }.getOrDefault(false)) return s
    return touch(s.with("readerInk" to s.obj("readerInk").with(ink.id to ink.toJson())))
}
fun deleteReaderInk(s: JsonObject,id: String,now: String=Instant.now().toString()): JsonObject {
    val old=s.obj("readerInk").obj(id);if(old.isEmpty()) return s
    return touch(s.with("readerInk" to s.obj("readerInk").with(id to old.with("deletedAt" to JsonPrimitive(now),"updatedAt" to JsonPrimitive(now),"outlines" to JsonArray(emptyList()),"note" to JsonArray(emptyList())))))
}

/** Local ink is retained even when an older client sends a state without this extension. */
fun mergeReaderInkCopies(local: JsonObject,remote: JsonObject): JsonObject = JsonObject((local.keys+remote.keys).filter { local[it] is JsonObject||remote[it] is JsonObject }.associateWith { id ->
    val candidates=listOfNotNull(local[id] as? JsonObject,remote[id] as? JsonObject)
    val deleted=candidates.filter { it.str("deletedAt").isNotBlank() }
    (if(deleted.isNotEmpty()) deleted else candidates).maxBy { runCatching { Instant.parse(it.str("updatedAt")) }.getOrDefault(Instant.EPOCH) }
})
