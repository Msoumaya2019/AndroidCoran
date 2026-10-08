package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*

fun recordingPayload(row: JsonObject,owner: String,path: String): JsonObject {
    require(row.str("user_id")==owner&&owner.isNotBlank())
    val invocation=row.str("recording_type")=="invocation"
    if(invocation) require(row.obj("invocation_snapshot").str("id").isNotBlank()) else VerseRange(row.num("start_verse_id"),row.num("end_verse_id"))
    require(row.num("duration_ms")>0)
    return json("id" to row.str("id"),"user_id" to owner,"start_verse_id" to if(invocation) null else row.num("start_verse_id"),"end_verse_id" to if(invocation) null else row.num("end_verse_id"),"duration_ms" to row.num("duration_ms"),"storage_path" to path,"created_at" to row.str("created_at"),"recording_type" to if(invocation) "invocation" else "quran","invocation_id" to if(invocation) row.obj("invocation_snapshot").str("id") else null,"invocation_snapshot" to if(invocation) row.obj("invocation_snapshot") else null)
}
