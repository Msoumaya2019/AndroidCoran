package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
@Composable private fun StudyChoice(label: String,value: Int,options: List<Pair<Int,String>>,onSelect: (Int)->Unit) {
 var open by rememberSaveable(label) { mutableStateOf(false) }
 Text(label)
 OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().semantics { contentDescription=label }) { Text(options.firstOrNull { it.first==value }?.second?:"Choisir") }
 if(open) AlertDialog(onDismissRequest={open=false},title={Text(label)},text={
  LazyColumn(Modifier.heightIn(max=280.dp)) { items(options,key={it.first}) { (id,name) -> TextButton(onClick={onSelect(id);open=false},modifier=Modifier.fillMaxWidth()) { Text(name) } } }
 },confirmButton={},dismissButton={TextButton(onClick={open=false}) { Text("Annuler le choix") }})
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StudyCompletionSheet(q: Quran,study: StudyProgress,mode: String,range: VerseRange,through: Int,source: String,currentPage: Int,initialGrade: String="perfect",busy: Boolean=false,onClose: ()->Unit,onValidate: (Int,String)->Unit) {
 val remaining=study.remaining(range,through)
 var unit by rememberSaveable(mode,range.start,range.end,through) { mutableStateOf("verse") }
 var all by rememberSaveable(mode,range.start,range.end,through) { mutableStateOf(false) }
 var grade by rememberSaveable(mode,range.start,range.end,through) { mutableStateOf(initialGrade) }
 var endpoint by rememberSaveable(mode,range.start,range.end,through) { mutableIntStateOf(study.initialEndpoint(range,through,currentPage,source)?:range.end) }
 val metrics=remember(range,through,source) { study.metrics(range,through,source) };val selected=if(all) range.end else endpoint;val completed=remember(range,selected,source) { study.metrics(range,selected,source) };val validPages=remember(remaining,source) { remaining?.let { study.pages(it,source) }?:emptyList() }
 val learning=mode=="learning";val page=q.sourcePage(endpoint,source)
 ModalBottomSheet(onDismissRequest={if(!busy) onClose()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().heightIn(max=680.dp).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(if(learning) "Terminer mon apprentissage" else "Terminer ma révision",style=MaterialTheme.typography.headlineSmall)
   Text(if(learning) "Apprentissage prévu" else "Révision prévue",style=MaterialTheme.typography.titleMedium)
   Text("${metrics.label} · ${metrics.total} ${metrics.unit}")
   Text("${metrics.done} / ${metrics.total} ${metrics.unit} déjà validés")
   LinearProgressIndicator(progress={metrics.ratio},modifier=Modifier.fillMaxWidth())
   if(remaining!=null) {
    if(learning) Row { FilterChip(selected=all,onClick={all=true},label={Text("J’ai tout appris")});FilterChip(selected=!all,onClick={all=false},label={Text("J’ai appris jusqu’ici")}) }
    Text(if(learning) "J’ai appris jusqu’ici" else "J’ai révisé jusqu’ici",style=MaterialTheme.typography.titleMedium)
    Text("Choisis le dernier verset réellement ${if(learning) "appris" else "révisé"}.")
    if(!all) {
     Row {
      FilterChip(selected=unit=="page",onClick={val target=if(page in validPages) page else validPages.firstOrNull();if(target!=null) { unit="page";endpoint=q.studyEndpoint(target,remaining,source) }},enabled=validPages.isNotEmpty(),label={Text("Page")})
      FilterChip(selected=unit=="verse",onClick={unit="verse"},label={Text("Verset")})
     }
     if(unit=="verse") {
      val surahs=q.surahs.filter { it.range.end>=remaining.start&&it.range.start<=remaining.end }
      StudyChoice("Sourate",q.verse(endpoint).surah,surahs.map { it.number to "${it.name} (${it.number})" }) { n -> endpoint=maxOf(remaining.start,q.surahs[n-1].range.start) }
      val surah=q.surah(endpoint);val ids=(maxOf(remaining.start,surah.range.start)..minOf(remaining.end,surah.range.end)).toList()
      StudyChoice(if(learning) "Dernier verset appris" else "Dernier verset révisé",endpoint,ids.map { it to "Verset ${q.verse(it).ayah}" }) { endpoint=it }
     } else StudyChoice(if(learning) "Dernière page apprise" else "Dernière page révisée",page,validPages.map { it to "Page $it" }) { endpoint=q.studyEndpoint(it,remaining,source) }
    }
    val juz=q.juzs.indexOfFirst { selected in it.start..it.end }+1
    Text("Juz $juz · ${q.surah(selected).name} · verset ${q.verse(selected).ayah}")
    Text("Arrêt exact : ${q.reference(VerseRange(range.start,selected))}")
    Text("${completed.done} / ${metrics.total} ${metrics.unit} après validation")
    if(!learning) Row { listOf("perfect" to "Parfait","hesitant" to "Quelques hésitations","rework" to "À retravailler").forEach { (key,label) -> FilterChip(selected=grade==key,onClick={grade=key},modifier=Modifier.weight(1f),label={Text(label)}) } }
    Button(enabled=!busy&&selected in remaining.start..remaining.end,onClick={onValidate(selected,if(learning) "perfect" else grade)},modifier=Modifier.fillMaxWidth()) {
     Text(if(busy) "Enregistrement…" else if(selected==range.end) if(learning) "Valider tout l’apprentissage" else "Valider toute la révision" else if(unit=="page"&&!all) "Valider jusqu’à la page ${q.sourcePage(selected,source)}" else "Valider jusqu’au verset ${q.verse(selected).ayah}")
    }
   } else Text("Cette séance est déjà entièrement validée.")
   TextButton(enabled=!busy,onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text("Annuler") }
  }
 }
}
