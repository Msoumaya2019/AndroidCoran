package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.ContentService
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

@Composable fun HomeDailyContents(vm: CoranViewModel,onOpen: (String)->Unit) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var attempt by remember { mutableIntStateOf(0) }
    var rows by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember(owner) { mutableStateOf(true) }
    var error by remember(owner) { mutableStateOf("") }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            attempt++;var date=LocalDate.now()
            while(isActive) { delay(60_000);val today=LocalDate.now();if(today!=date) { date=today;attempt++ } }
        }
    }
    LaunchedEffect(owner,attempt) {
        val service=ContentService(vm.repo)
        if(rows.isEmpty()) rows=service.cachedDaily()
        loading=true
        try { val result=service.daily();if(vm.repo.user.value==owner) { rows=result;error="" } }
        catch(e: Exception) { if(e is CancellationException) throw e;error="Contenu du jour indisponible hors connexion." }
        finally { loading=false }
    }
    TodayContentCarousel(rows,loading,error,onOpen,onRetry={attempt++},image={ row ->
        Box(Modifier.size(88.dp,98.dp)) {
            AssetImage("illustrations/reading.png",null,Modifier.matchParentSize())
            if(row.str("image_url").isNotBlank()) ContentImage(vm,row.str("image_url"),row.str("title"),Modifier.matchParentSize())
        }
    })
}

@Composable fun TodayContentCarousel(rows: List<JsonObject>,loading: Boolean,error: String,onOpen: (String)->Unit,onRetry: ()->Unit,image: @Composable (JsonObject)->Unit={}) {
    val entries=remember(rows) { dailyContentEntries(rows) }
    val pager=rememberPagerState(pageCount={entries.size})
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text(if(entries.getOrNull(pager.currentPage)?.str("type")=="invocation") "Invocation du jour" else "Rappel du jour",style=MaterialTheme.typography.titleMedium)
    if(entries.isEmpty()) {
        Panel(if(loading) "Chargement du contenu du jour…" else if(error.isNotBlank()) error else "Aucun contenu disponible aujourd’hui.") {
            if(error.isNotBlank()) TextButton(enabled=!loading,onClick=onRetry) { Text("Réessayer") }
        }
    } else {
        HorizontalPager(state=pager,modifier=Modifier.fillMaxWidth().testTag("daily-content-pager"),key={entries[it].str("id")}) { index ->
            val row=entries[index]
            Card(Modifier.fillMaxWidth().clickable { onOpen(row.str("id")) }) {
                Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    image(row)
                    Column(Modifier.weight(1f)) {
                        Text(if(row.str("type")=="invocation") "Invocation du jour" else "Rappel du jour",style=MaterialTheme.typography.titleSmall)
                        if(row.str("arabic_text").isNotBlank()) Text(row.str("arabic_text"),Modifier.fillMaxWidth(),style=LocalTextStyle.current.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Rtl),fontFamily=arabicInterfaceFont,fontSize=22.sp,lineHeight=36.sp,textAlign=TextAlign.Right,maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(row.str("phonetic_text").isNotBlank()) Text(row.str("phonetic_text"),fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(row.str("french_text"),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(listOf(row.str("source"),row.str("reference")).filter { it.isNotBlank() }.joinToString(" · "),fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text("Ouvrir la fiche ›",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if(entries.size>1) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterHorizontally)) {
            entries.indices.forEach { index -> Box(Modifier.size(6.dp).background(if(pager.currentPage==index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,CircleShape)) }
        }
    }
    }
}
