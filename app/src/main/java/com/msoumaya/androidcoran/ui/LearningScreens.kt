package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.LocalDate

@Composable fun ProgramScreen(vm: CoranViewModel,s: JsonObject,open: (JsonObject)->Unit) {
    val sessions=s.arr("sessions").map { it.jsonObject };val today=LocalDate.now();val week=weeklyProgress(s,today)
    var period by rememberSaveable { mutableStateOf("Jour") };var history by rememberSaveable { mutableStateOf(false) }
    val upcoming=sessions.filter { it.str("status")=="todo"&&it.str("scheduledDate",it.str("date")) in today.toString()..today.plusDays(10).toString() }
    val future=when(period) { "Jour" -> upcoming.filter { it.str("scheduledDate",it.str("date"))==upcoming.firstOrNull()?.str("scheduledDate",upcoming.firstOrNull()?.str("date")?:today.toString()) };"Semaine" -> upcoming.filter { it.str("scheduledDate",it.str("date"))<=today.plusDays(7).toString() };else -> upcoming }
    val partial=sessions.filter { s.obj("studyProgress").obj("learning:${it.str("id")}").str("status")=="partial" }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("Mon programme",style=MaterialTheme.typography.headlineSmall)
            Text(s.obj("goal").str("label"));Text("Rythme : ${paceLabels[s.str("pace")]?:s.str("pace")}")
            Text("Cette semaine : ${week.done} / ${week.total} séances")
            LinearProgressIndicator(progress={week.ratio},modifier=Modifier.fillMaxWidth())
            Button(onClick={vm.action { vm.repo.mutate { vm.repo.program.generate(it) } }}) { Text("Créer / actualiser") }
        }
        items(partial,key={"resume-${it.str("id")}"}) { session -> Panel("Reprendre ma séance",vm.repo.quran.reference(range(session)),{open(session)}) { Text("${s.obj("studyProgress").obj("learning:${session.str("id")}").num("through")-session.num("start")+1} versets validés") } }
        item { Row { listOf("Jour","Semaine","Mois").forEach { FilterChip(selected=period==it,onClick={period=it},label={Text(it)}) } } }
        items(future,key={it.str("id")}) { session -> Panel(vm.repo.quran.reference(range(session)),"${session.str("scheduledDate",session.str("date"))} · ${sessionStatus(s,session)}",{open(session)}) { TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.program.postpone(it,session.str("id")) } }}) { Text("Reporter") } } }
        if(future.isEmpty()) item { Text("Aucune séance sur les 10 prochains jours") }
        item { TextButton(onClick={history=!history}) { Text(if(history) "Masquer l’historique" else "Voir l’historique") } }
        if(history) items(sessions.filter { it.str("status")!="todo" }.takeLast(20).reversed(),key={"history-${it.str("id")}"}) { row -> Panel(vm.repo.quran.reference(range(row)),"${row.str("scheduledDate",row.str("date"))} · ${sessionStatus(s,row)}") }
    }
}

@Composable fun ProgressScreen(vm: CoranViewModel,s: JsonObject) {
    val q=vm.repo.quran;val progress=vm.repo.program.progress(s);val known=knownIds(s).toSet();val stats=statistics(s,q)
    var period by rememberSaveable { mutableStateOf("Semaine") };var graph by rememberSaveable { mutableStateOf(false) }
    PageList {
        Text("Ma progression",style=MaterialTheme.typography.headlineSmall)
        Panel("${(progress.first*100).toInt()} % du Coran","${known.size} / 6236 versets mémorisés") { LinearProgressIndicator(progress={progress.first},modifier=Modifier.fillMaxWidth()) }
        Row { listOf("Jour","Semaine","Mois").forEach { FilterChip(selected=period==it,onClick={period=it},label={Text(it)}) } }
        Panel("Versets appris",(when(period) { "Jour"->stats.today;"Mois"->stats.month;else->stats.week }).toString()) { Text("${activityStreak(s)} jours d’affilée");Text("${q.pages.count { known.containsAll(it.ids) }} pages mémorisées");Text("${s.arr("reviewHistory").size} révisions faites") }
        TextButton(onClick={graph=!graph}) { Text(if(graph) "Masquer le graphique" else "Voir le graphique de la période") }
        if(graph) {
            val bars=progressSeries(s,period);val max=bars.maxOf { it.count }.coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(130.dp),verticalAlignment=Alignment.Bottom) { bars.forEach { bar -> Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) { Text(bar.count.toString(),style=MaterialTheme.typography.labelSmall);Box(Modifier.width(24.dp).height((bar.count.toFloat()/max*64).coerceAtLeast(3f).dp).background(MaterialTheme.colorScheme.primary));Text(bar.label,style=MaterialTheme.typography.labelSmall) } } }
        }
        Panel("Mon objectif",s.obj("goal").str("label")) { LinearProgressIndicator(progress={progress.second},modifier=Modifier.fillMaxWidth());Text("${(progress.second*100).toInt()} % appris") }
        Panel("Statistiques") { Text("${q.juzs.count { known.containsAll(it.ids) }} juz complétés");Text("${stats.hizbs} hizb complets");Text("${activityDates(s).size} jours actifs");Text("${(s["readPages"] as? JsonArray)?.size?:if(s["lastRead"]!=null) 1 else 0} pages lues");Text("${stats.weeklySessions} séances terminées cette semaine") }
        q.surahs.forEach { surah -> val count=surah.range.ids.count { it in known };if(count>0) Panel(surah.name,"$count / ${surah.range.ids.size} versets") }
    }
}
