package com.msoumaya.androidcoran.data

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.LocalDate

class ContentService(private val repo: Repository) {
    suspend fun getContent(id: String): JsonObject? {
        require(id.isNotBlank())
        return repo.query("daily_contents",eq=mapOf("id" to id),size=1,authenticated=false,cacheResult=true).singleOrNull()
    }
    suspend fun daily()=repo.cachedRpc("daily_content_for_date",json("p_date" to LocalDate.now().toString()),authenticated=false).jsonArray.map { it.jsonObject }
    suspend fun categories()=repo.query("content_categories",eq=mapOf("is_active" to "true"),orderBy="display_order",ascending=true,authenticated=false,cacheResult=true,size=300)
    suspend fun favorites(): List<String> {
        val owner=repo.user.value
        return if(owner==null) (repo.cached("guest-content-favorites") as? JsonArray)?.map { it.jsonPrimitive.content }?:emptyList()
        else repo.query("content_favorites",eq=mapOf("user_id" to owner),orderBy="content_id",size=300,cacheResult=true).map { it.str("content_id") }
    }
    suspend fun favorite(id: String,enabled: Boolean) {
        val owner=repo.user.value
        if(owner==null) { val current=favorites();repo.cache("guest-content-favorites",element(if(enabled) (current+id).distinct() else current-id)) }
        else if(enabled) repo.upsertRows("content_favorites",json("user_id" to owner,"content_id" to id))
        else repo.deleteRows("content_favorites",mapOf("user_id" to owner,"content_id" to id))
    }
    suspend fun list(type: String,category: String?,favoriteOnly: Boolean,offset: Int): List<JsonObject> {
        require(type in listOf("reminder","invocation"))
        val filters=mutableMapOf("type" to type)
        if(!favoriteOnly) filters["is_active"]="true"
        if(category!=null) filters["category_id"]=category
        return repo.query("daily_contents",eq=filters,ids=if(favoriteOnly) "id" to favorites() else null,offset=offset,size=30,authenticated=false,cacheResult=true)
    }
}
