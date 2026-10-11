package com.msoumaya.androidcoran.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject

fun dailyContentEntries(rows: List<JsonObject>): List<JsonObject> =
    listOf("reminder","invocation").mapNotNull { type -> rows.firstOrNull { it.str("type")==type&&it.str("id").isNotBlank() } }

data class ContentSelection(val owner: String?,val type: String,val daily: Boolean,val favorites: Boolean,val category: String?)
data class ContentPage(val rows: List<JsonObject> = emptyList(),val offset: Int=0,val more: Boolean=false,val loading: Boolean=false,val error: String="")

/** An obsolete request cannot publish rows or clear the loading state of its successor. */
class ContentBrowser {
    private val mutable=MutableStateFlow(ContentPage())
    val state=mutable.asStateFlow()
    private var generation=0L
    private var selection: ContentSelection?=null
    private var job: Job?=null
    fun load(scope: CoroutineScope,query: ContentSelection,append: Boolean=false,fetch: suspend (ContentSelection,Int)->List<JsonObject>) {
        if(append&&(selection!=query||mutable.value.loading||!mutable.value.more)) return
        generation++;val token=generation
        job?.cancel();selection=query
        val previous=if(append) mutable.value else ContentPage()
        mutable.value=previous.copy(loading=true,error="")
        job=scope.launch {
            try {
                val rows=fetch(query,if(append) previous.offset else 0)
                if(token==generation) mutable.value=ContentPage(
                    rows=if(append) (previous.rows+rows).distinctBy { it.str("id") } else rows.distinctBy { it.str("id") },
                    offset=previous.offset+rows.size,more=!query.daily&&rows.size==30)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(token==generation) mutable.value=previous.copy(error=e.message?:"Contenus indisponibles") }
            finally { if(token==generation) mutable.value=mutable.value.copy(loading=false) }
        }
    }
    fun close() { generation++;job?.cancel() }
}
