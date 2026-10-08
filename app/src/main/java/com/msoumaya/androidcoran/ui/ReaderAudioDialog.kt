package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.AudioPreferences
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

@Composable fun ReaderAudioDialog(vm: CoranViewModel,initialRange: VerseRange,pageRange: VerseRange,reciterId: String,onReciter: (String)->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current;val store=remember { AudioPreferences(context) };val scope=rememberCoroutineScope();val q=vm.repo.quran
    val saved by produceState<RepeatPreferences?>(null) { value=store.load() }
    var loaded by rememberSaveable { mutableStateOf(false) };var choice by rememberSaveable { mutableStateOf("3") };var custom by rememberSaveable { mutableStateOf("20") }
    var each by rememberSaveable { mutableStateOf(false) };var gap by rememberSaveable { mutableIntStateOf(0) };var speed by rememberSaveable { mutableFloatStateOf(1f) };var autoStop by rememberSaveable { mutableStateOf(true) }
    var selection by rememberSaveable { mutableStateOf("session") };var surah by rememberSaveable { mutableStateOf(q.verse(initialRange.start).surah.toString()) };var first by rememberSaveable { mutableStateOf(q.verse(initialRange.start).ayah.toString()) };var last by rememberSaveable { mutableStateOf(q.verse(initialRange.end).let { if(it.surah==q.verse(initialRange.start).surah) it.ayah.toString() else first }) }
    var launching by remember { mutableStateOf(false) }
    fun preferences()=RepeatPreferences(choice,custom,if(each) RepeatMode.EACH_VERSE else RepeatMode.PASSAGE,gap,speed,autoStop)
    LaunchedEffect(saved) { val prefs=saved;if(prefs!=null&&!loaded) { choice=prefs.countChoice;custom=prefs.customCount;each=prefs.mode==RepeatMode.EACH_VERSE;gap=prefs.gap;speed=prefs.speed;autoStop=prefs.autoStop;loaded=true } }
    LaunchedEffect(choice,custom,each,gap,speed,autoStop,loaded) { if(loaded) store.save(preferences()) }
    fun selectedRange(): VerseRange?=when(selection) {
        "session"->initialRange;"page"->pageRange
        else->{ val chapter=q.surahs.getOrNull((surah.toIntOrNull()?:0)-1);if(chapter==null) null else if(selection=="surah") chapter.range else { val start=first.toIntOrNull();val end=if(selection=="verse") start else last.toIntOrNull();if(start==null||end==null||start !in 1..chapter.range.ids.size||end !in start..chapter.range.ids.size) null else VerseRange(q.id(chapter.number,start),q.id(chapter.number,end)) } }
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Récitation et répétitions")},text={ Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text("Récitateur",style=MaterialTheme.typography.titleMedium)
        reciters.forEach { r->FilterChip(selected=reciterId==r.id,onClick={onReciter(r.id);vm.action { vm.repo.mutate { touch(it.with("audioPreferences" to it.obj("audioPreferences").with("reciterId" to JsonPrimitive(r.id)))) } };if(RecitationService.current.value!=null) context.startService(Intent(context,RecitationService::class.java).setAction("STOP"))},label={Text(r.name)}) }
        Text("Passage",style=MaterialTheme.typography.titleMedium)
        listOf("session" to "Passage en cours","page" to "Page","surah" to "Sourate","verse" to "Un verset","custom" to "Versets personnalisés").forEach { (key,label)->FilterChip(selected=selection==key,onClick={selection=key},label={Text(label)}) }
        if(selection !in listOf("session","page")) {
            OutlinedTextField(surah,{surah=it},label={Text("Numéro de sourate (1–114)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            q.surahs.getOrNull((surah.toIntOrNull()?:0)-1)?.let { Text(it.name) }
            if(selection!="surah") { OutlinedTextField(first,{first=it},label={Text("Premier verset")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));if(selection=="custom") OutlinedTextField(last,{last=it},label={Text("Dernier verset")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }
        }
        selectedRange()?.let { Text(q.reference(it)) }
        Text("Répétitions",style=MaterialTheme.typography.titleMedium)
        repeatCounts.forEach { n->FilterChip(selected=choice==n.toString(),onClick={choice=n.toString()},label={Text("$n fois")}) }
        FilterChip(selected=choice=="custom",onClick={choice="custom"},label={Text("Nombre personnalisé")});if(choice=="custom") OutlinedTextField(custom,{custom=it},label={Text("Nombre personnalisé (1 à 999)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        FilterChip(selected=choice=="continuous",onClick={choice="continuous"},label={Text("En continu")})
        Row { Checkbox(each,{each=it});Text("Chaque verset",Modifier.padding(top=12.dp)) }
        Text("Vitesse",style=MaterialTheme.typography.titleMedium);Row { audioSpeeds.forEach { n->FilterChip(selected=speed==n,onClick={speed=n},label={Text("$n×")}) } }
        Text("Pause entre les répétitions",style=MaterialTheme.typography.titleMedium);Row { repeatGaps.forEach { n->FilterChip(selected=gap==n,onClick={gap=n},label={Text("${n}s")}) } }
        Row { Checkbox(autoStop&&choice!="continuous",{autoStop=it},enabled=choice!="continuous");Text("Arrêter à la fin des écoutes",Modifier.padding(top=12.dp)) }
    } },confirmButton={TextButton(enabled=loaded&&!launching&&selectedRange()!=null,onClick={
        val range=selectedRange()?:return@TextButton;val prefs=preferences();launching=true
        scope.launch { try { store.save(prefs);vm.repo.mutate { touch(it.with("audioPreferences" to it.obj("audioPreferences").with("reciterId" to JsonPrimitive(reciterId)))) };context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_RANGE").putExtra("start",range.start).putExtra("end",range.end).putExtra("reciter",reciterId).putExtra("count",prefs.count?:0).putExtra("each",each).putExtra("gap",gap).putExtra("speed",speed).putExtra("autoStop",autoStop));onDismiss() } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e;vm.repo.feedback("Impossible de lancer la récitation") } finally { launching=false } }
    }) { Text("Lire") }},dismissButton={TextButton(onClick=onDismiss) { Text("Fermer") }})
}
