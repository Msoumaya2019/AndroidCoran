package com.msoumaya.androidcoran.domain

import android.content.Context
import kotlinx.serialization.json.*

fun JsonObject.obj(key: String) = this[key] as? JsonObject ?: JsonObject(emptyMap())
fun JsonObject.arr(key: String) = this[key] as? JsonArray ?: JsonArray(emptyList())
fun JsonObject.str(key: String, fallback: String = "") = (this[key] as? JsonPrimitive)?.contentOrNull ?: fallback
fun JsonObject.num(key: String, fallback: Int = 0) = (this[key] as? JsonPrimitive)?.intOrNull ?: fallback
fun JsonObject.flag(key: String, fallback: Boolean = false) = (this[key] as? JsonPrimitive)?.booleanOrNull ?: fallback
fun JsonObject.with(vararg entries: Pair<String, JsonElement>) = JsonObject(toMutableMap().apply { putAll(entries) })
fun json(vararg entries: Pair<String, Any?>): JsonObject = JsonObject(entries.associate { it.first to element(it.second) })
fun element(value: Any?): JsonElement = when(value) {
    null -> JsonNull; is JsonElement -> value; is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value); is String -> JsonPrimitive(value)
    is Iterable<*> -> JsonArray(value.map(::element)); else -> error("Unsupported JSON value")
}
data class Verse(val id: Int, val surah: Int, val ayah: Int, val text: String)
data class VerseRange(val start: Int, val end: Int) { init { require(start in 1..6236 && end in start..6236) }; val ids get() = (start..end).toList() }
data class Surah(val number: Int, val name: String, val arabic: String, val meaning: String, val range: VerseRange)

class Quran(private val asset: (String)->JsonElement) {
    constructor(context: Context): this({ path -> context.assets.open(path).bufferedReader().use { Json.parseToJsonElement(it.readText()) } })
    private val meta = asset("meta.json").jsonObject
    val verses = asset("verses.json").jsonArray.mapIndexed { i,v -> v.jsonObject.let { Verse(i+1,it.num("surah"),it.num("ayah"),it.str("text")) } }
    val surahs = meta.arr("surahs").map { v -> v.jsonObject.let { Surah(it.num("number"),it.str("name"),it.str("arabic"),it.str("meaning"),VerseRange(it.num("start"),it.num("end"))) } }
    val juzs = divisions(meta.arr("juzs"))
    val quarters = divisions(meta.arr("quarters"))
    val hizbs = quarters.chunked(4).map { VerseRange(it.first().start,it.last().end) }
    val halves = quarters.chunked(2).map { VerseRange(it.first().start,it.last().end) }
    val pages = asset("pages.json").jsonArray.map { p -> p.jsonObject.let { val a=it.arr("first");val b=it.arr("last");VerseRange(id(a[0].jsonPrimitive.int,a[1].jsonPrimitive.int),id(b[0].jsonPrimitive.int,b[1].jsonPrimitive.int)) } }
    val weights = verses.map { v -> v.text.count { it in '\u0621'..'\u064A' }.coerceAtLeast(1) }
    val totalVolume = weights.sum()
    private val translation = asset("translation-fr-rashid.json")
    private val index = asset("qcf/verse-index.json").jsonObject
    private val zipBounds by lazy { asset("quran-tests/coran_1441-bounds.json").jsonObject }
    private val zipIndex by lazy { val map=mutableMapOf<Int,MutableList<Int>>();zipBounds.forEach { (page,rows) -> rows.jsonArray.forEach { row -> val a=row.jsonArray;val id=id(a[0].jsonPrimitive.int,a[1].jsonPrimitive.int);val pages=map.getOrPut(id) { mutableListOf() };if(page.toInt() !in pages) pages+=page.toInt() } };map }
    fun divisions(a: JsonArray) = a.map { VerseRange(it.jsonObject.num("start"),it.jsonObject.num("end")) }
    fun id(surah: Int, ayah: Int): Int { val s=surahs[surah-1];require(ayah in 1..s.range.ids.size);return s.range.start+ayah-1 }
    fun verse(id: Int) = verses[id-1]
    fun surah(id: Int) = surahs[verse(id).surah-1]
    fun page(id: Int) = pages.indexOfFirst { id in it.start..it.end }+1
    fun qcfPage(id: Int) = index.obj("${verse(id).surah}:${verse(id).ayah}").arr("pages").first().jsonPrimitive.int
    fun sourcePage(id: Int,source: String) = when(source) { "coranTest"->qcfPage(id);"coran_1441"->zipIndex[id]?.firstOrNull()?:1;else->page(id) }
    fun sourceRange(page: Int,source: String): VerseRange = when(source) {
        "coran_1441" -> zipBounds[page.toString()]!!.jsonArray.map { val a=it.jsonArray;id(a[0].jsonPrimitive.int,a[1].jsonPrimitive.int) }.let { VerseRange(it.min(),it.max()) }
        "coranTest" -> index.values.filter { it.jsonObject.arr("pages").any { p -> p.jsonPrimitive.int==page } }.map { it.jsonObject.num("id") }.let { VerseRange(it.min(),it.max()) }
        else -> pages[page-1]
    }
    fun volume(ids: List<Int>) = ids.sumOf { weights[it-1] }
    fun reference(r: VerseRange): String = if(verse(r.start).surah==verse(r.end).surah) "${surah(r.start).name} ${verse(r.start).ayah}–${verse(r.end).ayah}" else "${surah(r.start).name} ${verse(r.start).ayah} → ${surah(r.end).name} ${verse(r.end).ayah}"
    fun french(id: Int): String = when(translation) {
        is JsonArray -> translation.getOrNull(id-1)?.let { if(it is JsonPrimitive) it.content else it.jsonObject.str("translation",it.jsonObject.str("text")) } ?: ""
        is JsonObject -> translation.str("${verse(id).surah}:${verse(id).ayah}",translation.str(id.toString()))
        else -> ""
    }
    fun qcfData(context: Context,page: Int) = context.assets.open("qcf/$page.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
}
