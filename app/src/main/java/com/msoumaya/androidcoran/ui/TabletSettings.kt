package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
@Composable fun TabletSettings(vm: CoranViewModel,s: JsonObject) {
 val p=TabletPreferences.from(s.obj("reader").obj("tablet"))
 fun change(next: TabletPreferences) { vm.action { vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("tablet" to next.toJson()))) } } }
 Panel("AFFICHAGE ET APPAREIL") {
  Text("Mode tablette",style=MaterialTheme.typography.titleMedium)
  listOf("auto" to "Automatique","on" to "Activé","off" to "Désactivé").forEach { (key,label) -> FilterChip(selected=p.mode==key,onClick={change(p.copy(mode=key))},label={Text(label)}) }
  Row { Switch(p.toolsVisible,{change(p.copy(toolsVisible=it))});Text("Afficher les outils d’annotation") }
  Row { Switch(p.stylusOnly,{change(p.copy(stylusOnly=it))});Text("Dessiner uniquement avec le stylet") }
  Text("Le mode automatique tient compte de l’appareil et de la fenêtre. Désactive le stylet uniquement pour dessiner au doigt.")
 }
}
@Composable fun TabletToolbar(vertical: Boolean,tool: String,onTool: (String)->Unit,onPreferences: ()->Unit,onUndo: ()->Unit,onRedo: ()->Unit,canUndo: Boolean,canRedo: Boolean) {
 val tools=listOf("pen" to "Stylo","highlight" to "Surligneur","circle" to "Entourer","underline" to "Souligner","arrow" to "Flèche","eraser" to "Gomme","select" to "Sélection","note" to "Ajouter une note")
 @Composable fun buttons() {
  val icons=mapOf("pen" to Icons.Default.Edit,"highlight" to Icons.Default.Highlight,"circle" to Icons.Default.RadioButtonUnchecked,"underline" to Icons.Default.FormatUnderlined,"arrow" to Icons.Default.ArrowForward,"eraser" to Icons.Default.AutoFixHigh,"select" to Icons.Default.PanToolAlt,"note" to Icons.Default.StickyNote2)
  tools.forEach { (key,label) -> FilledTonalIconToggleButton(checked=tool==key,onCheckedChange={onTool(key)}) { Icon(icons.getValue(key),label) } }
  IconButton(enabled=canUndo,onClick=onUndo) { Icon(Icons.Default.Undo,"Annuler") };IconButton(enabled=canRedo,onClick=onRedo) { Icon(Icons.Default.Redo,"Rétablir") };IconButton(onClick=onPreferences) { Icon(Icons.Default.Palette,"Couleur et épaisseur") }
 }
 Surface(tonalElevation=3.dp,modifier=Modifier.testTag(if(vertical) "tablet-tools-vertical" else "tablet-tools-floating")) {
  if(vertical) Column(Modifier.width(72.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(6.dp)) { buttons() }
  else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) { buttons() }
 }
}
@Composable fun TabletInkPreferences(p: TabletPreferences,onChange: (TabletPreferences)->Unit,onClose: ()->Unit) {
 AlertDialog(onDismissRequest=onClose,title={Text("Couleur et épaisseur")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
  Row(Modifier.horizontalScroll(rememberScrollState())) { tabletColors.forEach { (name,color) -> FilterChip(selected=p.color==color,onClick={onChange(p.copy(color=color))},label={Text(name,color=androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color)))}) } }
  Text("Épaisseur du stylo");Row(Modifier.horizontalScroll(rememberScrollState())) { listOf(.0015f,.003f,.006f,.01f).forEachIndexed { i,w -> FilterChip(selected=p.penWidth==w,onClick={onChange(p.copy(penWidth=w))},label={Text(listOf("Fin","Moyen","Large","Très large")[i])}) } }
  Text("Épaisseur du surligneur");Row(Modifier.horizontalScroll(rememberScrollState())) { listOf(.01f,.018f,.03f,.05f).forEachIndexed { i,w -> FilterChip(selected=p.highlightWidth==w,onClick={onChange(p.copy(highlightWidth=w))},label={Text(listOf("Fin","Moyen","Large","Très large")[i])}) } }
  Row { listOf("ink" to "Encre","pencil" to "Crayon").forEach { (key,label) -> FilterChip(selected=p.penStyle==key,onClick={onChange(p.copy(penStyle=key))},label={Text(label)}) } }
  Row { Switch(p.stylusOnly,{onChange(p.copy(stylusOnly=it))});Text("Stylet uniquement") }
 }},confirmButton={TextButton(onClick=onClose) { Text("Fermer") }})
}
