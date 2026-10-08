package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun TajwidReader(q: Quran,r: VerseRange,selected: Int?,onVerse: (Int)->Unit,modifier: Modifier) {
    val context=LocalContext.current
    val texts=remember { context.assets.open("tajweed-text.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonArray } }
    val rules=remember { context.assets.open("tajweed-rules.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonArray } }
    LazyColumn(modifier.padding(16.dp)) { items(r.ids) { id ->
        val text=texts[id-1].jsonObject.str("text");val chars=text.codePoints().toArray();val colors=Array<String?>(chars.size) { null }
        rules[id-1].jsonObject.arr("annotations").forEach { a -> val row=a.jsonObject;for(i in row.num("start") until minOf(row.num("end"),chars.size)) colors[i]=row.str("rule") }
        val annotated=buildAnnotatedString { chars.forEachIndexed { i,cp -> val color=when { colors[i]?.startsWith("madd")==true -> Color(0xFFB45375);colors[i]?.startsWith("ikhfa")==true||colors[i]=="iqlab" -> Color(0xFF3A779B);colors[i]?.startsWith("idghaam")==true||colors[i]=="ghunnah" -> Color(0xFF6F5FA5);colors[i]=="qalqalah" -> Color(0xFFB05E32);colors[i]=="silent" -> Color(0xFFA2A2A2);colors[i]!=null -> Color(0xFFA26C44);else -> Color(0xFF241C2B) };pushStyle(SpanStyle(color=color));append(String(Character.toChars(cp)));pop() } }
        Card(modifier=Modifier.fillMaxWidth().padding(vertical=5.dp).clickable { onVerse(id) },colors=CardDefaults.cardColors(containerColor=if(selected==id) MaterialTheme.colorScheme.primaryContainer else Color.White)) { Column(Modifier.padding(14.dp)) { Text("${q.surah(id).name} · ${q.verse(id).ayah}",fontSize=12.sp);Text(annotated,style=TextStyle(fontSize=28.sp,lineHeight=48.sp,textDirection=TextDirection.Rtl)) } }
    };item { Text("Tajweed : cpfair, CC BY 4.0 · texte Hafs Tanzil 2017",fontSize=11.sp) } }
}
