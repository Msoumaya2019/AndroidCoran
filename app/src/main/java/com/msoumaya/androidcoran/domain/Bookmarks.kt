package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.Instant

class Bookmarks(private val quran: Quran) {
    fun save(state: JsonObject,id: Int,source: String,page: Int,at: String=Instant.now().toString()): JsonObject {
        require(id in 1..6236)
        val verse=quran.verse(id);val old=state.obj("bookmarks").obj(id.toString())
        val bookmark=json("verseId" to id,"surah" to verse.surah,"ayah" to verse.ayah,"page" to quran.page(id),
            "sourcePages" to old.obj("sourcePages").with(source to JsonPrimitive(page)),"createdAt" to old.str("createdAt",at),"updatedAt" to at)
        return state.with("updatedAt" to JsonPrimitive(at),"lastRead" to json("page" to page,"verseId" to id,"readAt" to at),
            "bookmarks" to state.obj("bookmarks").with(id.toString() to bookmark))
    }
    fun delete(state: JsonObject,id: Int,at: String=Instant.now().toString()): JsonObject {
        val bookmark=state.obj("bookmarks")[id.toString()] as? JsonObject ?: return state
        return state.with("updatedAt" to JsonPrimitive(at),"bookmarks" to state.obj("bookmarks").with(id.toString() to bookmark.with("deletedAt" to JsonPrimitive(at),"updatedAt" to JsonPrimitive(at))))
    }
    fun use(state: JsonObject,id: Int,page: Int,at: String=Instant.now().toString()): JsonObject {
        val bookmark=state.obj("bookmarks")[id.toString()] as? JsonObject ?: return state
        if(!bookmark.str("deletedAt").isBlank()) return state
        return state.with("updatedAt" to JsonPrimitive(at),"lastRead" to json("page" to page,"verseId" to id,"readAt" to at),
            "bookmarks" to state.obj("bookmarks").with(id.toString() to bookmark.with("lastUsedAt" to JsonPrimitive(at),"updatedAt" to JsonPrimitive(at))))
    }
    fun visible(state: JsonObject): List<JsonObject> = state.obj("bookmarks").values.map { it.jsonObject }
        .filter { it.str("deletedAt").isBlank() }.sortedByDescending { it.str("lastUsedAt",it.str("updatedAt")) }
}

fun mergeBookmarks(first: JsonObject,second: JsonObject): JsonObject {
    val result=first.toMutableMap()
    second.forEach { (id,item) -> if(result[id]==null||item.jsonObject.str("updatedAt")>result[id]!!.jsonObject.str("updatedAt")) result[id]=item }
    return JsonObject(result)
}
