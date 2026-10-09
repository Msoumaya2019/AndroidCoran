package com.msoumaya.androidcoran.domain

import kotlinx.serialization.json.JsonObject

fun sharedContentText(content: JsonObject): String =
    listOf("title","arabic_text","phonetic_text","french_text","explanation")
        .map { content.str(it) }
        .plus("Source : ${content.str("source")} ${content.str("reference")}")
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
