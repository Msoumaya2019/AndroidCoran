package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.ContentService
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun ContentsScreen(vm: CoranViewModel,initialId: String?=null) {
    val owner by vm.repo.user.collectAsStateWithLifecycle();val service=remember(vm) { ContentService(vm.repo) }
    var detail by remember(owner,initialId) { mutableStateOf<JsonObject?>(null) }
    var detailError by remember(owner,initialId) { mutableStateOf("") }
    var detailLoading by remember(owner,initialId) { mutableStateOf(initialId!=null) }
    var detailAttempt by remember { mutableIntStateOf(0) }
    var recordingActive by remember { mutableStateOf(false) }
    var contents by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) };var categories by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) };var favorites by remember(owner) { mutableStateOf<List<String>>(emptyList()) }
    var type by rememberSaveable { mutableStateOf("reminder") };var daily by rememberSaveable { mutableStateOf(true) };var favoriteOnly by rememberSaveable { mutableStateOf(false) };var category by rememberSaveable { mutableStateOf<String?>(null) };var menu by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) };var loading by remember { mutableStateOf(false) };var offset by remember { mutableIntStateOf(0) };var recording by remember { mutableStateOf<JsonObject?>(null) }
    val context=LocalContext.current
    fun load(append: Boolean=false) { vm.action {
        loading=true
        try {
            if(!append) { categories=service.categories();favorites=service.favorites();offset=0 }
            val rows=if(daily) service.daily() else service.list(type,category,favoriteOnly,if(append) offset else 0)
            contents=if(append) (contents+rows).distinctBy { it.str("id") } else rows
            offset+=rows.size;more=!daily&&rows.size==30
        } finally { loading=false }
    } }
    LaunchedEffect(owner,type,daily,favoriteOnly,category) { if(initialId==null) load() }
    LaunchedEffect(owner,initialId,detailAttempt) {
        if(initialId!=null) {
            detailLoading=true;detailError=""
            try {
                val result=service.getContent(initialId)
                if(vm.repo.user.value==owner) { detail=result;val ids=service.favorites();if(vm.repo.user.value==owner) favorites=ids;if(result==null) detailError="Cette invocation n’est plus disponible." }
            } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e;detailError=e.message?:"Invocation indisponible" }
            finally { detailLoading=false }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if(initialId==null) {
        Row { FilterChip(selected=daily,onClick={daily=true;favoriteOnly=false},label={Text("Aujourd’hui")});FilterChip(selected=!daily&&type=="reminder",onClick={type="reminder";daily=false},label={Text("Rappels")});FilterChip(selected=!daily&&type=="invocation",onClick={type="invocation";daily=false},label={Text("Invocations")}) }
        Row {
            FilterChip(selected=favoriteOnly,onClick={favoriteOnly=!favoriteOnly;daily=false},label={Text("Favoris")})
            Box { TextButton(onClick={menu=true}) { Text(categories.firstOrNull { it.str("id")==category }?.str("name")?:"Toutes les catégories") };DropdownMenu(expanded=menu,onDismissRequest={menu=false}) { DropdownMenuItem(text={Text("Toutes")},onClick={category=null;menu=false;daily=false});categories.filter { it.str("type")==type }.forEach { row -> DropdownMenuItem(text={Text(row.str("name"))},onClick={category=row.str("id");menu=false;daily=false}) } } }
            TextButton(enabled=!loading,onClick={load()}) { Text("Actualiser") }
        }
        }
        if(initialId!=null&&detailError.isNotBlank()) { Text(detailError);TextButton(onClick={detailAttempt++}) { Text("Réessayer") } }
        if(loading||detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(if(initialId==null) contents else listOfNotNull(detail),key={it.str("id")}) { content ->
                Panel(content.str("title",if(content.str("type")=="invocation") "Invocation" else "Rappel"),content.str("french_text")) {
                    if(content.str("arabic_text").isNotBlank()) CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) { Text(content.str("arabic_text"),Modifier.fillMaxWidth(),fontFamily=arabicInterfaceFont,fontSize=24.sp,lineHeight=42.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
                    if(content.str("phonetic_text").isNotBlank()) Text(content.str("phonetic_text"))
                    if(content.str("explanation").isNotBlank()) Text(content.str("explanation"))
                    Text("${content.str("source")} ${content.str("reference")}",fontSize=12.sp)
                    if(content.str("image_url").isNotBlank()) ContentImage(vm,content.str("image_url"),content.str("title"),Modifier.fillMaxWidth().heightIn(max=280.dp))
                    Row {
                        TextButton(onClick={vm.action { val enabled=content.str("id") !in favorites;service.favorite(content.str("id"),enabled);favorites=if(enabled) (favorites+content.str("id")).distinct() else favorites-content.str("id");if(favoriteOnly&&!enabled) contents=contents.filter { it.str("id")!=content.str("id") } }}) { Text(if(content.str("id") in favorites) "Retirer des favoris" else "Favori") }
                        if(content.str("audio_url").startsWith("https://")) ContentAudioButton(vm,content)
                        TextButton(onClick={ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,sharedContentText(content)),"Partager")) }) { Text("Partager") }
                        if(content.str("type")=="invocation") TextButton(onClick={recording=content}) { Text("Enregistrer") }
                    }
                }
            }
            if(initialId==null&&!loading&&contents.isEmpty()) item { Text("Aucun contenu chargé pour cette sélection") }
            if(initialId==null&&more) item { Button(enabled=!loading,onClick={load(true)}) { Text("Charger la suite") } }
        }
    }
    recording?.let { content -> AlertDialog(onDismissRequest={if(!recordingActive) recording=null},title={Text(content.str("title","Invocation"))},text={ Column(Modifier.verticalScroll(rememberScrollState())) { RecorderPanel(vm,content,onActiveChanged={recordingActive=it}) } },confirmButton={TextButton(enabled=!recordingActive,onClick={recording=null}) { Text("Fermer") }}) }
}
