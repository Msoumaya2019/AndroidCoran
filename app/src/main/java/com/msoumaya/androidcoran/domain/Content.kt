package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.*
import java.time.LocalDate

fun contentMediaPath(url: String,backend: String): String? {
    val root="$backend/storage/v1/object/public/daily-content-media/"
    return if(url.startsWith(root)) url.removePrefix(root).takeIf { it.isNotBlank() } else null
}
fun validateAdminContent(d: JsonObject,date: String): JsonObject {
    require(d.str("type") in listOf("reminder","invocation")&&d.str("category_id").isNotBlank()&&d.str("french_text").isNotBlank()&&d.str("source").isNotBlank())
    if(d.str("type")=="invocation") require(d.str("arabic_text").isNotBlank()&&d.str("phonetic_text").isNotBlank())
    listOf("image_url","audio_url").forEach { key -> require(d.str(key).isBlank()||Regex("https://\\S+").matches(d.str(key))) }
    if(date.isNotBlank()) require(LocalDate.parse(date).toString()==date)
    return d
}
