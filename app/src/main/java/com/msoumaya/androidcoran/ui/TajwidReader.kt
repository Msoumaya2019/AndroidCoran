package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun TajwidReader(q: Quran,r: VerseRange,selected: Int?,onVerse: (Int)->Unit,modifier: Modifier,annotations: List<ReaderInk> = emptyList(),playing: Int?=null,onNote: (ReaderInk)->Unit={},onSelection: (JsonObject)->Unit={}) {
    val context=LocalContext.current
    val hostView=androidx.compose.ui.platform.LocalView.current
    val texts=remember { context.assets.open("tajweed-text.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonArray } }
    val rules=remember { context.assets.open("tajweed-rules.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonArray } }
    LazyColumn(modifier.padding(16.dp)) { items(r.ids) { id ->
        val text=texts[id-1].jsonObject.str("text");val chars=text.codePoints().toArray();val colors=Array<String?>(chars.size) { null }
        rules[id-1].jsonObject.arr("annotations").forEach { a -> val row=a.jsonObject;for(i in row.num("start") until minOf(row.num("end"),chars.size)) colors[i]=row.str("rule") }
        val annotated=buildAnnotatedString { chars.forEachIndexed { i,cp -> val color=when { colors[i]?.startsWith("madd")==true -> Color(0xFFB45375);colors[i]?.startsWith("ikhfa")==true||colors[i]=="iqlab" -> Color(0xFF3A779B);colors[i]?.startsWith("idghaam")==true||colors[i]=="ghunnah" -> Color(0xFF6F5FA5);colors[i]=="qalqalah" -> Color(0xFFB05E32);colors[i]=="silent" -> Color(0xFFA2A2A2);colors[i]!=null -> Color(0xFFA26C44);else -> Color(0xFF241C2B) };pushStyle(SpanStyle(color=color));append(String(Character.toChars(cp)));pop() } }
        Card(modifier=Modifier.fillMaxWidth().padding(vertical=5.dp),colors=CardDefaults.cardColors(containerColor=if(playing==id) Color(0x52F6C445) else if(selected==id) MaterialTheme.colorScheme.primaryContainer else Color.White)) { Column(Modifier.padding(14.dp)) { Row { Text("${q.surah(id).name} · ${q.verse(id).ayah}",fontSize=12.sp,modifier=Modifier.weight(1f));annotations.filter { it.verses.firstOrNull()==id&&(it.tool=="note"||it.note.isNotEmpty()||it.text.isNotBlank()) }.forEach { ink -> IconButton(onClick={onNote(ink)}) { Icon(androidx.compose.material.icons.Icons.Default.StickyNote2,"Ouvrir la note "+ink.title,tint=Color(android.graphics.Color.parseColor(ink.iconColor))) } } };run {
            var layout by remember(id) { mutableStateOf<TextLayoutResult?>(null) }
            val words=Regex("\\S+").findAll(annotated.text).toList()
            val marks=annotations.filter { it.anchor?.num("verseId")==id||id in it.verses }
            fun boxes(start: Int,end: Int): List<android.graphics.RectF> { val l=layout?:return emptyList();return (start until end).map { l.getBoundingBox(it) }.groupBy { it.top }.values.map { line -> android.graphics.RectF(line.minOf { it.left },line.first().top,line.maxOf { it.right },line.first().bottom) } }
            fun select(position: androidx.compose.ui.geometry.Offset) {
                val l=layout?:return;val offset=l.getOffsetForPosition(position);val index=words.indexOfFirst { offset in it.range&&boxes(it.range.first,it.range.last+1).any { r -> r.contains(position.x,position.y) } }
                val base=json("verseId" to id,"verseRegions" to emptyList<Any>(),"regions" to emptyList<Any>())
                onSelection(if(index<0) base else base.with("wordIndex" to JsonPrimitive(index),"wordText" to JsonPrimitive(words[index].value)))
            }
            Text(annotated,onTextLayout={layout=it},modifier=Modifier.fillMaxWidth().pointerInput(id,annotations) { detectTapGestures(onLongPress={position -> hostView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);onVerse(id);select(position)}) }.drawWithContent {
                drawContent();val l=layout
                if(l!=null) drawIntoCanvas { canvas -> marks.forEach { ink ->
                    val index=ink.anchor?.get("wordIndex")?.jsonPrimitive?.intOrNull;val word=index?.let { words.getOrNull(it) }
                    val r=if(word!=null) boxes(word.range.first,word.range.last+1) else if(index==null) boxes(0,annotated.length) else emptyList()
                    drawReaderMark(canvas.nativeCanvas,ink,r,size.width,size.height)
                } }
            },style=TextStyle(fontSize=28.sp,lineHeight=48.sp,textDirection=TextDirection.Rtl))
        } } }
    };item { Text("Tajweed : cpfair, CC BY 4.0 · texte Hafs Tanzil 2017",fontSize=11.sp) } }
}
