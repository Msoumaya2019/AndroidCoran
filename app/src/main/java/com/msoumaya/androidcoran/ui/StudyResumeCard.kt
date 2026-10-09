package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt
private data class StudyDetail(val label: String,val range: VerseRange)
@Composable fun StudyResumeCard(q: Quran,study: StudyProgress,record: JsonObject,onResume: ()->Unit) {
 val planned=range(record);val through=record.num("through",planned.start-1);val remaining=study.remaining(planned,through)?:return
 val source=record.str("source","coranTest");val learning=record.str("mode")=="learning"
 val metrics=remember(record) { study.metrics(planned,through,source) }
 var expanded by rememberSaveable(record.str("id"),record.str("mode")) { mutableStateOf(false) }
 val rows=remember(record) {
  if(metrics.pages) metrics.pageList.map { page -> val r=q.sourceRange(page,source);StudyDetail("Page $page",VerseRange(maxOf(planned.start,r.start),minOf(planned.end,r.end))) }
  else planned.ids.map { id -> StudyDetail((if(q.verse(planned.start).surah!=q.verse(planned.end).surah) "${q.surah(id).name} · " else "")+"Verset ${q.verse(id).ayah}",VerseRange(id,id)) }
 }
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Panel(if(learning) "Apprentissage à continuer" else "Révision à continuer","Poursuivez là où vous vous êtes arrêté") {
   val firstPage=q.sourcePage(remaining.start,source)
   Text(if(metrics.pages) if(firstPage==metrics.last) "Page ${metrics.last}" else "Pages $firstPage à ${metrics.last}" else q.reference(remaining),style=MaterialTheme.typography.titleMedium)
   val unit=if(metrics.pages) if(metrics.remaining==1) "page restante" else "pages restantes" else if(metrics.remaining==1) "verset restant" else "versets restants"
   Text("${metrics.remaining} $unit")
   Text("${metrics.done} / ${metrics.total} ${metrics.unit} · ${(metrics.ratio*100).roundToInt()} %")
   LinearProgressIndicator(progress={metrics.ratio},modifier=Modifier.fillMaxWidth())
   Button(onClick=onResume,modifier=Modifier.fillMaxWidth()) { Text(if(learning) "Reprendre mon apprentissage" else "Reprendre ma révision") }
   if(through>=planned.start) Text("✓ ${if(metrics.pages&&metrics.done>0) if(metrics.done==1) "Page ${metrics.first}" else "Pages ${metrics.first} à ${metrics.first+metrics.done-1}" else q.reference(VerseRange(planned.start,through))} · Validation effectuée",color=MaterialTheme.colorScheme.primary)
  }
  Panel("Détail de la séance","${metrics.total} ${metrics.unit} au total · Juz ${q.juzs.indexOfFirst { planned.start in it.start..it.end }+1}") {
   TextButton(onClick=onResume) { Text("Voir dans le Coran") }
   (if(expanded) rows else rows.take(12)).forEach { row ->
    val done=row.range.end<=through;val partial=!done&&row.range.start<=through
    val status=if(done) if(learning) "Appris" else "Révisée" else if(partial) "À continuer" else if(learning) "À apprendre" else "À réviser"
    Row(Modifier.fillMaxWidth().heightIn(min=46.dp),verticalAlignment=Alignment.CenterVertically) {
     Icon(if(done) Icons.Default.CheckCircle else if(partial) Icons.Default.Timelapse else Icons.Default.RadioButtonUnchecked,contentDescription=null,tint=if(done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
     Text(row.label,Modifier.weight(1f).padding(horizontal=8.dp));Text(status,style=MaterialTheme.typography.labelMedium)
    }
    HorizontalDivider()
   }
   if(rows.size>12) TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Réduire" else "Voir tout") }
  }
 }
}
