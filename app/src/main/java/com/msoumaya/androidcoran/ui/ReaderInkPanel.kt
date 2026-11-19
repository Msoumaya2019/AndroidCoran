package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.Instant

@Composable fun ReaderInkEditor(annotation: ReaderInk,onChange: (ReaderInk)->Unit,onDelete: ()->Unit,onClose: ()->Unit,stylusOnly: Boolean=false,inline: Boolean=false) {
    var draft by remember(annotation.id) { mutableStateOf(annotation) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var handwriting by rememberSaveable(annotation.id) { mutableStateOf(true) }
    fun update(next: ReaderInk) { val value=next.copy(updatedAt=annotationTime(draft.updatedAt));draft=value;onChange(value) }
    val body: @Composable ()->Unit = {
        Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Annotation · page "+annotation.page,style=MaterialTheme.typography.titleLarge)
            Row { FilterChip(!handwriting,{handwriting=false},label={Text("Commentaire")});FilterChip(handwriting,{handwriting=true},label={Text("Stylet")}) }
            if(!handwriting) OutlinedTextField(draft.text,{update(draft.copy(text=it))},label={Text("Commentaire")},modifier=Modifier.fillMaxWidth().weight(1f))
            else {
                Text(if(stylusOnly) "Écrivez avec le stylet" else "Écrivez librement à la main",style=MaterialTheme.typography.bodySmall)
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                    val padWidth=minOf(maxWidth,maxHeight*1.4f)
                    AndroidView(factory={HandwritingView(it)},modifier=Modifier.width(padWidth).height(padWidth/1.4f).semantics { contentDescription="Zone de note manuscrite" },update={v -> v.stylusOnly=stylusOnly;v.strokes=draft.note;v.onStroke={update(draft.copy(note=draft.note+it))} })
                }
                Row { TextButton(enabled=draft.note.isNotEmpty(),onClick={update(draft.copy(note=draft.note.dropLast(1)))}) { Text("Annuler le trait") };TextButton(enabled=draft.note.isNotEmpty(),onClick={update(draft.copy(note=emptyList()))}) { Text("Effacer la note") } }
            }
            TextButton(onClick={confirmDelete=true}) { Text("Supprimer l’annotation") }
            Button(onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text("Fermer") }
        }
    }
    if(inline) Surface(Modifier.fillMaxSize(),tonalElevation=3.dp,content=body)
    else Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) { Surface(Modifier.widthIn(max=900.dp).fillMaxWidth(.96f).fillMaxHeight(.9f),shape=MaterialTheme.shapes.large,content=body) }
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
