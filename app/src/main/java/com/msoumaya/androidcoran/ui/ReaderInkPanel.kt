package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.Instant

@Composable fun ReaderInkEditor(annotation: ReaderInk,onChange: (ReaderInk)->Unit,onDelete: ()->Unit,onClose: ()->Unit) {
    var saved by rememberSaveable(annotation.id) { mutableStateOf(annotation.toJson().toString()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val draft=readerInk(json("readerInk" to json("draft" to Json.parseToJsonElement(saved))),annotation.page).firstOrNull()?:annotation
    fun update(strokes: List<InkStroke>) { val next=draft.copy(note=strokes,updatedAt=Instant.now().toString());saved=next.toJson().toString();onChange(next) }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=900.dp).fillMaxWidth(.96f).fillMaxHeight(.9f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Annotation manuscrite · page "+annotation.page,style=MaterialTheme.typography.titleLarge)
                Text("Écrivez librement au stylet. Les traits sont sauvegardés sans conversion en texte.")
                AndroidView(factory={HandwritingView(it)},modifier=Modifier.fillMaxWidth().weight(1f),update={v -> v.strokes=draft.note;v.onStroke={update(draft.note+it)} })
                Row { TextButton(enabled=draft.note.isNotEmpty(),onClick={update(draft.note.dropLast(1))}) { Text("Annuler le trait") };TextButton(enabled=draft.note.isNotEmpty(),onClick={update(emptyList())}) { Text("Effacer la note") } }
                Row { TextButton(onClick={confirmDelete=true}) { Text("Supprimer l’annotation") };Spacer(Modifier.weight(1f));Button(onClick=onClose) { Text("Fermer") } }
            }
        }
    }
    if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Supprimer cette annotation ?")},text={Text("Le tracé autour du passage et sa note manuscrite seront supprimés.")},confirmButton={TextButton(onClick=onDelete) { Text("Supprimer") }},dismissButton={TextButton(onClick={confirmDelete=false}) { Text("Conserver") }})
}
@Composable fun ReaderInkControls(vm: CoranViewModel,s: JsonObject,page: Int,enabled: Boolean,onToggle: ()->Unit,onEdit: (ReaderInk)->Unit) {
    var list by rememberSaveable(page) { mutableStateOf(false) }
    val annotations=readerInk(s,page)
    Column {
        FilledTonalButton(onClick=onToggle) { Text(if(enabled) "Terminer le tracé" else "Annoter au stylet") }
        if(enabled) Text("Entourez librement le passage",style=MaterialTheme.typography.labelSmall)
        if(annotations.isNotEmpty()) TextButton(onClick={list=true}) { Text("Annotations de cette page ("+annotations.size+")") }
    }
    if(list) AlertDialog(onDismissRequest={list=false},title={Text("Annotations manuscrites")},text={Column(Modifier.verticalScroll(rememberScrollState())) { annotations.forEachIndexed { index,ink -> TextButton(onClick={list=false;onEdit(ink)}) { Text("Annotation "+(index+1)+(if(ink.verses.isNotEmpty()) " · "+vm.repo.quran.reference(VerseRange(ink.verses.min(),ink.verses.max())) else "")) } }},confirmButton={TextButton(onClick={list=false}) { Text("Fermer") }})
}
