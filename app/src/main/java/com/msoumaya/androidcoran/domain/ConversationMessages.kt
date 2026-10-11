package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.JsonObject

/** Existing history remains visible; newer snapshots replace edited/deleted rows by id. */
fun mergeConversationMessages(existing: List<JsonObject>,incoming: List<JsonObject>): List<JsonObject> {
    val rows=linkedMapOf<String,JsonObject>()
    (existing+incoming).forEach { row -> row.str("id").takeIf { it.isNotBlank() }?.let { rows[it]=row } }
    return rows.values.sortedBy { it.str("created_at") }
}
