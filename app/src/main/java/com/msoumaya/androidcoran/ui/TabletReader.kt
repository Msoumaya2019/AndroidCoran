package com.msoumaya.androidcoran.ui
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
@Composable fun TabletReader(vm: CoranViewModel,s: JsonObject,page: Int,source: String,primary: Triple<JsonObject?,Bitmap?,JsonArray?>,selected: Int?,playing: Int?,onVerse: (Int)->Unit,onPage: (Int)->Unit,onAudio: (Int,Boolean)->Unit,onBusy: (Boolean)->Unit,modifier: Modifier,onRecord: (Int)->Unit={},onClearSelection: ()->Unit={}) {
 val context=LocalContext.current;val configuration=LocalConfiguration.current;val owner by vm.repo.user.collectAsStateWithLifecycle();val expectedOwner=owner
 val state=if(s.str("userId")==owner.orEmpty()) s else json()
 val prefs=TabletPreferences.from(s.obj("reader").obj("tablet"))
 var tool by rememberSaveable(owner) { mutableStateOf("pen") };var annotating by rememberSaveable(owner) { mutableStateOf(false) }
 var readerOptions by remember { mutableStateOf(false) }
 var notesOpen by rememberSaveable(owner) { mutableStateOf(false) };var notebook by rememberSaveable(owner) { mutableStateOf(false) };var options by remember { mutableStateOf(false) };var export by remember { mutableStateOf(false) }
 var immersive by rememberSaveable { mutableStateOf(false) };var editing by rememberSaveable(owner) { mutableStateOf<String?>(null) };var selection by remember { mutableStateOf<JsonObject?>(null) };var targetPage by rememberSaveable { mutableIntStateOf(page) }
 var pendingJson by rememberSaveable(owner) { mutableStateOf<String?>(null) }
 val pending=pendingJson?.let { inkFromJson(Json.parseToJsonElement(it).jsonObject) }
 var undo by remember(owner) { mutableStateOf(listOf<InkOperation>()) };var redo by remember(owner) { mutableStateOf(listOf<InkOperation>()) }
 fun save(ink: ReaderInk,record: Boolean=true,restore: Boolean=false) {
  if(vm.repo.user.value!=expectedOwner) return
  val previous=allReaderInk(vm.repo.state.value).firstOrNull { it.id==ink.id }
  if(record) { undo=(undo+InkOperation(previous,ink)).takeLast(40);redo=emptyList() }
  vm.action { vm.repo.mutate { if(vm.repo.user.value==expectedOwner) saveReaderInk(it,ink,restore).let { next -> next.with("reader" to next.obj("reader").with("noteCategories" to element((next.obj("reader").arr("noteCategories").mapNotNull { c -> (c as? JsonPrimitive)?.contentOrNull }+ink.categories).distinct()))) } else it } }
 }
 suspend fun persistNote(ink: ReaderInk) {
  check(vm.repo.user.value==expectedOwner) {"Le compte a changé. Rouvrez la note."}
  val previous=allReaderInk(vm.repo.state.value).firstOrNull {it.id==ink.id}
  vm.repo.mutate {check(vm.repo.user.value==expectedOwner) {"Le compte a changé."};saveReaderNote(it,ink)}
  check(vm.repo.user.value==expectedOwner&&allReaderInk(vm.repo.state.value).any {it.id==ink.id&&it.updatedAt==ink.updatedAt}) {"La note a été modifiée ailleurs. Rouvrez-la avant d’enregistrer."}
  undo=(undo+InkOperation(previous,ink)).takeLast(40);redo=emptyList()
  annotating=false;tool="select";notesOpen=false;options=false;readerOptions=false;selection=null;onClearSelection()
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
 LaunchedEffect(annotating,export,editing) { onBusy(annotating||export||editing!=null) }
 var rootPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
 var moreActions by remember { mutableStateOf(false) }
 fun closeSelection() { selection=null;moreActions=false;onClearSelection() }
 LaunchedEffect(page,source,owner) { closeSelection() }
 LaunchedEffect(selection) { if(selection==null) onClearSelection() }
 BackHandler(selection!=null) {closeSelection()}
 val edit=pending?:allReaderInk(state).firstOrNull { it.id==editing }
 BoxWithConstraints(modifier.onGloballyPositioned { rootPosition=it.positionInRoot() }) {
  val density=context.resources.displayMetrics.density
  val physicalShort=if(android.os.Build.VERSION.SDK_INT>=30) context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.let { minOf(it.width(),it.height())/density } else configuration.smallestScreenWidthDp.toFloat()
  val device=if(physicalShort>=600) "tablet" else "phone"
  val layout=tabletLayout(if(immersive) prefs.copy(toolsVisible=false) else prefs,device,maxWidth.value,maxHeight.value,notesOpen&&!immersive)
  val double=layout.doublePage&&source!="tajweed";val pages=spreadPages(page,double)
  val controls=tabletReaderControls(layout.enabled,double,immersive||!layout.enabled,annotating,prefs.toolsVisible,layout.sidebars)
  val showTools=controls.showTools
  LaunchedEffect(double) { if(double) { annotating=false;notesOpen=false;editing=null;selection=null } }
  LaunchedEffect(layout.landscape) { if(!layout.landscape) notesOpen=false }
  fun toggleFinger() { change(prefs.copy(stylusOnly=!prefs.stylusOnly,toolsVisible=prefs.stylusOnly||prefs.toolsVisible));if(prefs.stylusOnly) annotating=true }
 fun quick(anchor: JsonObject,kind: String,verse: Boolean=false) {
  if(double&&kind!="note") return
  if(double) change(prefs.copy(spread="single"))
  val a=if(verse) JsonObject(anchor.filterKeys { it !in listOf("wordId","wordIndex","wordText") }).with("regions" to anchor.arr("verseRegions")) else anchor
  val id=a.num("verseId");val previous=if(kind=="note") null else allReaderInk(vm.repo.state.value,source).firstOrNull { it.anchor?.num("verseId")==id&&it.anchor?.get("wordId")==a["wordId"]&&it.anchor?.get("wordIndex")==a["wordIndex"] }
  val now=annotationTime(previous?.updatedAt)
  val base=previous?:ReaderInk(UUID.randomUUID().toString(),targetPage,emptyList(),emptyList(),listOf(id).filter { it in 1..6236 },now,source=source,color=prefs.color,createdAt=now)
  val ink=base.copy(tool=if(kind=="note") previous?.tool?:"highlight" else kind,width=if(kind=="highlight") prefs.highlightWidth else prefs.penWidth,anchor=a,updatedAt=now)
  selection=null;if(kind=="note") { pendingJson=ink.toJson().toString();editing=ink.id;notesOpen=false } else { save(ink);editing=null }
 }
  val pageNotes=allReaderInk(state,source).filter { it.page in pages }
  @Composable fun editorOrNotes() {
   Column(Modifier.fillMaxSize().padding(10.dp)) {
    Row { Text("Mes annotations",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));TextButton(onClick={notesOpen=false}) { Text("Fermer") } }
    LazyColumn(Modifier.weight(1f)) { if(pageNotes.isEmpty()) item { Text("Aucune annotation sur ces pages") };items(pageNotes,key={it.id}) { ink -> Box(Modifier.testTag("reader-annotation-"+ink.id)) { Panel(ink.title.ifBlank { "Page "+ink.page },ink.text.ifBlank { if(ink.note.isNotEmpty()) "Note manuscrite" else annotationToolLabel(ink.tool) },{if(edit==null) { editing=ink.id;notesOpen=false }}) { TextButton(onClick={save(ink.copy(favorite=!ink.favorite,updatedAt=annotationTime(ink.updatedAt)))}) { Text(if(ink.favorite) "Retirer des favoris" else "Favori") };TextButton(onClick={save(ink.copy(masteredAt=if(ink.masteredAt==null) annotationTime() else null,updatedAt=annotationTime(ink.updatedAt)))}) { Text(if(ink.masteredAt==null) "Passage maîtrisé" else "Retirer la maîtrise") };ink.verses.firstOrNull()?.let { id -> TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,id) } }}) { Text("Difficulté") } };TextButton(onClick={remove(ink)}) { Text("Supprimer") } } } } }
   }
  }
  Column(Modifier.fillMaxSize()) {
   if(layout.enabled&&!immersive) {
    Row(Modifier.fillMaxWidth().padding(end=56.dp).horizontalScroll(rememberScrollState()).testTag("tablet-reader-actions"),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
     if(!double) {
     FilterChip(annotating,{if(annotating) annotating=false else startDrawing(if(tool=="select") "pen" else tool)},label={Text("Annoter")})
     TextButton(onClick={notesOpen=!notesOpen}) { Text("Mes annotations") }
     TextButton(onClick={notebook=true}) { Text("Mon cahier de révision") }
     TextButton(onClick={toggleFinger()}) { Text(if(prefs.stylusOnly) "Doigt : désactivé" else "Doigt : activé") }
     }
     TextButton(onClick={notesOpen=false;annotating=false;editing=null;selection=null;change(prefs.copy(spread=if(prefs.spread=="double") "single" else "double"));if(prefs.spread!="double"&&layout.landscape) immersive=true}) { Text(if(prefs.spread=="double") "Une seule page" else "Double page") }
     if(!double) TextButton(onClick={change(prefs.copy(toolsVisible=!prefs.toolsVisible))}) { Text("Outils") }
     TextButton(enabled=source!="tajweed",onClick={export=true}) { Text("Exporter") }
     TextButton(onClick={immersive=true;notesOpen=false}) { Text("Lecture plein écran") }
    }
    Row(Modifier.fillMaxWidth().padding(end=56.dp),verticalAlignment=Alignment.CenterVertically) {
     TextButton(onClick={onPage(nextSpread(page,-1,double))}) { Text("Précédente") }
     Text((if(double) "Pages "+pages.sorted().joinToString("–") else "Page "+page)+(if(annotating) " · Annotation" else " · Lecture")+(if(prefs.spread=="double"&&!double) { if(layout.landscape) " · Double page : agrandis la fenêtre" else " · Double page : tourne en paysage" } else ""),Modifier.weight(1f))
     TextButton(onClick={onPage(nextSpread(page,1,double))}) { Text("Suivante") }
    }
   }
   Row(Modifier.weight(1f)) {
    if(controls.verticalTools) TabletToolbar(true,tool,{startDrawing(it)},{options=true},::undoInk,::redoInk,undo.isNotEmpty(),redo.isNotEmpty())
    Box(Modifier.weight(1f).fillMaxHeight()) {
     Column(Modifier.fillMaxSize()) {
      Box(Modifier.weight(1f)) { val visiblePages=pages
       CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
       BoxWithConstraints(Modifier.fillMaxSize()) {
       val ratio=if(source=="coranTest") 1000f/2120f else primary.second?.let { it.width.toFloat()/it.height }?:1000f/2120f
       val gutter=24.dp
       val paneWidth=minOf((maxWidth-gutter)/2,maxHeight*ratio)
       Row(Modifier.fillMaxSize(),horizontalArrangement=if(double) Arrangement.spacedBy(gutter,Alignment.CenterHorizontally) else Arrangement.Center) {
        visiblePages.forEachIndexed { slot,printedPage ->
         key(source,slot) {
          if(source=="tajweed") TajwidReader(vm.repo.quran,vm.repo.quran.sourceRange(printedPage,source),selected,onVerse,Modifier.weight(1f).fillMaxHeight(),allReaderInk(state,source),playing,onNote={if(edit==null) {editing=it.id;notesOpen=false}}) { a -> if(edit==null) {targetPage=printedPage;selection=a} }
          else PrintedMushafPage(vm,s,source,printedPage,if(printedPage==page) primary else null,selected,playing,edit==null&&controls.canAnnotate&&annotating&&tool!="select",prefs,tool,if(double) Modifier.width(paneWidth).fillMaxHeight() else Modifier.weight(1f).fillMaxHeight(),double,layout.enabled,onVerse,{onPage(nextSpread(page,if(it>printedPage) 1 else -1,double))},{a -> if(a.flag("dismiss")) closeSelection() else if(edit==null) { targetPage=printedPage;selection=a } },{ ink -> if(edit==null) { editing=ink.id;notesOpen=false } },{stroke,verses ->
           val ink=newReaderInk(printedPage,stroke,verses).copy(source=source,tool=tool,color=prefs.color,width=if(tool=="highlight") prefs.highlightWidth else prefs.penWidth,penStyle=prefs.penStyle,createdAt=annotationTime())
           if(tool=="note") { pendingJson=ink.toJson().toString();editing=ink.id;notesOpen=false } else save(ink)
          },{remove(it)},{active -> onBusy(annotating||active||export||editing!=null)})
         }
        }
       }
      }
      }
      }
     }
     if(controls.floatingTools) Box(Modifier.align(Alignment.BottomCenter)) {TabletToolbar(false,tool,{startDrawing(it)},{options=true},::undoInk,::redoInk,undo.isNotEmpty(),redo.isNotEmpty())}
     if(controls.canAnnotate&&notesOpen&&!immersive&&layout.notesOverlay) Surface(Modifier.align(Alignment.CenterEnd).widthIn(max=340.dp).fillMaxWidth(.9f).fillMaxHeight(.9f).testTag("tablet-notes-overlay"),tonalElevation=6.dp,shape=MaterialTheme.shapes.large) { editorOrNotes() }
    }
    if(controls.canAnnotate&&notesOpen&&!immersive&&!layout.notesOverlay) Surface(Modifier.width(280.dp).fillMaxHeight().testTag("tablet-notes-sidebar"),tonalElevation=3.dp) { editorOrNotes() }
   }
  }
  if(!layout.enabled&&!immersive) Box(Modifier.align(Alignment.TopStart).displayCutoutPadding()) {
   TextButton(onClick={readerOptions=true}) { Text("⋯") }
   DropdownMenu(readerOptions,{readerOptions=false}) {
    DropdownMenuItem(text={Text(if(annotating) "Terminer les annotations" else "Annoter")},onClick={readerOptions=false;if(annotating) annotating=false else startDrawing("pen")})
    DropdownMenuItem(text={Text("Autoriser le doigt")},onClick={readerOptions=false;toggleFinger()})
    DropdownMenuItem(text={Text("Mon cahier de révision")},onClick={readerOptions=false;notebook=true})
    DropdownMenuItem(text={Text("Mes annotations")},onClick={readerOptions=false;notesOpen=true})
    if(source!="tajweed") DropdownMenuItem(text={Text("Exporter")},onClick={readerOptions=false;export=true})
   }
  }
  if(immersive) Row(Modifier.align(Alignment.TopStart).displayCutoutPadding().padding(end=56.dp).horizontalScroll(rememberScrollState())) {
   if(controls.canAnnotate) { TextButton(onClick={if(annotating) annotating=false else {change(prefs.copy(toolsVisible=true));startDrawing(if(tool=="select") "pen" else tool)}}) { Text(if(annotating) "Terminer les annotations" else "Annoter") };TextButton(onClick={toggleFinger()}) { Text(if(prefs.stylusOnly) "Autoriser le doigt" else "Doigt activé") } }
   if(double) TextButton(onClick={change(prefs.copy(spread="single"))}) { Text("Une seule page") }
   TextButton(onClick={immersive=false}) { Text("Quitter la lecture plein écran") }
  }
  if(edit!=null) ReaderInkEditor(edit,{save(it);pendingJson=null},onDelete={if(pending==null) remove(edit);pendingJson=null;editing=null},onClose={pendingJson=null;editing=null},stylusOnly=prefs.stylusOnly,startEditing=pending!=null,onSave={persistNote(it)},availableCategories=(noteCategories+state.obj("reader").arr("noteCategories").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }+allReaderInk(state).flatMap { it.categories }).distinct())
  if(options) TabletInkPreferences(prefs,::change) { options=false }
  if(notebook) AnnotationNotebook(vm,state,onOpen={ink ->
   notebook=false
   vm.action {
    vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive(ink.source),"tablet" to prefs.copy(spread="single").toJson()))) }
    withContext(Dispatchers.Main) { onPage(ink.page);editing=ink.id;notesOpen=false }
   }
  },onSave={save(it)},onClose={notebook=false},onExport=::textExport)
  if(export) AnnotationExportDialog(vm,state,source,page,onBusy) { export=false }
  selection?.let { a ->
   val id=a.num("verseId");val verse=vm.repo.quran.verse(id)
   val x=(a["anchorRootX"] as? JsonPrimitive)?.floatOrNull
   val top=(a["anchorRootTop"] as? JsonPrimitive)?.floatOrNull
   val bottom=(a["anchorRootBottom"] as? JsonPrimitive)?.floatOrNull
   val anchor=if(x!=null&&top!=null&&bottom!=null) androidx.compose.ui.geometry.Rect(x-rootPosition.x,top-rootPosition.y,x-rootPosition.x,bottom-rootPosition.y) else null
   val difficult=state.obj("difficultyMarkers").obj(id.toString())["user"].let { it!=null&&it!=JsonNull }
   VerseFloatingBar(verse.surah.toString()+":"+verse.ayah,anchor,difficult,
    onListen={closeSelection();onAudio(id,false)},
    onDifficulty={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,id) } }},
    onNote={quick(a,"note",true);onClearSelection()},
    onRecord={closeSelection();onRecord(id)},onClose=::closeSelection,
    extra={ Box {
     IconButton(onClick={moreActions=!moreActions},modifier=Modifier.size(32.dp)) { Icon(androidx.compose.material.icons.Icons.Default.MoreHoriz,"Autres actions du verset") }
     DropdownMenu(moreActions,{moreActions=false}) { Column(Modifier.widthIn(max=340.dp)) {
   if(!double) {
   allReaderInk(state,source).firstOrNull { ink -> ink.anchor?.num("verseId")==a.num("verseId")&&ink.anchor?.get("wordId")==a["wordId"]&&ink.anchor?.get("wordIndex")==a["wordIndex"] }?.let { ink -> TextButton(onClick={selection=null;editing=ink.id}) { Text("Consulter la note") } }
   if(a.str("wordText").isNotBlank()) Text(a.str("wordText"),fontFamily=arabicInterfaceFont)
   if(a["wordId"]!=null||a["wordIndex"]!=null) TextButton(onClick={quick(a,"note")}) { Text("Annoter ce mot") }
   TextButton(onClick={quick(a,"note",true)}) { Text("Annoter le verset") }
   Row { listOf("highlight" to "Surligner","circle" to "Entourer","underline" to "Souligner").forEach { (kind,label) -> TextButton(onClick={quick(a,kind)}) { Text(label) } } }
   TextButton(onClick={quick(a,"note")}) { Text("Commenter") }
   if(source!="coranTest"&&source!="tajweed") TextButton(onClick={
    selection=null
    vm.action {
     vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive("coranTest")))) }
     withContext(Dispatchers.Main) { onPage(vm.repo.quran.sourcePage(a.num("verseId"),"coranTest")) }
    }
   }) { Text("Choisir un mot dans le Coran avec Tajwid") }
   }
   TextButton(onClick={vm.action {vm.repo.mutate { Bookmarks(vm.repo.quran).save(it,a.num("verseId"),source,targetPage) }};closeSelection()}) {Text("Marquer la lecture")}
   Row { TextButton(onClick={selection=null;onAudio(a.num("verseId"),false)}) { Text("Écouter") };TextButton(onClick={selection=null;onAudio(a.num("verseId"),true)}) { Text("Répéter") } }
   TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.toggleDifficulty(it,a.num("verseId")) } };selection=null}) { Text("Marquer comme difficile") }

     } }
    } })
  }
 }
}
@Composable private fun PrintedMushafPage(vm: CoranViewModel,s: JsonObject,source: String,page: Int,primary: Triple<JsonObject?,Bitmap?,JsonArray?>?,selected: Int?,playing: Int?,annotating: Boolean,prefs: TabletPreferences,tool: String,modifier: Modifier,double: Boolean,tablet: Boolean,onVerse: (Int)->Unit,onPage: (Int)->Unit,onSelection: (JsonObject)->Unit,onOpen: (ReaderInk)->Unit,onStroke: (InkStroke,List<Int>)->Unit,onErase: (ReaderInk)->Unit,onActive: (Boolean)->Unit) {
 var pagePosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
 val context=LocalContext.current;val owner by vm.repo.user.collectAsStateWithLifecycle()
 val loaded by produceState<Pair<Int,Triple<JsonObject?,Bitmap?,JsonArray?>>?>(null,source,page,primary) {
  val ready=primary?.takeIf { it.first!=null||it.second!=null }
  value=page to (ready?:withContext(Dispatchers.IO) { loadPrintedPage(context,vm.repo.quran,source,page) })
 }
 val payload=loaded?.takeIf { it.first==page }?.second

 if(source=="coran_1441"&&payload!=null&&payload.second==null) Box(modifier) { Text("Télécharge cette édition depuis Réglages → Téléchargements") }
 else AndroidView(factory={MushafView(it)},modifier=modifier.onGloballyPositioned { pagePosition=it.positionInRoot() }.testTag("printed-page-"+page),update={v ->
  v.pageCurlEnabled=prefs.pageCurl
  if(payload!=null&&(v.page!=page||v.source!=source||v.tag!=payload)) { v.load(vm.repo.quran,page,source,payload.first,payload.second,payload.third);v.tag=payload }
  v.paper=paperColor(s.obj("reader").str("paper"));v.selected=selected;v.playing=playing;v.annotationOwner=owner?:"guest";v.annotationMode=annotating;v.stylusOnly=prefs.stylusOnly;v.inkTool=tool;v.inkColor=prefs.color;v.inkWidth=if(tool=="highlight") prefs.highlightWidth else prefs.penWidth;v.inkPenStyle=prefs.penStyle
  v.annotationsEnabled=true;v.annotations=if(s.str("userId")==owner.orEmpty()) allReaderInk(s,source) else emptyList();v.onVerse=onVerse;v.onPage=onPage;v.onViewportChange={onSelection(json("dismiss" to true))};v.onSelection={a -> onSelection(a.with("anchorRootX" to JsonPrimitive(pagePosition.x+((a["anchorX"] as? JsonPrimitive)?.floatOrNull?:0f)),"anchorRootTop" to JsonPrimitive(pagePosition.y+((a["anchorTop"] as? JsonPrimitive)?.floatOrNull?:0f)),"anchorRootBottom" to JsonPrimitive(pagePosition.y+((a["anchorBottom"] as? JsonPrimitive)?.floatOrNull?:0f)))) };v.onInkClick=onOpen;v.onInkStroke=onStroke;v.onInkErase=onErase;v.onInkActive=onActive
  v.marginEnabled=!double||hasMarginInk(v.annotations.filter { it.page==page })||v.annotations.any { it.page==page&&(it.note.isNotEmpty()||it.text.isNotBlank()||it.tool=="note") };v.bookmarks=s.obj("bookmarks").values.map { it.jsonObject }.filter { it.str("deletedAt").isBlank() }.map { it.num("verseId") }.toSet();v.difficulties=s.obj("difficultyMarkers").keys.mapNotNull { it.toIntOrNull() }.toSet();v.invalidate()
 })
}
