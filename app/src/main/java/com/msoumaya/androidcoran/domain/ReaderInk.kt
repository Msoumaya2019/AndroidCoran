package com.msoumaya.androidcoran.domain
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID

data class InkPoint(val x: Float,val y: Float,val pressure: Float=1f)
data class InkStroke(val points: List<InkPoint>,val color: String?=null)
data class ReaderInk(val id: String,val page: Int,val outlines: List<InkStroke>,val note: List<InkStroke>,val verses: List<Int>,val updatedAt: String,val deletedAt: String?=null,val source: String="coranTest",val tool: String="pen",val color: String="#AD2A30",val width: Float=.003f,val penStyle: String="ink",val text: String="",val anchor: JsonObject?=null,val favorite: Boolean=false,val masteredAt: String?=null,val createdAt: String?=null,val title: String="",val categories: List<String> = emptyList(),val iconColor: String="#16A34A")
private fun pointJson(p: InkPoint)=json("x" to p.x,"y" to p.y,"pressure" to p.pressure)
private fun inkJson(strokes: List<InkStroke>)=JsonArray(strokes.map { stroke -> JsonArray(stroke.points.map(::pointJson)) })
private fun inkStrokes(value: JsonArray,margins: Boolean=false)=value.mapNotNull { raw ->
 val array=if(raw is JsonObject) raw.arr("points") else raw as? JsonArray?:return@mapNotNull null
 val points=array.mapNotNull point@ { p ->
  val a=p as? JsonArray;val o=p as? JsonObject
  val x=((o?.get("x")?:a?.getOrNull(0)) as? JsonPrimitive)?.floatOrNull
  val y=((o?.get("y")?:a?.getOrNull(1)) as? JsonPrimitive)?.floatOrNull
  val pressure=((o?.get("pressure")?:a?.getOrNull(2)) as? JsonPrimitive)?.floatOrNull?:1f
  if(x==null||y==null||!x.isFinite()||!y.isFinite()||!pressure.isFinite()) null else workspacePoint(x,y,margins).copy(pressure=pressure.coerceIn(.1f,1f))
 }
 if(points.isEmpty()) null else InkStroke(points.take(12000),(raw as? JsonObject)?.str("color")?.takeIf { it.isNotBlank()&&it!="#1C272B" })
}
fun ReaderInk.toJson()=json("id" to id,"page" to page,"outlines" to inkJson(outlines),"note" to JsonArray(note.map { json("points" to JsonArray(it.points.map(::pointJson)),"color" to (it.color?:"#1C272B")) }),"verses" to verses,"updatedAt" to updatedAt,"deletedAt" to deletedAt,"source" to source,"tool" to tool,"color" to color,"width" to width,"penStyle" to penStyle,"text" to text,"anchor" to anchor,"favorite" to favorite,"masteredAt" to masteredAt,"createdAt" to createdAt,"title" to title,"categories" to categories,"iconColor" to iconColor)
fun ReaderInk.toSourceJson()=toJson().with("points" to JsonArray(outlines.firstOrNull()?.points.orEmpty().map(::pointJson)),"handwriting" to JsonArray(note.map { json("points" to JsonArray(it.points.map(::pointJson)),"color" to (it.color?:"#1C272B")) }),"related" to verses.firstOrNull()?.let { json("verseId" to it) }.let { it?:JsonNull })
fun inkFromJson(r: JsonObject): ReaderInk? {
 if(r.str("id").isBlank()||r.num("page") !in 1..604) return null
 val outline=if(r["outlines"] is JsonArray) inkStrokes(r.arr("outlines"),true) else inkStrokes(JsonArray(listOf(r.arr("points"))),true)
 val anchor=r["anchor"] as? JsonObject
 val ids=r.arr("verses").mapNotNull { (it as? JsonPrimitive)?.intOrNull }.ifEmpty { listOf(anchor?.num("verseId")?:r.obj("related").num("verseId")).filter { it in 1..6236 } }
 return ReaderInk(r.str("id"),r.num("page"),outline,inkStrokes(if(r["handwriting"] is JsonArray) r.arr("handwriting") else r.arr("note")),ids,r.str("updatedAt"),r.str("deletedAt").ifBlank { null },r.str("source","coranTest"),r.str("tool","pen"),r.str("color","#AD2A30"),((r["width"] as? JsonPrimitive)?.floatOrNull?:.003f).coerceIn(.001f,.06f),r.str("penStyle","ink"),r.str("text"),anchor,r.flag("favorite"),r.str("masteredAt").ifBlank { null },r.str("createdAt").ifBlank { null },r.str("title"),r.arr("categories").mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }.distinct(),r.str("iconColor","#16A34A").takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }?:"#16A34A")
}
fun allReaderInk(s: JsonObject,source: String?=null): List<ReaderInk> = mergeSourceAnnotations(s.obj("readerInk"),s.obj("readerAnnotations")).values.mapNotNull { (it as? JsonObject)?.let(::inkFromJson) }.filter { it.deletedAt==null&&(source==null||it.source==source) }
fun readerInk(s: JsonObject,page: Int,source: String="coranTest")=allReaderInk(s,source).filter { it.page==page }
fun newReaderInk(page: Int,stroke: InkStroke,verses: List<Int>,now: String=annotationTime()): ReaderInk { require(page in 1..604&&stroke.points.isNotEmpty());return ReaderInk(UUID.randomUUID().toString(),page,listOf(stroke),emptyList(),verses.distinct().filter { it in 1..6236 },now) }
fun saveReaderInk(s: JsonObject,ink: ReaderInk,restore: Boolean=false): JsonObject {
 val existing=s.obj("readerAnnotations").obj(ink.id).ifEmpty { s.obj("readerInk").obj(ink.id) }
 if(!restore&&existing.str("deletedAt").isNotBlank()) return s
 if(runCatching { Instant.parse(existing.str("updatedAt")).isAfter(Instant.parse(ink.updatedAt)) }.getOrDefault(false)) return s
 return touch(s.with("readerInk" to s.obj("readerInk").with(ink.id to ink.toJson()),"readerAnnotations" to s.obj("readerAnnotations").with(ink.id to ink.toSourceJson())))
}
fun deleteReaderInk(s: JsonObject,id: String,now: String=annotationTime()): JsonObject {
 val old=s.obj("readerAnnotations").obj(id).ifEmpty { s.obj("readerInk").obj(id) };if(old.isEmpty()) return s
 val deleted=old.with("deletedAt" to JsonPrimitive(now),"updatedAt" to JsonPrimitive(now),"outlines" to JsonArray(emptyList()),"note" to JsonArray(emptyList()),"handwriting" to JsonArray(emptyList()),"points" to JsonArray(emptyList()),"text" to JsonPrimitive(""))
 return touch(s.with("readerInk" to s.obj("readerInk").with(id to deleted),"readerAnnotations" to s.obj("readerAnnotations").with(id to deleted)))
}
fun mergeReaderInkCopies(local: JsonObject,remote: JsonObject): JsonObject = JsonObject((local.keys+remote.keys).filter { local[it] is JsonObject||remote[it] is JsonObject }.associateWith { id ->
 val candidates=listOfNotNull(local[id] as? JsonObject,remote[id] as? JsonObject);val deleted=candidates.filter { it.str("deletedAt").isNotBlank() }
 (if(deleted.isNotEmpty()) deleted else candidates).maxBy { runCatching { Instant.parse(it.str("updatedAt")) }.getOrDefault(Instant.EPOCH) }
})
/** Expo uses timestamp order, deletion on equal timestamps, then deterministic JSON order. */
fun mergeSourceAnnotations(local: JsonObject,remote: JsonObject): JsonObject = JsonObject((local.keys+remote.keys).mapNotNull { id ->
 val rows=listOfNotNull(local[id] as? JsonObject,remote[id] as? JsonObject)
 rows.maxWithOrNull(compareBy<JsonObject> { runCatching { Instant.parse(it.str("updatedAt")) }.getOrDefault(Instant.EPOCH) }.thenBy { it.str("deletedAt").isNotBlank() }.thenBy { it.toString() })?.let { id to it }
}.toMap())
fun annotationTime(previous: String?=null): String {
 val old=runCatching { Instant.parse(previous).toEpochMilli() }.getOrDefault(0L)
 val next=Instant.ofEpochMilli(maxOf(System.currentTimeMillis(),old+1))
 return java.time.format.DateTimeFormatterBuilder().appendInstant(3).toFormatter().format(next)
}
