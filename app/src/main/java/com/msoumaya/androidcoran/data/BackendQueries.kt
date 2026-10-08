package com.msoumaya.androidcoran.data

import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

suspend fun Repository.query(table: String,eq: Map<String,String> = emptyMap(),ids: Pair<String,List<String>>?=null,
    before: Pair<String,String>?=null,after: Pair<String,String>?=null,orderBy: String="created_at",ascending: Boolean=false,
    offset: Int=0,size: Int=50,authenticated: Boolean=true,cacheResult: Boolean=false): List<JsonObject> {
    require(offset>=0&&size in 1..300)
    if(authenticated) check(user.value!=null) { "Connexion nécessaire" }
    if(ids?.second?.isEmpty()==true) return emptyList()
    val owner=user.value
    val key="${owner?:"guest"}:query:$table:${eq.toSortedMap()}:$ids:$before:$after:$orderBy:$ascending:$offset:$size"
    return try {
        val result=db().from(table).select {
            filter { eq.forEach { (column,value) -> eq(column,value) };ids?.let { isIn(it.first,it.second) };before?.let { lt(it.first,it.second) };after?.let { gte(it.first,it.second) } }
            order(orderBy,if(ascending) Order.ASCENDING else Order.DESCENDING)
            range(offset.toLong(),(offset+size-1).toLong())
        }.decodeList<JsonObject>()
        check(owner==user.value) { "Le compte a changé" }
        if(cacheResult) cache(key,JsonArray(result))
        result
    } catch(e: Exception) {
        if(e is CancellationException||owner!=user.value||!cacheResult) throw e
        (cached(key) as? JsonArray)?.map { it.jsonObject } ?: throw e
    }
}
suspend fun Repository.updateRows(table: String,data: JsonObject,eq: Map<String,String>) {
    check(user.value!=null);require(eq.isNotEmpty());db().from(table).update(data) { filter { eq.forEach { (k,v)->eq(k,v) } } }
}
suspend fun Repository.deleteRows(table: String,eq: Map<String,String>) {
    check(user.value!=null);require(eq.isNotEmpty());db().from(table).delete { filter { eq.forEach { (k,v)->eq(k,v) } } }
}
suspend fun Repository.upsertRows(table: String,data: JsonObject,conflict: String?=null) {
    check(user.value!=null);db().from(table).upsert(data) { if(conflict!=null) onConflict=conflict }
}

fun Repository.observeTable(table: String): Flow<Unit> = channelFlow {
    val owner=user.value?:error("Connexion nécessaire")
    val client=db();val channel=client.channel("android-$table-${java.util.UUID.randomUUID()}")
    val changes=channel.postgresChangeFlow<PostgresAction>(schema="public") { this.table=table }
    val collector=launch { changes.collect { if(owner==user.value) send(Unit) } }
    try { channel.subscribe(blockUntilSubscribed=true);send(Unit);awaitCancellation() }
    finally { collector.cancel();withContext(NonCancellable) { channel.unsubscribe() } }
}
