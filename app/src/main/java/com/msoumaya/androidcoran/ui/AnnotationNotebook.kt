package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
@Composable fun AnnotationNotebook(vm: CoranViewModel,s: JsonObject,onOpen: (ReaderInk)->Unit,onSave: (ReaderInk)->Unit,onClose: ()->Unit,onExport: (List<ReaderInk>)->Unit) {
 var surah by rememberSaveable { mutableStateOf("") };var page by rememberSaveable { mutableStateOf("") };var color by rememberSaveable { mutableStateOf<String?>(null) };var tool by rememberSaveable { mutableStateOf<String?>(null) };var favorite by rememberSaveable { mutableStateOf(false) };var difficult by rememberSaveable { mutableStateOf(false) };var sort by rememberSaveable { mutableStateOf("date") };var chosen by remember { mutableStateOf(setOf<String>()) }
 var category by rememberSaveable { mutableStateOf<String?>(null) }
 val categories=(noteCategories+s.obj("reader").arr("noteCategories").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }+allReaderInk(s).flatMap { it.categories }).distinct()
 val difficultIds=s.obj("difficultyMarkers").filterValues { (it as? JsonObject)?.let { m -> listOf("user","admin").any { m[it]!=null&&m[it]!=JsonNull } }==true }.keys.mapNotNull { it.toIntOrNull() }.toSet()
 val rows=notebookEntries(allReaderInk(s),NotebookFilter(surah.toIntOrNull(),page.toIntOrNull(),color,tool,favorite,difficult,sort,category),difficultIds) { id -> if(id in 1..6236) vm.repo.quran.verse(id).surah else 0 }
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.widthIn(max=1100.dp).fillMaxWidth(.97f).fillMaxHeight(.94f),shape=MaterialTheme.shapes.large) {
   Column(Modifier.padding(16.dp)) {
    Row { Text("Mon cahier de révision",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));TextButton(onClick=onClose) { Text("Fermer") } }
    Row { OutlinedTextField(surah,{surah=it},label={Text("Sourate (1–114)")},modifier=Modifier.weight(1f));OutlinedTextField(page,{page=it},label={Text("Page (1–604)")},modifier=Modifier.weight(1f)) }
    Row(Modifier.horizontalScroll(rememberScrollState())) { FilterChip(favorite,{favorite=!favorite},label={Text("Favoris")});FilterChip(difficult,{difficult=!difficult},label={Text("Difficiles")});tabletColors.forEach { (name,c) -> FilterChip(color==c,{color=if(color==c) null else c},label={Text(name)}) } }
    Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("pen","highlight","circle","underline","arrow","note").forEach { t -> FilterChip(tool==t,{tool=if(tool==t) null else t},label={Text(annotationToolLabel(t))}) };listOf("date","surah","page","color","type","difficulty").forEach { t -> FilterChip(sort==t,{sort=t},label={Text("Tri : "+mapOf("date" to "Date","surah" to "Sourate","page" to "Page","color" to "Couleur","type" to "Type","difficulty" to "Difficulté")[t])}) } }
    Row(Modifier.horizontalScroll(rememberScrollState())) { FilterChip(category==null,{category=null},label={Text("Toutes les catégories")});categories.forEach { c -> FilterChip(category==c,{category=if(category==c) null else c},label={Text(c)}) } }
    TextButton(onClick={onExport(if(chosen.isEmpty()) rows else rows.filter { it.id in chosen })},enabled=rows.isNotEmpty()) { Text("Exporter les notes textuelles") }
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     if(rows.isEmpty()) item { Text("Aucune annotation pour ces filtres") }
     items(rows,key={it.id}) { n -> Panel(n.title.ifBlank { n.source+" · page "+n.page },n.text.ifBlank { if(n.note.isNotEmpty()) "Note manuscrite" else annotationToolLabel(n.tool) },{onOpen(n)}) {
      if(n.categories.isNotEmpty()) Text(n.categories.joinToString(" · "),style=MaterialTheme.typography.labelMedium)
      Row { Checkbox(n.id in chosen,{chosen=if(it) chosen+n.id else chosen-n.id});TextButton(onClick={onSave(n.copy(favorite=!n.favorite,updatedAt=annotationTime(n.updatedAt)))}) { Text(if(n.favorite) "Retirer des favoris" else "Favori") } }
      TextButton(onClick={onSave(n.copy(masteredAt=if(n.masteredAt==null) annotationTime() else null,updatedAt=annotationTime(n.updatedAt)))}) { Text(if(n.masteredAt==null) "Passage maîtrisé" else "Retirer la maîtrise") }
      n.verses.firstOrNull()?.let { id -> Text(vm.repo.quran.reference(VerseRange(id,id)));TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,id) } }}) { Text(if(id in difficultIds) "Retirer la difficulté" else "Marquer difficile") } }
     } }
    }
   }
  }
 }
}

internal fun annotationToolLabel(tool: String)=mapOf("pen" to "Stylo","highlight" to "Surligneur","circle" to "Cercle","underline" to "Souligné","arrow" to "Flèche","note" to "Note")[tool]?:"Annotation"
