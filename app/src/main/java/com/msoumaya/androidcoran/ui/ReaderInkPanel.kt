package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

private val inkDraftSaver=Saver<ReaderInk,String>(save={it.toJson().toString()},restore={inkFromJson(Json.parseToJsonElement(it).jsonObject)})
@Composable fun ReaderInkEditor(annotation: ReaderInk,onChange: (ReaderInk)->Unit,onDelete: ()->Unit,onClose: ()->Unit,stylusOnly: Boolean=false,inline: Boolean=false,availableCategories: List<String> = noteCategories) {
 val focusManager=androidx.compose.ui.platform.LocalFocusManager.current
 val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
 var draft by rememberSaveable(annotation.id,stateSaver=inkDraftSaver) { mutableStateOf(annotation) }
 var confirmDelete by rememberSaveable { mutableStateOf(false) }
 var confirmClose by remember { mutableStateOf(false) }
 var handwriting by rememberSaveable(annotation.id) { mutableStateOf(true) }
 var allowFinger by rememberSaveable(annotation.id) { mutableStateOf(!stylusOnly) }
 var optionsExpanded by rememberSaveable { mutableStateOf(true) }
 var custom by remember { mutableStateOf(false) };var customName by rememberSaveable { mutableStateOf("") }
 var dragX by rememberSaveable { mutableFloatStateOf(0f) };var dragY by rememberSaveable { mutableFloatStateOf(0f) }
 fun close() { if(draft!=annotation) confirmClose=true else onClose() }
 fun save() { if(draft.canSaveNote()) { onChange(draft.copy(updatedAt=annotationTime(annotation.updatedAt)));onClose() } }
 Dialog(onDismissRequest=::close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
  BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
   val density=LocalDensity.current
   val windowWidth=minOf(maxWidth*.96f,900.dp);val windowHeight=maxHeight*(if(maxWidth>=600.dp) .86f else .96f)
   val maxX=with(density) { (maxWidth-windowWidth).toPx()/2 };val maxY=with(density) { (maxHeight-windowHeight).toPx()/2 }
   Surface(Modifier.testTag("floating-note-window").align(Alignment.Center).offset { IntOffset(dragX.coerceIn(-maxX,maxX).roundToInt(),dragY.coerceIn(-maxY,maxY).roundToInt()) }.width(windowWidth).height(windowHeight),shape=MaterialTheme.shapes.large,tonalElevation=6.dp,shadowElevation=12.dp) {
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text("⠿ Déplacer la note",Modifier.fillMaxWidth().semantics { contentDescription="Déplacer la note" }.pointerInput(maxX,maxY) { detectDragGestures { change,amount -> change.consume();dragX=(dragX+amount.x).coerceIn(-maxX,maxX);dragY=(dragY+amount.y).coerceIn(-maxY,maxY) } },style=MaterialTheme.typography.labelLarge)
     OutlinedTextField(draft.title.ifBlank { "Annotation · page "+annotation.page },{draft=draft.copy(title=it)},label={Text("Titre de la note")},singleLine=true,modifier=Modifier.fillMaxWidth())
     Row(Modifier.horizontalScroll(rememberScrollState())) {
      FilterChip(handwriting,{handwriting=true},label={Text("Stylet")});FilterChip(!handwriting,{handwriting=false;optionsExpanded=false},label={Text("Clavier")})
      if(handwriting) FilterChip(allowFinger,{allowFinger=!allowFinger},label={Text(if(allowFinger) "Doigt et stylet autorisés" else "Autoriser le doigt ou un stylet non reconnu")})
     }
     Surface(Modifier.fillMaxWidth().weight(1f),color=Color.White,shape=MaterialTheme.shapes.small) {
      if(handwriting) AndroidView(factory={HandwritingView(it)},modifier=Modifier.fillMaxSize().semantics { contentDescription="Zone de note manuscrite" },update={v -> v.stylusOnly=stylusOnly&&!allowFinger;v.strokes=draft.note;v.onStroke={draft=draft.copy(note=draft.note+it)} })
      else TextField(draft.text,{draft=draft.copy(text=it)},modifier=Modifier.fillMaxSize(),placeholder={Text("Écrivez votre note…")},colors=TextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White))
     }
     if(handwriting) Row { TextButton(enabled=draft.note.isNotEmpty(),onClick={draft=draft.copy(note=draft.note.dropLast(1))}) { Text("Annuler le trait") };TextButton(enabled=draft.note.isNotEmpty(),onClick={draft=draft.copy(note=emptyList())}) { Text("Effacer la note") } }
     TextButton(onClick={focusManager.clearFocus();keyboard?.hide();optionsExpanded=!optionsExpanded}) { Text(if(optionsExpanded) "Masquer catégories et couleur" else "Catégories et couleur · "+draft.categories.size+" sélectionnée(s)") }
     if(optionsExpanded) {
     Text("Catégories · au moins une obligatoire",style=MaterialTheme.typography.labelLarge)
     Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
      (availableCategories+draft.categories).distinct().forEach { category -> FilterChip(category in draft.categories,{draft=draft.copy(categories=if(category in draft.categories) draft.categories-category else draft.categories+category)},label={Text(category)}) }
      TextButton(onClick={custom=true}) { Text("+ Créer une catégorie") }
     }
     Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically) { Text("Icône : ");tabletColors.forEach { (name,c) -> FilterChip(draft.iconColor==c,{draft=draft.copy(iconColor=c)},label={Text(name,color=Color(android.graphics.Color.parseColor(c)))}) } }
     }
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      IconButton(onClick={confirmDelete=true}) { Icon(Icons.Default.Delete,"Supprimer l’annotation") }
      TextButton(onClick=::close) { Text("Fermer") };Spacer(Modifier.weight(1f));Button(enabled=draft.canSaveNote(),onClick=::save) { Text("Enregistrer") }
     }
    }
   }
  }
 }
 if(custom) AlertDialog(onDismissRequest={custom=false},title={Text("Créer une catégorie")},text={OutlinedTextField(customName,{customName=it.take(80)},label={Text("Nom de la catégorie")})},confirmButton={TextButton(enabled=customName.isNotBlank(),onClick={draft=draft.copy(categories=(draft.categories+customName.trim()).distinct());customName="";custom=false}) { Text("Ajouter") }},dismissButton={TextButton(onClick={custom=false}) { Text("Annuler") }})
 if(confirmClose) AlertDialog(onDismissRequest={confirmClose=false},title={Text("Conserver les modifications ?")},text={Text("Enregistrez la note avec au moins une catégorie, ou abandonnez les modifications.")},confirmButton={TextButton(enabled=draft.canSaveNote(),onClick=::save) { Text("Enregistrer") }},dismissButton={TextButton(onClick={confirmClose=false;onClose()}) { Text("Abandonner") }})
 if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Supprimer cette annotation ?")},text={Text("Le tracé et les commentaires seront supprimés.")},confirmButton={TextButton(onClick=onDelete) { Text("Supprimer") }},dismissButton={TextButton(onClick={confirmDelete=false}) { Text("Conserver") }})
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
