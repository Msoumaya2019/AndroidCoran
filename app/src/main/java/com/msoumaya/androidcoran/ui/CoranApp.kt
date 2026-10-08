package com.msoumaya.androidcoran.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CoranApp(vm: CoranViewModel=viewModel()) {
    val s by vm.repo.state.collectAsStateWithLifecycle();val notice by vm.repo.notice.collectAsStateWithLifecycle();val user by vm.repo.user.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("Accueil") };var route by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableIntStateOf(1) };var sessionId by rememberSaveable { mutableStateOf<String?>(null) };var reviewTask by remember { mutableStateOf<ReviewTask?>(null) }
    val primary=when(s.str("accent")) { "rose"->Color(0xFFA95069);"green"->Color(0xFF54734E);"gold"->Color(0xFF916825);else->when(s.str("theme")) { "classic"->Color(0xFF153F36);"feminine"->Color(0xFF9D496B);"lilac"->Color(0xFF5F548E);"night"->Color(0xFF132B47);else->Color(0xFF7B285C) } }
    val palette=lightColorScheme(primary=primary,secondary=Color(0xFFC89A52),background=Color(0xFFFCFBF9),surface=Color.White,onSurface=Color(0xFF241C2B))
    MaterialTheme(colorScheme=palette) {
        BackHandler(route!=null) { route=null;sessionId=null;reviewTask=null }
        Scaffold(topBar={ TopAppBar(title={ Text(if(route=="reader") "Coran Mémoire" else route?:tab) },navigationIcon={ if(route!=null) IconButton(onClick={route=null}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour") } },actions={ IconButton(onClick={route="Réglages"}) { Icon(Icons.Default.Settings,"Réglages") };IconButton(onClick={route="Compte"}) { Icon(Icons.Default.AccountCircle,"Compte") } }) },bottomBar={ if(route==null) NavigationBar { listOf("Accueil" to Icons.Default.Home,"Coran" to Icons.Default.MenuBook,"Programme" to Icons.Default.DateRange,"Progrès" to Icons.Default.Insights,"Amis" to Icons.Default.People).forEach { (label,icon)->NavigationBarItem(selected=tab==label,onClick={tab=label},icon={Icon(icon,label)},label={Text(label,fontSize=10.sp)}) } } }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if(notice.isNotEmpty()) Text(notice,Modifier.fillMaxWidth().background(palette.primaryContainer).padding(10.dp),fontSize=12.sp)
                val open: (Int)->Unit = { id -> val source=s.obj("reader").str("mushaf","coranTest");page=vm.repo.quran.sourcePage(id,source);route="reader" }
                when(route) {
                    "reader" -> ReaderScreen(vm,s,page,{page=it.coerceIn(1,604)},sessionId,reviewTask)
                    "Compte" -> AccountScreen(vm,user)
                    "Réglages" -> SettingsScreen(vm,s,{route=it})
                    "Téléchargements" -> DownloadScreen()
                    "Objectif" -> GoalScreen(vm,s)
                    "Marques-pages" -> BookmarkScreen(vm,s,open)
                    "Révisions" -> RevisionScreen(vm,s) { task -> reviewTask=task;open(task.range.start) }
                    "Contenus quotidiens" -> ContentsScreen(vm)
                    "Quiz" -> QuizScreen(vm)
                    "Récitations" -> RecitationsScreen(vm)
                    "Signaler un problème" -> ReportScreen(vm)
                    else -> when(tab) {
                        "Accueil" -> HomeScreen(vm,s,{route=it}, { sessionId=null;reviewTask=null;open(s.obj("lastRead").num("verseId",1)) })
                        "Coran" -> QuranScreen(vm,s,open)
                        "Programme" -> ProgramScreen(vm,s) { session -> sessionId=session.str("id");reviewTask=null;val record=s.obj("studyProgress").obj("learning:${session.str("id")}");open((record.num("through",session.num("start")-1)+1).coerceAtMost(session.num("end"))) }
                        "Progrès" -> ProgressScreen(vm,s)
                        "Amis" -> FriendsScreen(vm)
                    }
                }
            }
        }
    }
}
@Composable fun PageList(content: @Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content) }
@Composable fun Panel(title: String,subtitle: String="",onClick: (()->Unit)?=null,content: @Composable ColumnScope.()->Unit={}) { Card(onClick={onClick?.invoke()},enabled=onClick!=null,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,Color(0xFFECE8E5))) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(title,style=MaterialTheme.typography.titleMedium);if(subtitle.isNotEmpty()) Text(subtitle,style=MaterialTheme.typography.bodyMedium);content() } } }
@Composable fun HomeScreen(vm: CoranViewModel,s: JsonObject,navigate: (String)->Unit,read: ()->Unit) { val progress=vm.repo.program.progress(s);PageList {
    Text("السلام عليكم",fontSize=28.sp,color=MaterialTheme.colorScheme.primary)
    Text("${s.obj("profile").str("firstName","Bienvenue")} · Chaque verset est un pas de plus",style=MaterialTheme.typography.titleLarge)
    Panel("Mon objectif",s.obj("goal").str("label"),{navigate("Objectif")}) { LinearProgressIndicator(progress={progress.second},modifier=Modifier.fillMaxWidth());Text("${(progress.second*100).toInt()} % appris") }
    Panel("Lire le Coran","Reprendre ma lecture",read)
    Panel("Révisions","Consolidation, cycle et versets à retravailler",{navigate("Révisions")})
    Panel("Quiz","Question du jour et défis entre amis",{navigate("Quiz")})
    Panel("Rappels et invocations","Contenus quotidiens",{navigate("Contenus quotidiens")})
    Panel("Mes récitations","Enregistrements et corrections",{navigate("Récitations")})
    Panel("Marques-pages","Retrouver mes versets",{navigate("Marques-pages")})
} }
@Composable fun QuranScreen(vm: CoranViewModel,s: JsonObject,open: (Int)->Unit) { var search by rememberSaveable { mutableStateOf("") };var division by rememberSaveable { mutableStateOf("Sourates") }
    Column(Modifier.padding(16.dp)) { OutlinedTextField(search,{search=it},label={Text("Rechercher une sourate")},modifier=Modifier.fillMaxWidth());Row { listOf("Sourates","Juz","Hizb").forEach { FilterChip(selected=division==it,onClick={division=it},label={Text(it)},modifier=Modifier.padding(end=6.dp)) } }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { if(division=="Sourates") items(vm.repo.quran.surahs.filter { it.name.contains(search,true)||it.arabic.contains(search)||it.number.toString()==search },key={it.number}) { surah -> Panel("${surah.number}. ${surah.name}","${surah.arabic} · ${surah.meaning} · ${surah.range.ids.size} versets",{open(surah.range.start)}) } else items(if(division=="Juz") vm.repo.quran.juzs else vm.repo.quran.hizbs) { r -> val units=if(division=="Juz") vm.repo.quran.juzs else vm.repo.quran.hizbs;Panel("$division ${units.indexOf(r)+1}",vm.repo.quran.reference(r),{open(r.start)}) } }
    }
}
@Composable fun ProgramScreen(vm: CoranViewModel,s: JsonObject,open: (JsonObject)->Unit) { val sessions=s.arr("sessions").map { it.jsonObject };Column(Modifier.padding(16.dp)) {
    Row { Button(onClick={vm.action { vm.repo.mutate { vm.repo.program.generate(it) } }}) { Text("Créer / actualiser") };Text("${sessions.count { it.str("status")=="done" }}/${sessions.size}",Modifier.padding(12.dp)) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(sessions,key={it.str("id")}) { session -> val record=s.obj("studyProgress").obj("learning:${session.str("id")}");Panel(vm.repo.quran.reference(range(session)),"${session.str("scheduledDate",session.str("date"))} · ${session.str("status")} ${if(record.str("status")=="partial") "· reprise au verset ${record.num("through")+1}" else ""}",{open(session)}) { if(session.str("status")=="todo") TextButton(onClick={vm.action { vm.repo.mutate { state -> touch(state.with("sessions" to element(state.arr("sessions").map { val v=it.jsonObject;if(v.str("id")==session.str("id")&&record.str("status")!="partial") v.with("status" to JsonPrimitive("postponed")) else v }))) } }}) { Text("Reporter") } } } }
} }
@Composable fun ProgressScreen(vm: CoranViewModel,s: JsonObject) { val p=vm.repo.program.progress(s);val known=knownIds(s).toSet();PageList { Panel("Ma progression") { Text("${known.size} versets mémorisés");Text("${(p.first*100).toInt()} % du Coran");LinearProgressIndicator(progress={p.first},modifier=Modifier.fillMaxWidth());Text("${(p.second*100).toInt()} % de mon objectif") };Panel("Statistiques") { Text("${s.arr("sessions").count { it.jsonObject.str("status")=="done" }} séances terminées");Text("${s.arr("reviewHistory").size} validations de révision");Text("${vm.repo.quran.hizbs.count { known.containsAll(it.ids) }} hizb complets") };vm.repo.quran.surahs.forEach { surah -> val n=surah.range.ids.count { it in known };if(n>0) Panel(surah.name,"$n / ${surah.range.ids.size} versets") } } }
@Composable fun GoalScreen(vm: CoranViewModel,s: JsonObject) { val q=vm.repo.quran;var selected by remember { mutableStateOf(s.obj("goal").arr("ranges").map { range(it.jsonObject) }) };var label by rememberSaveable { mutableStateOf(s.obj("goal").str("label")) };var pace by rememberSaveable { mutableStateOf(s.str("pace","verse3")) };var fromNas by rememberSaveable { mutableStateOf(s.obj("goal").str("direction")=="fromNas") };var days by remember { mutableStateOf(s.arr("learningDays").map { it.jsonPrimitive.int }) }
    PageList { Text("Objectif et connaissances",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(label,{label=it},label={Text("Nom de l’objectif")});Row { Text("Depuis An Nâs",Modifier.weight(1f));Switch(fromNas,{fromNas=it}) }
        listOf("10 dernières sourates" to listOf(VerseRange(q.surahs[104].range.start,6236)),"Hizb Sabbih" to listOf(q.hizbs[59]),"Juz ‘Amma" to listOf(q.juzs[29]),"Jusqu’à Ya Sîn" to listOf(VerseRange(q.surahs[35].range.start,6236)),"Moitié du Coran" to listOf(VerseRange(q.juzs[15].start,6236)),"Tout le Coran" to listOf(VerseRange(1,6236))).forEach { (name,ranges)->FilterChip(selected=selected==ranges,onClick={selected=ranges;label=name},label={Text(name)}) }
        Text("Rythme");paceLabels.forEach { (key,text)->FilterChip(selected=pace==key,onClick={pace=key},label={Text(text)}) }
        Text("Jours d’apprentissage");listOf("Dimanche","Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi").forEachIndexed { i,day -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(i in days,{enabled->days=if(enabled) days+i else days-i});Text(day) } }
        Button(onClick={vm.action { require(vm.repo.program.validGoal(selected)) { "L’objectif doit représenter au moins un hizb" };require(days.isNotEmpty());vm.repo.mutate { vm.repo.program.generate(it.with("goal" to json("label" to label,"ranges" to selected.map { r -> json("start" to r.start,"end" to r.end) },"direction" to if(fromNas) "fromNas" else "fromStart"),"pace" to JsonPrimitive(pace),"learningDays" to element(days),"onboardingDone" to JsonPrimitive(true))) } }}) { Text("Enregistrer et créer mon programme") }
        Text("Connaissances existantes");q.surahs.forEach { surah -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(surah.range.ids.all { known(s,it) },{checked -> vm.action { vm.repo.mutate { markKnowledge(it,surah.range,if(checked) "perfect" else "learning") } }});Text(surah.name) } }
    }
}
@Composable fun RevisionScreen(vm: CoranViewModel,s: JsonObject,open: (ReviewTask)->Unit) { LaunchedEffect(Unit) { vm.action { vm.repo.mutate { vm.repo.review.prepare(it) } } };val tasks=vm.repo.review.tasks(s);PageList { Text("Révision quotidienne",style=MaterialTheme.typography.headlineSmall);Row { Text("Révisions activées",Modifier.weight(1f));Switch(s.obj("reviewSettings").flag("enabled",true),{ enabled -> vm.action { vm.repo.mutate { touch(it.with("reviewSettings" to it.obj("reviewSettings").with("enabled" to JsonPrimitive(enabled)))) } } }) }
    Row { listOf(7,14,21,30).forEach { n -> FilterChip(selected=s.obj("reviewSettings").num("cycleDays",7)==n,onClick={vm.action { vm.repo.mutate { vm.repo.review.prepare(touch(it.with("reviewSettings" to it.obj("reviewSettings").with("cycleDays" to JsonPrimitive(n),"mode" to JsonPrimitive("cycle"))))) } }},label={Text("$n j")}) } }
    tasks.forEach { t -> Panel(vm.repo.quran.reference(t.range),"${t.category} · ${t.scheduledDate}",{open(t)}) };if(tasks.isEmpty()) Text("Aucune révision à effectuer aujourd’hui")
    Text("Historique");s.arr("reviewHistory").takeLast(40).reversed().forEach { v -> val e=v.jsonObject;Panel(vm.repo.quran.reference(range(e)),"${e.str("date")} · ${e.str("grade")}") }
} }
@Composable fun BookmarkScreen(vm: CoranViewModel,s: JsonObject,open: (Int)->Unit) { PageList { s.obj("bookmarks").values.map { it.jsonObject }.filter { it["deletedAt"]==null }.sortedByDescending { it.str("updatedAt") }.forEach { b -> Panel(vm.repo.quran.reference(VerseRange(b.num("verseId"),b.num("verseId"))),"Page ${b.num("page")}",{open(b.num("verseId"))}) { TextButton(onClick={vm.action { vm.repo.mutate { val now=Instant.now().toString();touch(it.with("bookmarks" to it.obj("bookmarks").with(b.num("verseId").toString() to b.with("deletedAt" to JsonPrimitive(now),"updatedAt" to JsonPrimitive(now))))) } }}) { Text("Supprimer") } } } } }
@Composable fun AccountScreen(vm: CoranViewModel,user: String?) { var email by rememberSaveable { mutableStateOf("") };var password by rememberSaveable { mutableStateOf("") };var register by rememberSaveable { mutableStateOf(false) };PageList {
    Text(if(user==null) "Retrouver mon compte" else "Compte connecté",style=MaterialTheme.typography.headlineSmall)
    if(user!=null) { Text(user);Button(onClick={vm.action { vm.repo.sync() }}) { Text("Synchroniser") };Button(onClick={vm.action { vm.repo.logout() }}) { Text("Déconnexion") } } else { OutlinedTextField(email,{email=it},label={Text("E-mail")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email));OutlinedTextField(password,{password=it},label={Text("Mot de passe")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password));Row { Checkbox(register,{register=it});Text("Créer un compte",Modifier.padding(top=12.dp)) };Button(onClick={vm.action { vm.repo.login(email,password,register) }}) { Text(if(register) "Inscription" else "Connexion") };TextButton(onClick={vm.action { vm.repo.resetPassword(email) }}) { Text("Mot de passe oublié") } }
} }
@Composable fun SettingsScreen(vm: CoranViewModel,s: JsonObject,navigate: (String)->Unit) { var key by rememberSaveable { mutableStateOf("") };var name by rememberSaveable { mutableStateOf(s.obj("profile").str("firstName")) };PageList {
    Text("Profil");OutlinedTextField(name,{name=it},label={Text("Prénom")});Button(onClick={vm.action { vm.repo.mutate { touch(it.with("profile" to it.obj("profile").with("firstName" to JsonPrimitive(name)))) } }}) { Text("Enregistrer") }
    ReminderSettings(vm,s);Text("Apparence");listOf("white" to "Blanc","classic" to "Vert","feminine" to "Rose","lilac" to "Lilas et Perle","night" to "Bleu Nuit et Or").forEach { (id,label)->FilterChip(selected=s.str("theme","white")==id,onClick={vm.action { vm.repo.mutate { touch(it.with("theme" to JsonPrimitive(id))) } }},label={Text(label)}) }
    Text("Édition du Coran");listOf("coranTest" to "Mushaf QPC","traditional" to "Mushaf traditionnel","tajweed" to "Tajwid simplifié","tajweedPages" to "Mushaf Tajwid","coran_1441" to "Coran 1441 (à télécharger)").forEach { (id,label)->FilterChip(selected=s.obj("reader").str("mushaf","coranTest")==id,onClick={vm.action { vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive(id)))) } }},label={Text(label)}) }
    Text("Projet Supabase existant");Text(SUPABASE_URL,fontSize=12.sp);OutlinedTextField(key,{key=it},label={Text("Clé publique publishable / anon")},visualTransformation=PasswordVisualTransformation());Button(onClick={vm.action { vm.repo.configure(key);key="" }}) { Text("Configurer la connexion") }
    listOf("Objectif","Marques-pages","Téléchargements","Signaler un problème").forEach { Panel(it,onClick={navigate(it)}) }
} }

@Composable fun ReaderScreen(vm: CoranViewModel,s: JsonObject,page: Int,onPage: (Int)->Unit,sessionId: String?,task: ReviewTask?) {
    val context=LocalContext.current;val q=vm.repo.quran;val source=s.obj("reader").str("mushaf","coranTest");val current by RecitationService.current.collectAsStateWithLifecycle();val isPlaying by RecitationService.playing.collectAsStateWithLifecycle()
    var selected by rememberSaveable(page,source) { mutableStateOf<Int?>(null) };var reciterId by rememberSaveable { mutableStateOf(s.obj("audioPreferences").str("reciterId","ar.shaatree")) };var repeat by rememberSaveable { mutableIntStateOf(1) };var each by rememberSaveable { mutableStateOf(false) };var french by rememberSaveable { mutableStateOf(false) };var showAudio by rememberSaveable { mutableStateOf(false) };var startInput by rememberSaveable { mutableStateOf("") };var endInput by rememberSaveable { mutableStateOf("") }
    val session=s.arr("sessions").map { it.jsonObject }.firstOrNull { it.str("id")==sessionId }
    val payload by produceState<Triple<JsonObject?,android.graphics.Bitmap?,JsonArray?>>(Triple(null,null,null),page,source) { value=withContext(Dispatchers.IO) { if(source=="coranTest") Triple(q.qcfData(context,page),null,null) else if(source=="coran_1441") madaniImage(context,page) else { val folder=if(source=="tajweedPages"||source=="tajweed") "mushaf-tajweed" else "mushaf";val bitmap=context.assets.open("$folder/page${page.toString().padStart(3,'0')}.png").use { BitmapFactory.decodeStream(it) };val filename=if(folder=="mushaf") "bounds.json" else "mushaf-tajweed-bounds.json";val regions=context.assets.open(filename).bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject[page.toString()]?.jsonArray };Triple(null,bitmap,regions) } } }
    val pageRange=if(source=="coranTest"&&payload.first!=null) { val ids=payload.first!!.arr("lines").flatMap { it.jsonObject.arr("words") }.map { val a=it.jsonArray;q.id(a[1].jsonPrimitive.int,a[2].jsonPrimitive.int) };VerseRange(ids.min(),ids.max()) } else q.sourceRange(page,source)
    LaunchedEffect(page,source,payload.first) { if(source!="coranTest"||payload.first!=null) vm.action { vm.repo.mutate { val now=Instant.now().toString();touch(it.with("lastRead" to json("page" to page,"verseId" to pageRange.start,"readAt" to now),"readPages" to element((it.arr("readPages").map { p->p.jsonPrimitive.int }+page).distinct()))) } } }
    LaunchedEffect(current?.verseId) { if(current!=null&&s.obj("reader").flag("followAudio",true)) { val next=q.sourcePage(current!!.verseId,source);if(next!=page) onPage(next) } }
    fun command(action: String) { context.startService(Intent(context,RecitationService::class.java).setAction(action)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) { TextButton(onClick={onPage(page+1)}) { Text("Suivante") };Text("$page / 604",Modifier.weight(1f),textAlign=TextAlign.Center);TextButton(onClick={onPage(page-1)}) { Text("Précédente") };TextButton(onClick={french=!french}) { Text(if(french) "Arabe" else "Français") } }
        if(source=="coran_1441"&&payload.second==null) { Column(Modifier.weight(1f).padding(20.dp)) { Text("Télécharge le Coran 1441 depuis Réglages → Téléchargements");Button(onClick={QuranDownloadWorker.enqueue(context)}) { Text("Lancer le téléchargement") } } } else if(source=="tajweed"&&!french) TajwidReader(q,pageRange,selected,{selected=it},Modifier.weight(1f)) else if(french) LazyColumn(Modifier.weight(1f).padding(16.dp)) { items(pageRange.ids) { id -> Text("${q.verse(id).surah}:${q.verse(id).ayah} · ${q.french(id)}",Modifier.padding(vertical=10.dp)) } } else AndroidView(factory={MushafView(it)},modifier=Modifier.fillMaxWidth().weight(1f),update={ v -> if(v.page!=page||v.source!=source||v.tag!=payload) { v.load(q,page,source,payload.first,payload.second,payload.third);v.tag=payload };v.selected=selected;v.playing=current?.verseId;v.bookmarks=s.obj("bookmarks").values.map { it.jsonObject }.filter { it["deletedAt"]==null }.map { it.num("verseId") }.toSet();v.difficulties=s.obj("difficultyMarkers").keys.mapNotNull { it.toIntOrNull() }.toSet();v.onVerse={selected=it};v.onPage=onPage;v.invalidate() })
        if(selected!=null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("${q.verse(selected!!).surah}:${q.verse(selected!!).ayah}",Modifier.padding(8.dp));TextButton(onClick={vm.action { vm.repo.mutate { val id=selected!!;val now=Instant.now().toString();val v=q.verse(id);val old=it.obj("bookmarks").obj(id.toString());touch(it.with("bookmarks" to it.obj("bookmarks").with(id.toString() to json("verseId" to id,"surah" to v.surah,"ayah" to v.ayah,"page" to q.page(id),"sourcePages" to old.obj("sourcePages").with(source to JsonPrimitive(page)),"createdAt" to old.str("createdAt",now),"updatedAt" to now)))) } }}) { Text("Marquer") };TextButton(onClick={startInput=selected.toString();endInput=selected.toString();showAudio=true}) { Text("Écouter") };TextButton(onClick={selected=null}) { Text("Fermer") } }
        if(session!=null||task!=null) Row(Modifier.fillMaxWidth().padding(8.dp)) { Button(onClick={vm.action { val through=selected?:minOf(pageRange.end,session?.num("end")?:task!!.range.end);vm.repo.mutate { if(session!=null) vm.repo.program.complete(it,session.str("id"),through) else vm.repo.review.grade(it,task!!,through,"perfect") } }},modifier=Modifier.weight(1f)) { Text("Valider jusqu’au verset sélectionné") };if(task!=null) TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.grade(it,task,minOf(selected?:pageRange.end,task.range.end),"rework") } }}) { Text("À retravailler") } }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Button(onClick={showAudio=!showAudio},modifier=Modifier.padding(6.dp)) { Text("Audio") };if(current!=null) { Text("Verset ${q.verse(current!!.verseId).ayah} · ${current!!.repetition}",Modifier.weight(1f));IconButton(onClick={command("TOGGLE")}) { Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,"Lecture / pause") };IconButton(onClick={command("STOP")}) { Icon(Icons.Default.Stop,"Arrêter") } } }
        if(showAudio) AlertDialog(onDismissRequest={showAudio=false},title={Text("Récitation et répétitions")},text={ Column(Modifier.verticalScroll(rememberScrollState())) { reciters.forEach { r -> FilterChip(selected=reciterId==r.id,onClick={reciterId=r.id},label={Text(r.name)}) };OutlinedTextField(startInput,{startInput=it},label={Text("Premier verset : numéro global (1–6236)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(endInput,{endInput=it},label={Text("Dernier verset : numéro global")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));Row { Checkbox(each,{each=it});Text("Chaque verset",Modifier.padding(top=12.dp)) };Row { listOf(1,3,5,10,0).forEach { n -> FilterChip(selected=repeat==n,onClick={repeat=n},label={Text(if(n==0) "∞" else n.toString())}) } } } },confirmButton={TextButton(onClick={val start=startInput.toIntOrNull()?:session?.num("start")?:task?.range?.start?:pageRange.start;val end=endInput.toIntOrNull()?:session?.num("end")?:task?.range?.end?:pageRange.end;if(start in 1..6236&&end in start..6236) { vm.action { vm.repo.mutate { touch(it.with("audioPreferences" to json("reciterId" to reciterId))) } };context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_RANGE").putExtra("start",start).putExtra("end",end).putExtra("reciter",reciterId).putExtra("count",repeat).putExtra("each",each));showAudio=false } else vm.repo.feedback("Plage de versets invalide")}) { Text("Lire") }})
    }
}
