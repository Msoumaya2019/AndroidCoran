package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

private val inkDraftSaver=Saver<ReaderInk,String>(save={it.toJson().toString()},restore={inkFromJson(Json.parseToJsonElement(it).jsonObject)})
@Composable fun ReaderInkEditor(annotation: ReaderInk,onChange: (ReaderInk)->Unit,onDelete: ()->Unit,onClose: ()->Unit,stylusOnly: Boolean=false,inline: Boolean=false,availableCategories: List<String> = noteCategories,startEditing: Boolean=true,onSave: (suspend (ReaderInk)->Unit)?=null) {
 val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
 val focusManager=androidx.compose.ui.platform.LocalFocusManager.current
 val scope=rememberCoroutineScope()
 var draft by rememberSaveable(annotation.id,stateSaver=inkDraftSaver) { mutableStateOf(annotation) }
 var editing by rememberSaveable(annotation.id) { mutableStateOf(startEditing) }
 var confirmDelete by rememberSaveable { mutableStateOf(false) };var confirmClose by remember { mutableStateOf(false) }
 var saving by remember { mutableStateOf(false) };var error by remember { mutableStateOf("") }
 var handwriting by rememberSaveable(annotation.id) { mutableStateOf(true) }
 var allowFinger by rememberSaveable(annotation.id) { mutableStateOf(!stylusOnly) }
 var optionsExpanded by rememberSaveable { mutableStateOf(true) };var metadataOpen by remember { mutableStateOf(false) }
 var custom by remember { mutableStateOf(false) };var customName by rememberSaveable { mutableStateOf("") }
 var leftDp by rememberSaveable(annotation.id) { mutableFloatStateOf(-1f) };var topDp by rememberSaveable(annotation.id) { mutableFloatStateOf(-1f) }
 var resizedWidth by rememberSaveable(annotation.id) { mutableFloatStateOf(0f) };var resizedHeight by rememberSaveable(annotation.id) { mutableFloatStateOf(0f) }
 var measuredWidth by remember { mutableFloatStateOf(0f) };var measuredHeight by remember { mutableFloatStateOf(0f) }
 fun update(next: ReaderInk) {if(!saving) draft=next}
 fun close() { if(saving) return;if(editing&&draft!=annotation) confirmClose=true else onClose() }
 fun save() {
  if(saving||!draft.canSaveNote()) return
  saving=true;error="";focusManager.clearFocus();keyboard?.hide()
  val value=draft.copy(updatedAt=annotationTime(annotation.updatedAt))
  scope.launch {try {if(onSave!=null) onSave(value) else onChange(value);onClose()} catch(e: Exception) {if(e is CancellationException) throw e;error=e.message?:"La note n’a pas pu être enregistrée."} finally {saving=false}}
 }
 @Composable fun metadata(withTitle: Boolean=false) {
  Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
   if(withTitle) OutlinedTextField(draft.title.ifBlank {"Annotation · page "+annotation.page},{update(draft.copy(title=it))},label={Text("Titre de la note")},singleLine=true,modifier=Modifier.fillMaxWidth())
   Text("Catégories · au moins une obligatoire",style=MaterialTheme.typography.labelLarge)
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
    (availableCategories+draft.categories).distinct().forEach { category -> FilterChip(category in draft.categories,{update(draft.copy(categories=if(category in draft.categories) draft.categories-category else draft.categories+category))},label={Text(category)},enabled=!saving) }
    TextButton(onClick={custom=true},enabled=!saving) {Text("+ Créer une catégorie")}
   }
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically) {Text("Icône : ");tabletColors.forEach { (name,c) -> FilterChip(draft.iconColor==c,{update(draft.copy(iconColor=c))},label={Text(name,color=Color(android.graphics.Color.parseColor(c)))},enabled=!saving)} }
  }
 }
 Dialog(onDismissRequest=::close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
  val window=(LocalView.current.parent as? DialogWindowProvider)?.window
  SideEffect {window?.setDimAmount(.08f)}
  BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
   val density=LocalDensity.current;val availableW=maxWidth.value;val availableH=maxHeight.value
   val minW=minOf(280f,availableW);val minH=minOf(if(editing) 300f else 160f,availableH)
   val defaultW=if(editing) minOf(availableW*.96f,900f) else minOf(availableW-16f,460f).coerceAtLeast(minW)
   val windowW=(resizedWidth.takeIf {it>0f}?:defaultW).coerceIn(minW,availableW)
   val defaultH=availableH*(if(availableW>=600) .86f else .96f)
   val fixedH=if(resizedHeight>0) resizedHeight.coerceIn(minH,availableH) else if(editing) defaultH.coerceIn(minH,availableH) else 0f
   val actualH=if(fixedH>0) fixedH else measuredHeight.takeIf {it>0}?:220f
   val left=(leftDp.takeIf {it>=0}?:if(!editing&&availableW>=900) 16f else (availableW-windowW)/2).coerceIn(0f,(availableW-windowW).coerceAtLeast(0f))
   val top=(topDp.takeIf {it>=0}?:if(!editing) 64f else (availableH-actualH)/2).coerceIn(0f,(availableH-actualH).coerceAtLeast(0f))
   val latestLeft=rememberUpdatedState(left);val latestTop=rememberUpdatedState(top)
   val imeVisible=WindowInsets.ime.getBottom(density)>0
   val compact=editing&&(fixedH<520f||windowW<340f)
   val heightModifier=if(fixedH>0) Modifier.height(fixedH.dp) else Modifier.wrapContentHeight()
   Surface(Modifier.offset {IntOffset(with(density){left.dp.toPx()}.roundToInt(),with(density){top.dp.toPx()}.roundToInt())}.width(windowW.dp).heightIn(min=minH.dp,max=(if(!editing&&resizedHeight==0f) maxOf(minH,availableH*.82f) else availableH).dp).then(heightModifier).testTag("floating-note-window").onSizeChanged {measuredWidth=it.width/density.density;measuredHeight=it.height/density.density},shape=MaterialTheme.shapes.large,tonalElevation=6.dp,shadowElevation=12.dp) {
    Column((if(fixedH>0) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Text(if(editing) "⠿ Déplacer la note" else "⠿ "+draft.title.ifBlank {"Annotation · page "+annotation.page},Modifier.weight(1f).semantics {contentDescription="Déplacer la note"}.pointerInput(availableW,availableH) {detectDragGestures(onDragStart={leftDp=latestLeft.value;topDp=latestTop.value}) {change,amount -> change.consume();leftDp=(leftDp+amount.x/density.density).coerceIn(0f,(availableW-measuredWidth).coerceAtLeast(0f));topDp=(topDp+amount.y/density.density).coerceIn(0f,(availableH-measuredHeight).coerceAtLeast(0f))}},style=MaterialTheme.typography.titleSmall,maxLines=2)
      IconButton(onClick={leftDp=0f;topDp=0f;resizedWidth=availableW;resizedHeight=availableH},modifier=Modifier.size(32.dp)) {Icon(Icons.Default.OpenInFull,"Agrandir la note",Modifier.size(18.dp))}
      IconButton(onClick={leftDp=latestLeft.value;topDp=latestTop.value;resizedWidth=minOf(320f,availableW);resizedHeight=minOf(if(editing) 340f else 180f,availableH)},modifier=Modifier.size(32.dp)) {Icon(Icons.Default.CloseFullscreen,"Réduire la note",Modifier.size(18.dp))}
     }
     if(!editing) {
      Column(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       if(draft.text.isNotBlank()) SelectionContainer {Text(draft.text,style=MaterialTheme.typography.bodyLarge)}
       if(draft.note.isNotEmpty()) BoxWithConstraints(Modifier.fillMaxWidth()) {
        val aspect=draft.noteAspect?:LEGACY_NOTE_ASPECT;val frame=noteContentFrame(draft.note,aspect)
        val previewH=(maxWidth/frame.aspect).coerceIn(48.dp,300.dp)
        AndroidView(factory={HandwritingView(it)},modifier=Modifier.fillMaxWidth().height(previewH).semantics {contentDescription="Note manuscrite en lecture seule"},update={v -> v.readOnly=true;v.fixedAspect=aspect;v.cropToInk=true;v.strokes=draft.note})
       }
       if(draft.text.isBlank()&&draft.note.isEmpty()) Text("Cette annotation est visible sur la page.")
       if(draft.categories.isNotEmpty()) Text(draft.categories.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
     } else {
      if(!compact) OutlinedTextField(draft.title.ifBlank {"Annotation · page "+annotation.page},{update(draft.copy(title=it))},label={Text("Titre de la note")},singleLine=true,enabled=!saving,modifier=Modifier.fillMaxWidth())
      Row(Modifier.horizontalScroll(rememberScrollState())) {
       FilterChip(handwriting,{handwriting=true},label={Text("Stylet")},enabled=!saving);FilterChip(!handwriting,{handwriting=false;optionsExpanded=false},label={Text("Clavier")},enabled=!saving)
       if(handwriting) FilterChip(allowFinger,{allowFinger=!allowFinger},label={Text(if(allowFinger) "Doigt et stylet autorisés" else "Autoriser le doigt ou un stylet non reconnu")},enabled=!saving)
       if(handwriting&&compact) {IconButton(enabled=!saving&&draft.note.isNotEmpty(),onClick={update(draft.copy(note=draft.note.dropLast(1)))}) {Icon(Icons.Default.Undo,"Annuler le trait")};IconButton(enabled=!saving&&draft.note.isNotEmpty(),onClick={update(draft.copy(note=emptyList()))}) {Icon(Icons.Default.DeleteSweep,"Effacer la note")}}
      }
      Surface(Modifier.fillMaxWidth().weight(1f),color=Color.White,shape=MaterialTheme.shapes.small) {
       if(handwriting) AndroidView(factory={HandwritingView(it)},modifier=Modifier.fillMaxSize().semantics {contentDescription="Zone de note manuscrite"},update={v -> v.stylusOnly=stylusOnly&&!allowFinger;v.readOnly=saving;v.fixedAspect=draft.noteAspect?:if(draft.note.isNotEmpty()) LEGACY_NOTE_ASPECT else null;v.strokes=draft.note;v.onAspect={update(draft.copy(noteAspect=it))};v.onStroke={update(draft.copy(note=draft.note+it))}})
       else TextField(draft.text,{update(draft.copy(text=it))},enabled=!saving,modifier=Modifier.fillMaxSize(),placeholder={Text("Écrivez votre note…")},colors=TextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White))
      }
      if(handwriting&&!compact) Row {TextButton(enabled=!saving&&draft.note.isNotEmpty(),onClick={update(draft.copy(note=draft.note.dropLast(1)))}) {Text("Annuler le trait")};TextButton(enabled=!saving&&draft.note.isNotEmpty(),onClick={update(draft.copy(note=emptyList()))}) {Text("Effacer la note")}}
      TextButton(enabled=!saving,onClick={focusManager.clearFocus();keyboard?.hide();if(compact&&!(imeVisible&&resizedHeight==0f)) metadataOpen=true else optionsExpanded=!optionsExpanded}) {Text(if(compact) "Catégories et couleur · "+draft.categories.size+" sélectionnée(s)" else if(optionsExpanded) "Masquer catégories et couleur" else "Catégories et couleur · "+draft.categories.size+" sélectionnée(s)")}
      if(optionsExpanded&&!compact) metadata()
      if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
     }
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      if(editing) IconButton(enabled=!saving,onClick={confirmDelete=true},modifier=Modifier.size(36.dp)) {Icon(Icons.Default.Delete,"Supprimer l’annotation")}
      TextButton(enabled=!saving,onClick=::close) {Text("Fermer")};Spacer(Modifier.weight(1f))
      if(editing) Button(enabled=draft.canSaveNote()&&!saving,onClick=::save,contentPadding=PaddingValues(horizontal=10.dp)) {if(saving) CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp) else Text("Enregistrer")}
      else Button(onClick={editing=true;focusManager.clearFocus();keyboard?.hide()}) {Text("Modifier")}
      Box(Modifier.size(36.dp).testTag("note-resize-handle").semantics {contentDescription="Redimensionner la note"}.pointerInput(availableW,availableH,editing) {detectDragGestures(onDragStart={leftDp=latestLeft.value;topDp=latestTop.value;resizedWidth=measuredWidth;resizedHeight=measuredHeight}) {change,amount -> change.consume();resizedWidth=(resizedWidth+amount.x/density.density).coerceIn(minW,(availableW-leftDp).coerceAtLeast(minW));resizedHeight=(resizedHeight+amount.y/density.density).coerceIn(minH,(availableH-topDp).coerceAtLeast(minH))}},contentAlignment=Alignment.Center) {Icon(Icons.Default.DragHandle,null,Modifier.size(22.dp))}
     }
    }
   }
  }
 }
 if(metadataOpen) AlertDialog(onDismissRequest={metadataOpen=false},title={Text("Titre, catégories et couleur")},text={Column(Modifier.verticalScroll(rememberScrollState())) {metadata(true)}},confirmButton={TextButton(onClick={metadataOpen=false}) {Text("Terminer")}})
 if(custom) AlertDialog(onDismissRequest={custom=false},title={Text("Créer une catégorie")},text={OutlinedTextField(customName,{customName=it.take(80)},label={Text("Nom de la catégorie")})},confirmButton={TextButton(enabled=customName.isNotBlank(),onClick={update(draft.copy(categories=(draft.categories+customName.trim()).distinct()));customName="";custom=false}) {Text("Ajouter")}},dismissButton={TextButton(onClick={custom=false}) {Text("Annuler")}})
 if(confirmClose) AlertDialog(onDismissRequest={confirmClose=false},title={Text("Conserver les modifications ?")},text={Text("Enregistrez la note avec au moins une catégorie, ou abandonnez les modifications.")},confirmButton={TextButton(enabled=draft.canSaveNote()&&!saving,onClick=::save) {Text("Enregistrer")}},dismissButton={TextButton(enabled=!saving,onClick={confirmClose=false;onClose()}) {Text("Abandonner")}})
 if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Supprimer cette annotation ?")},text={Text("Le tracé et les commentaires seront supprimés.")},confirmButton={TextButton(onClick=onDelete) {Text("Supprimer")}},dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Conserver")}})
}
@Composable fun ReaderInkControls(vm: CoranViewModel,s: JsonObject,page: Int,enabled: Boolean,onToggle: ()->Unit,onEdit: (ReaderInk)->Unit) {
    var list by rememberSaveable(page) { mutableStateOf(false) }
    val annotations=readerInk(s,page)
    Row {
        FilledTonalIconButton(onClick=onToggle) { Icon(if(enabled) Icons.Default.Done else Icons.Default.Edit,if(enabled) "Terminer le tracé" else "Annoter au stylet") }
        if(annotations.isNotEmpty()) FilledTonalIconButton(onClick={list=true}) { Icon(Icons.Default.StickyNote2,"Annotations de cette page ("+annotations.size+")") }
    }
    if(list) AlertDialog(
        onDismissRequest={list=false},
        title={Text("Annotations manuscrites")},
        text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                annotations.forEachIndexed { index,ink ->
                    TextButton(onClick={list=false;onEdit(ink)}) {
                        val reference=if(ink.verses.isNotEmpty()) " · "+vm.repo.quran.reference(VerseRange(ink.verses.min(),ink.verses.max())) else ""
                        Text("Annotation "+(index+1)+reference)
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={list=false}) { Text("Fermer") }}
    )
}
