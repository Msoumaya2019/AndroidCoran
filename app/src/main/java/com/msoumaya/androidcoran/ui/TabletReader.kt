package com.msoumaya.androidcoran.ui
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.UUID

private data class InkOperation(val before: ReaderInk?,val after: ReaderInk)
@Composable fun TabletReader(vm: CoranViewModel,s: JsonObject,page: Int,source: String,primary: Triple<JsonObject?,Bitmap?,JsonArray?>,selected: Int?,playing: Int?,onVerse: (Int)->Unit,onPage: (Int)->Unit,onAudio: (Int,Boolean)->Unit,onBusy: (Boolean)->Unit,modifier: Modifier) {
 val context=LocalContext.current;val configuration=LocalConfiguration.current;val owner by vm.repo.user.collectAsStateWithLifecycle();val expectedOwner=owner
 val state=if(s.str("userId")==owner.orEmpty()) s else json()
 val prefs=TabletPreferences.from(s.obj("reader").obj("tablet"))
 var tool by rememberSaveable(owner) { mutableStateOf("pen") };var annotating by rememberSaveable(owner) { mutableStateOf(false) }
 var readerOptions by remember { mutableStateOf(false) }
 var notesOpen by rememberSaveable(owner) { mutableStateOf(false) };var notebook by rememberSaveable(owner) { mutableStateOf(false) };var options by remember { mutableStateOf(false) };var export by remember { mutableStateOf(false) }
 var immersive by rememberSaveable { mutableStateOf(false) };var editing by rememberSaveable(owner) { mutableStateOf<String?>(null) };var selection by remember { mutableStateOf<JsonObject?>(null) };var targetPage by rememberSaveable { mutableIntStateOf(page) }
 var undo by remember(owner) { mutableStateOf(listOf<InkOperation>()) };var redo by remember(owner) { mutableStateOf(listOf<InkOperation>()) }
 fun save(ink: ReaderInk,record: Boolean=true,restore: Boolean=false) {
  if(vm.repo.user.value!=expectedOwner) return
  val previous=allReaderInk(vm.repo.state.value).firstOrNull { it.id==ink.id }
  if(record) { undo=(undo+InkOperation(previous,ink)).takeLast(40);redo=emptyList() }
  vm.action { vm.repo.mutate { if(vm.repo.user.value==expectedOwner) saveReaderInk(it,ink,restore) else it } }
 }
 fun remove(ink: ReaderInk,record: Boolean=true) {
  if(record) { undo=(undo+InkOperation(ink,ink.copy(deletedAt=annotationTime(ink.updatedAt),updatedAt=annotationTime(ink.updatedAt)))).takeLast(40);redo=emptyList() }
  vm.action { vm.repo.mutate { if(vm.repo.user.value==expectedOwner) deleteReaderInk(it,ink.id,annotationTime(ink.updatedAt)) else it } }
 }
 fun change(next: TabletPreferences) { vm.action { vm.repo.mutate { if(vm.repo.user.value==expectedOwner) touch(it.with("reader" to it.obj("reader").with("tablet" to next.toJson()))) else it } } }
 fun startDrawing(next: String) {
  tool=next;annotating=next!="select"
  if(source=="tajweed"&&next!="select") vm.action {
   val verse=selected?:vm.repo.quran.sourceRange(page,source).start
   vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive("coranTest")))) }
   withContext(Dispatchers.Main) { onPage(vm.repo.quran.sourcePage(verse,"coranTest")) }
  }
 }
 fun undoInk() { val op=undo.lastOrNull()?:return;undo=undo.dropLast(1);redo=redo+op;if(op.before==null) remove(op.after,false) else save(op.before.copy(updatedAt=annotationTime(vm.repo.state.value.obj("readerAnnotations").obj(op.after.id).str("updatedAt"))),false,true) }
 fun redoInk() { val op=redo.lastOrNull()?:return;redo=redo.dropLast(1);undo=undo+op;if(op.after.deletedAt!=null) remove(op.after,false) else save(op.after.copy(updatedAt=annotationTime(vm.repo.state.value.obj("readerAnnotations").obj(op.after.id).str("updatedAt"))),false,true) }
 fun textExport(notes: List<ReaderInk>) { vm.action { val file=annotationExportFile(context,"txt");file.writeText(noteTextExport(notes) { vm.repo.quran.reference(VerseRange(it,it)) });withContext(Dispatchers.Main) { if(vm.repo.user.value==expectedOwner) shareAnnotationFile(context,file,"text/plain") else file.delete() } } }
 fun quick(anchor: JsonObject,kind: String,verse: Boolean=false) {
  val a=if(verse) JsonObject(anchor.filterKeys { it !in listOf("wordId","wordIndex","wordText") }).with("regions" to anchor.arr("verseRegions")) else anchor
  val id=a.num("verseId");val ink=ReaderInk(UUID.randomUUID().toString(),targetPage,emptyList(),emptyList(),listOf(id).filter { it in 1..6236 },annotationTime(),source=source,tool=kind,color=prefs.color,width=if(kind=="highlight") prefs.highlightWidth else prefs.penWidth,anchor=a,createdAt=annotationTime())
  save(ink);selection=null;editing=ink.id
 }
 LaunchedEffect(annotating,export,editing) { onBusy(annotating||export||editing!=null) }
 val edit=allReaderInk(state).firstOrNull { it.id==editing }
 BoxWithConstraints(modifier) {
  val density=context.resources.displayMetrics.density
  val physicalShort=if(android.os.Build.VERSION.SDK_INT>=30) context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.let { minOf(it.width(),it.height())/density } else configuration.smallestScreenWidthDp.toFloat()
  val device=if(physicalShort>=600) "tablet" else "phone"
  val layout=tabletLayout(if(immersive) prefs.copy(toolsVisible=false) else prefs,device,maxWidth.value,maxHeight.value,notesOpen&&!immersive)
  val double=layout.doublePage&&source!="tajweed";val pages=spreadPages(page,double)
  val showTools=layout.enabled&&!immersive&&prefs.toolsVisible
  val pageNotes=allReaderInk(state,source).filter { it.page in pages }
  @Composable fun editorOrNotes() {
   if(edit!=null) ReaderInkEditor(edit,{save(it)},onDelete={remove(edit);editing=null},onClose={editing=null},stylusOnly=prefs.stylusOnly,inline=true)
   else Column(Modifier.fillMaxSize().padding(10.dp)) {
    Row { Text("Mes annotations",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));TextButton(onClick={notesOpen=false}) { Text("Fermer") } }
    LazyColumn(Modifier.weight(1f)) { if(pageNotes.isEmpty()) item { Text("Aucune annotation sur ces pages") };items(pageNotes,key={it.id}) { ink -> Box(Modifier.testTag("reader-annotation-"+ink.id)) { Panel("Page "+ink.page,ink.text.ifBlank { if(ink.note.isNotEmpty()) "Note manuscrite" else annotationToolLabel(ink.tool) },{editing=ink.id}) { TextButton(onClick={save(ink.copy(favorite=!ink.favorite,updatedAt=annotationTime(ink.updatedAt)))}) { Text(if(ink.favorite) "Retirer des favoris" else "Favori") };TextButton(onClick={save(ink.copy(masteredAt=if(ink.masteredAt==null) annotationTime() else null,updatedAt=annotationTime(ink.updatedAt)))}) { Text(if(ink.masteredAt==null) "Passage maîtrisé" else "Retirer la maîtrise") };ink.verses.firstOrNull()?.let { id -> TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,id) } }}) { Text("Difficulté") } };TextButton(onClick={remove(ink)}) { Text("Supprimer") } } } } }
   }
  }
  Column(Modifier.fillMaxSize()) {
   if(layout.enabled&&!immersive) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("tablet-reader-actions"),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
     FilterChip(annotating,{if(annotating) annotating=false else startDrawing(tool)},label={Text("Annoter")})
     TextButton(onClick={notesOpen=!notesOpen}) { Text("Mes annotations") }
     TextButton(onClick={notebook=true}) { Text("Mon cahier de révision") }
     TextButton(onClick={change(prefs.copy(spread=if(prefs.spread=="double") "single" else "double"))}) { Text(if(double) "Une seule page" else "Double page") }
     TextButton(onClick={change(prefs.copy(toolsVisible=!prefs.toolsVisible))}) { Text("Outils") }
     TextButton(enabled=source!="tajweed",onClick={export=true}) { Text("Exporter") }
     TextButton(onClick={immersive=true;notesOpen=false}) { Text("Lecture plein écran") }
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     TextButton(onClick={onPage(nextSpread(page,-1,double))}) { Text("Précédente") }
     Text((if(double) "Pages "+pages.sorted().joinToString("–") else "Page "+page)+(if(annotating) " · Annotation" else " · Lecture"),Modifier.weight(1f))
     TextButton(onClick={onPage(nextSpread(page,1,double))}) { Text("Suivante") }
    }
   }
   Row(Modifier.weight(1f)) {
    if(showTools&&layout.sidebars) TabletToolbar(true,tool,{startDrawing(it)},{options=true},::undoInk,::redoInk,undo.isNotEmpty(),redo.isNotEmpty())
    Box(Modifier.weight(1f).fillMaxHeight()) {
     Column(Modifier.fillMaxSize()) {
      AnimatedContent(targetState=pages,modifier=Modifier.weight(1f),transitionSpec={ (fadeIn(tween(420))+slideInHorizontally(tween(420)) { if(targetState.first()>initialState.first()) -it/8 else it/8 }).togetherWith(fadeOut(tween(420))+slideOutHorizontally(tween(420)) { if(targetState.first()>initialState.first()) it/8 else -it/8 }) },label="Tourner la page") { visiblePages ->
       CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
       Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(if(double) 12.dp else 0.dp)) {
        visiblePages.forEach { printedPage ->
         key(source,printedPage) {
          if(source=="tajweed") TajwidReader(vm.repo.quran,vm.repo.quran.sourceRange(printedPage,source),selected,onVerse,Modifier.weight(1f).fillMaxHeight(),allReaderInk(state,source)) { a -> targetPage=printedPage;selection=a }
          else PrintedMushafPage(vm,s,source,printedPage,if(printedPage==page) primary else null,selected,playing,annotating&&tool!="select"&&!immersive,prefs,tool,Modifier.weight(1f).fillMaxHeight(),onVerse,{onPage(nextSpread(page,if(it>printedPage) 1 else -1,double))},{a -> targetPage=printedPage;selection=a},{ ink -> editing=ink.id;notesOpen=true },{stroke,verses ->
           val ink=newReaderInk(printedPage,stroke,verses).copy(source=source,tool=tool,color=prefs.color,width=if(tool=="highlight") prefs.highlightWidth else prefs.penWidth,penStyle=prefs.penStyle,createdAt=annotationTime())
           save(ink);if(tool=="note") { editing=ink.id;notesOpen=true }
          },{remove(it)},{active -> onBusy(annotating||active||export||editing!=null)})
         }
        }
       }
      }
      }
      if(showTools&&!layout.sidebars) TabletToolbar(false,tool,{startDrawing(it)},{options=true},::undoInk,::redoInk,undo.isNotEmpty(),redo.isNotEmpty())
     }
     if(notesOpen&&!immersive&&!layout.sidebars) Surface(Modifier.align(Alignment.CenterEnd).widthIn(max=340.dp).fillMaxWidth(.9f).fillMaxHeight(.9f).testTag("tablet-notes-overlay"),tonalElevation=6.dp,shape=MaterialTheme.shapes.large) { editorOrNotes() }
    }
    if(notesOpen&&!immersive&&layout.sidebars) Surface(Modifier.width(280.dp).fillMaxHeight().testTag("tablet-notes-sidebar"),tonalElevation=3.dp) { editorOrNotes() }
   }
  }
  if(!layout.enabled&&!immersive) Box(Modifier.align(Alignment.TopStart).displayCutoutPadding()) {
   TextButton(onClick={readerOptions=true}) { Text("⋯") }
   DropdownMenu(readerOptions,{readerOptions=false}) {
    DropdownMenuItem(text={Text("Mon cahier de révision")},onClick={readerOptions=false;notebook=true})
    DropdownMenuItem(text={Text("Mes annotations")},onClick={readerOptions=false;notesOpen=true})
    if(source!="tajweed") DropdownMenuItem(text={Text("Exporter")},onClick={readerOptions=false;export=true})
   }
  }
  if(immersive) TextButton(onClick={immersive=false},modifier=Modifier.align(Alignment.TopStart).displayCutoutPadding()) { Text("Quitter la lecture plein écran") }
  if(edit!=null&&!notesOpen) ReaderInkEditor(edit,{save(it)},onDelete={remove(edit);editing=null},onClose={editing=null},stylusOnly=prefs.stylusOnly)
  if(options) TabletInkPreferences(prefs,::change) { options=false }
  if(notebook) AnnotationNotebook(vm,state,onOpen={ink ->
   notebook=false
   vm.action {
    vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive(ink.source)))) }
    withContext(Dispatchers.Main) { onPage(ink.page);editing=ink.id;notesOpen=true }
   }
  },onSave={save(it)},onClose={notebook=false},onExport=::textExport)
  if(export) AnnotationExportDialog(vm,state,source,page,onBusy) { export=false }
  selection?.let { a -> AlertDialog(onDismissRequest={selection=null},title={Text(if(a["wordId"]!=null||a["wordIndex"]!=null) "Actions du mot" else "Actions du verset")},text={Column {
   allReaderInk(state,source).firstOrNull { ink -> ink.anchor?.num("verseId")==a.num("verseId")&&ink.anchor?.get("wordId")==a["wordId"]&&ink.anchor?.get("wordIndex")==a["wordIndex"] }?.let { ink -> TextButton(onClick={selection=null;editing=ink.id}) { Text("Consulter la note") } }
   if(a.str("wordText").isNotBlank()) Text(a.str("wordText"),fontFamily=arabicInterfaceFont)
   if(a["wordId"]!=null||a["wordIndex"]!=null) TextButton(onClick={quick(a,"circle")}) { Text("Annoter ce mot") }
   TextButton(onClick={quick(a,"circle",true)}) { Text("Annoter le verset") }
   Row { listOf("highlight" to "Surligner","circle" to "Entourer","underline" to "Souligner").forEach { (kind,label) -> TextButton(onClick={quick(a,kind)}) { Text(label) } } }
   TextButton(onClick={quick(a,"note")}) { Text("Commenter") }
   if(source!="coranTest"&&source!="tajweed") TextButton(onClick={
    selection=null
    vm.action {
     vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive("coranTest")))) }
     withContext(Dispatchers.Main) { onPage(vm.repo.quran.sourcePage(a.num("verseId"),"coranTest")) }
    }
   }) { Text("Choisir un mot dans le Coran avec Tajwid") }
   Row { TextButton(onClick={selection=null;onAudio(a.num("verseId"),false)}) { Text("Écouter") };TextButton(onClick={selection=null;onAudio(a.num("verseId"),true)}) { Text("Répéter") } }
   TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,a.num("verseId")) } };selection=null}) { Text("Marquer comme difficile") }
  }},confirmButton={TextButton(onClick={selection=null}) { Text("Fermer") }}) }
 }
}
@Composable private fun PrintedMushafPage(vm: CoranViewModel,s: JsonObject,source: String,page: Int,primary: Triple<JsonObject?,Bitmap?,JsonArray?>?,selected: Int?,playing: Int?,annotating: Boolean,prefs: TabletPreferences,tool: String,modifier: Modifier,onVerse: (Int)->Unit,onPage: (Int)->Unit,onSelection: (JsonObject)->Unit,onOpen: (ReaderInk)->Unit,onStroke: (InkStroke,List<Int>)->Unit,onErase: (ReaderInk)->Unit,onActive: (Boolean)->Unit) {
 val context=LocalContext.current;val owner by vm.repo.user.collectAsStateWithLifecycle()
 val payload by produceState(primary?:Triple<JsonObject?,Bitmap?,JsonArray?>(null,null,null),source,page,primary) { value=primary?:withContext(Dispatchers.IO) { loadPrintedPage(context,vm.repo.quran,source,page) } }
 if(source=="coran_1441"&&payload.second==null) Box(modifier) { Text("Télécharge cette édition depuis Réglages → Téléchargements") }
 else AndroidView(factory={MushafView(it)},modifier=modifier.testTag("printed-page-"+page),update={v ->
  if(v.page!=page||v.source!=source||v.tag!=payload) { v.load(vm.repo.quran,page,source,payload.first,payload.second,payload.third);v.tag=payload }
  v.paper=paperColor(s.obj("reader").str("paper"));v.selected=selected;v.playing=playing;v.annotationOwner=owner?:"guest";v.annotationMode=annotating;v.stylusOnly=prefs.stylusOnly;v.inkTool=tool;v.inkColor=prefs.color;v.inkWidth=if(tool=="highlight") prefs.highlightWidth else prefs.penWidth;v.inkPenStyle=prefs.penStyle
  v.annotations=if(s.str("userId")==owner.orEmpty()) allReaderInk(s,source) else emptyList();v.onVerse=onVerse;v.onPage=onPage;v.onSelection=onSelection;v.onInkClick=onOpen;v.onInkStroke=onStroke;v.onInkErase=onErase;v.onInkActive=onActive
  v.bookmarks=s.obj("bookmarks").values.map { it.jsonObject }.filter { it.str("deletedAt").isBlank() }.map { it.num("verseId") }.toSet();v.difficulties=s.obj("difficultyMarkers").keys.mapNotNull { it.toIntOrNull() }.toSet();v.invalidate()
 })
}
