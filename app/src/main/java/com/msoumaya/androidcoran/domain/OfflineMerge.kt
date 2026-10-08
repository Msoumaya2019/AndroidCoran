package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.Instant

/** Port of core/offlineMerge.ts. A null Kotlin value is an absent JS property. */
private fun mergeField(base: JsonElement?,local: JsonElement?,remote: JsonElement?,path: String=""): JsonElement? {
    if(local==base) return remote
    if(remote==base||local==remote) return local
    if(local is JsonArray&&remote is JsonArray) {
        if((local+remote).all { it is JsonObject && (it["id"] as? JsonPrimitive)?.isString==true }) {
            fun keyed(rows: JsonElement?) = (rows as? JsonArray)?.associate { it.jsonObject.str("id") to it }?:emptyMap()
            val b=keyed(base);val l=keyed(local);val r=keyed(remote)
            return JsonArray((l.keys+r.keys).distinct().mapNotNull { id ->
                if(l[id]==null&&(r[id] as? JsonObject)?.str("status")=="done") r[id] else mergeField(b[id],l[id],r[id],"$path.$id")
            })
        }
        if(path.endsWith("validations")||path.endsWith("History")) return JsonArray((remote+local).distinct())
        if(path.endsWith("readPages")||path.endsWith("completed")&&(local+remote).all { (it as? JsonPrimitive)?.intOrNull!=null })
            return JsonArray((local+remote).distinct().sortedBy { it.jsonPrimitive.int })
        return local
    }
    if(remote is JsonObject&&(local is JsonObject||local==null&&base is JsonObject)) {
        val b=base as? JsonObject?:json();val l=local as? JsonObject?:json()
        val result=(b.keys+l.keys+remote.keys).distinct().mapNotNull { key -> mergeField(b[key],l[key],remote[key],"$path.$key")?.let { key to it } }.toMap().toMutableMap()
        val through=(result["through"] as? JsonPrimitive)?.intOrNull;val end=(result["end"] as? JsonPrimitive)?.intOrNull
        if(through!=null&&end!=null) result["status"]=JsonPrimitive(if(through>=end) "completed" else "partial")
        return JsonObject(result)
    }
    if(path.endsWith(".through")) {
        val l=(local as? JsonPrimitive)?.intOrNull;val r=(remote as? JsonPrimitive)?.intOrNull
        if(l!=null&&r!=null) return JsonPrimitive(maxOf(l,r))
    }
    if(path.endsWith(".status")&&(local==JsonPrimitive("done")||remote==JsonPrimitive("done"))) return JsonPrimitive("done")
    return local
}

fun mergeOfflineState(base: JsonObject?,local: JsonObject,remote: JsonObject?,now: Instant=Instant.now()): JsonObject {
    if(remote==null||base==null||base.str("userId")!=local.str("userId")||remote.str("userId")!=local.str("userId")) return local
    if(base.flag("onboardingDone")&&!local.flag("onboardingDone")) return local
    val merged=mergeField(base,local,remote)!!.jsonObject
    fun timestamp(state: JsonObject) = Instant.parse(state.str("updatedAt")).toEpochMilli()
    val updated=java.time.format.DateTimeFormatterBuilder().appendInstant(3).toFormatter().format(Instant.ofEpochMilli(maxOf(now.toEpochMilli(),timestamp(local)+1,timestamp(remote)+1)))
    return merged.with("userId" to local["userId"]!!,"updatedAt" to JsonPrimitive(updated))
}
