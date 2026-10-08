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
import androidx.compose.runtime.saveable.listSaver
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CoranApp(vm: CoranViewModel=viewModel()) {
    val s by vm.repo.state.collectAsStateWithLifecycle();val notice by vm.repo.notice.collectAsStateWithLifecycle();val user by vm.repo.user.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("Accueil") };var route by rememberSaveable { mutableStateOf<String?>(null) }
    val recovery by vm.repo.passwordRecovery.collectAsStateWithLifecycle()
    LaunchedEffect(recovery,user) { if(recovery&&user!=null) route="Compte" }
    var page by rememberSaveable { mutableIntStateOf(1) };var sessionId by rememberSaveable { mutableStateOf<String?>(null) };var reviewTask by rememberSaveable(stateSaver=listSaver<ReviewTask?,String>(save={ t -> t?.let { listOf(it.id,it.range.start.toString(),it.range.end.toString(),it.category,it.scheduledDate,it.consolidationOffset?.toString().orEmpty()) }?:emptyList() },restore={ if(it.isEmpty()) null else ReviewTask(it[0],VerseRange(it[1].toInt(),it[2].toInt()),it[3],it[4],it[5].toIntOrNull()) })) { mutableStateOf<ReviewTask?>(null) }
    NativeAppTheme(s) {
        BackHandler(route!=null) { route=null;sessionId=null;reviewTask=null }
        Scaffold(topBar={ TopAppBar(title={ Text(if(route=="reader") "Coran Mémoire" else route?:tab) },navigationIcon={ if(route!=null) IconButton(onClick={route=null}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour") } },actions={ IconButton(onClick={route="Réglages"}) { Icon(Icons.Default.Settings,"Réglages") };IconButton(onClick={route="Compte"}) { Icon(Icons.Default.AccountCircle,"Compte") } }) },bottomBar={ if(route==null) NavigationBar { listOf("Accueil" to Icons.Default.Home,"Coran" to Icons.Default.MenuBook,"Programme" to Icons.Default.DateRange,"Progrès" to Icons.Default.Insights,"Amis" to Icons.Default.People).forEach { (label,icon)->NavigationBarItem(colors=NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary,indicatorColor=MaterialTheme.colorScheme.primaryContainer,unselectedIconColor=MaterialTheme.colorScheme.onSurfaceVariant,unselectedTextColor=MaterialTheme.colorScheme.onSurfaceVariant),selected=tab==label,onClick={tab=label},icon={Icon(icon,label)},label={Text(label,fontSize=10.sp)}) } } }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if(notice.isNotEmpty()) Text(notice,Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp),fontSize=12.sp)
                val open: (Int)->Unit = { id -> val source=s.obj("reader").str("mushaf","coranTest");page=vm.repo.quran.sourcePage(id,source);route="reader" }
                when(route) {
                    "reader" -> ReaderScreen(vm,s,page,{page=it.coerceIn(1,604)},sessionId,reviewTask)
                    "Compte" -> AccountScreen(vm,user)
                    "Administration" -> AdminScreen(vm)
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
                        "Accueil" -> HomeScreen(vm,s,{route=it}, { sessionId=null;reviewTask=null;open(homeReadingVerse(s)) })
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
@Composable fun Panel(title: String,subtitle: String="",onClick: (()->Unit)?=null,content: @Composable ColumnScope.()->Unit={}) { Card(modifier=Modifier.fillMaxWidth().then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(title,style=MaterialTheme.typography.titleMedium);if(subtitle.isNotEmpty()) Text(subtitle,style=MaterialTheme.typography.bodyMedium);content() } } }
@Composable fun HomeScreen(vm: CoranViewModel,s: JsonObject,navigate: (String)->Unit,read: ()->Unit) { val progress=vm.repo.program.progress(s);PageList {
    HomeGreeting(s)
    Panel("Mon objectif",s.obj("goal").str("label"),{navigate("Objectif")}) { LinearProgressIndicator(drawStopIndicator={},progress={progress.second},modifier=Modifier.fillMaxWidth());Text("${(progress.second*100).toInt()} % appris") }
    ContinueReadingCard(vm,s,read)
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
@Composable fun GoalScreen(vm: CoranViewModel,s: JsonObject) { val q=vm.repo.quran;var selected by rememberSaveable(stateSaver=listSaver<List<VerseRange>,Int>(save={it.flatMap { r -> listOf(r.start,r.end) }},restore={it.chunked(2).map { pair -> VerseRange(pair[0],pair[1]) }})) { mutableStateOf(s.obj("goal").arr("ranges").map { range(it.jsonObject) }) };var label by rememberSaveable { mutableStateOf(s.obj("goal").str("label")) };var pace by rememberSaveable { mutableStateOf(s.str("pace","verse3")) };var fromNas by rememberSaveable { mutableStateOf(s.obj("goal").str("direction")=="fromNas") };var days by rememberSaveable(stateSaver=listSaver<List<Int>,Int>(save={it},restore={it})) { mutableStateOf(s.arr("learningDays").map { it.jsonPrimitive.int }) }
    PageList { Text("Objectif et connaissances",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(label,{label=it},label={Text("Nom de l’objectif")});Row { Text("Depuis An Nâs",Modifier.weight(1f));Switch(fromNas,{fromNas=it}) }
        listOf("10 dernières sourates" to listOf(VerseRange(q.surahs[104].range.start,6236)),"Hizb Sabbih" to listOf(q.hizbs[59]),"Juz ‘Amma" to listOf(q.juzs[29]),"Jusqu’à Ya Sîn" to listOf(VerseRange(q.surahs[35].range.start,6236)),"Moitié du Coran" to listOf(VerseRange(q.juzs[15].start,6236)),"Tout le Coran" to listOf(VerseRange(1,6236))).forEach { (name,ranges)->FilterChip(selected=selected==ranges,onClick={selected=ranges;label=name},label={Text(name)}) }
        Text("Sélection personnalisée");RangePicker(q,"Ajouter à mon objectif") { r -> selected=(selected+r).distinct() };selected.forEach { r -> Row { Text(q.reference(r),Modifier.weight(1f));TextButton(onClick={selected=selected-r}) { Text("Retirer") } } };Text("Rythme");paceLabels.forEach { (key,text)->FilterChip(selected=pace==key,onClick={pace=key},label={Text(text)}) }
        Text("Jours d’apprentissage");listOf("Dimanche","Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi").forEachIndexed { i,day -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(i in days,{enabled->days=if(enabled) days+i else days-i});Text(day) } }
        Button(onClick={vm.action { require(vm.repo.program.validGoal(selected)) { "L’objectif doit représenter au moins un hizb" };require(days.isNotEmpty());vm.repo.mutate { vm.repo.program.generate(it.with("goal" to json("label" to label,"ranges" to selected.map { r -> json("start" to r.start,"end" to r.end) },"direction" to if(fromNas) "fromNas" else "fromStart"),"pace" to JsonPrimitive(pace),"learningDays" to element(days),"onboardingDone" to JsonPrimitive(true))) } }}) { Text("Enregistrer et créer mon programme") }
        KnowledgePicker(vm)
    }
}
@Composable fun ReviewRhythmPicker(settings: JsonObject,onCycle: (Int)->Unit,onQuantity: (String)->Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val quantities=listOf("nisf" to "1 Nisf / jour","hizb" to "1 Hizb / jour","juz" to "1 Juz / jour","juz2" to "2 Juz / jour")
    Column {
        Text(if(settings.str("mode")=="quantity") quantities.firstOrNull { it.first==settings.str("dailyQuantity","hizb") }?.second?:"1 Hizb / jour" else "Cycle de ${settings.num("cycleDays",7)} jours")
        TextButton(onClick={expanded=!expanded}) { Text("Modifier le rythme") }
        if(expanded) {
            Row { listOf(7,14,21,30).forEach { n -> FilterChip(selected=settings.str("mode")!="quantity"&&settings.num("cycleDays",7)==n,onClick={onCycle(n);expanded=false},label={Text("$n j")}) } }
            quantities.forEach { (quantity,label) -> FilterChip(selected=settings.str("mode")=="quantity"&&settings.str("dailyQuantity","hizb")==quantity,onClick={onQuantity(quantity);expanded=false},label={Text(label)}) }
        }
    }
}
@Composable fun RevisionScreen(vm: CoranViewModel,s: JsonObject,open: (ReviewTask)->Unit) { LaunchedEffect(Unit) { vm.action { vm.repo.mutate { vm.repo.review.prepare(it) } } };val tasks=vm.repo.review.tasks(s);PageList { Text("Révision quotidienne",style=MaterialTheme.typography.headlineSmall);Row { Text("Révisions activées",Modifier.weight(1f));Switch(s.obj("reviewSettings").flag("enabled",true),{ enabled -> vm.action { vm.repo.mutate { vm.repo.review.prepare(vm.repo.review.setEnabled(it,enabled)) } } }) }
    ReviewRhythmPicker(s.obj("reviewSettings"),{ n -> vm.action { vm.repo.mutate { vm.repo.review.prepare(vm.repo.review.setCycle(it,n)) } } },{ quantity -> vm.action { vm.repo.mutate { vm.repo.review.prepare(vm.repo.review.setQuantity(it,quantity)) } } })
    val cycle=s.obj("reviewCycle");val corpus=cycle.arr("corpus").map { it.jsonPrimitive.int }.toSet();val completed=cycle.arr("completed").map { it.jsonPrimitive.int }.count { it in corpus }
    if(cycle.isNotEmpty()) { Text("Mon cycle de révision");Text("${cycle.num("lengthDays",7)} jours · $completed / ${corpus.size} versets réellement révisés");LinearProgressIndicator(progress={if(corpus.isEmpty()) 0f else completed.toFloat()/corpus.size},modifier=Modifier.fillMaxWidth());Text("Une journée manquée reste à faire et peut décaler la fin du cycle.") }
    val rows=vm.repo.review.consolidations(s)
    if(rows.isNotEmpty()) { Text("Nouveaux versets à consolider");Text("Consolidations J+1, J+3 et J+7. Les dates restent liées à l’apprentissage.") }
    rows.forEach { row -> val pending=row.steps.first { it.completed==null };Panel(vm.repo.quran.reference(row.range),"Appris le ${row.learnedAt}\n"+row.steps.joinToString(" · ") { "J+${it.offset} : "+(it.completed?.let { date -> "validé le $date" }?:it.due) },{open(ReviewTask("consolidation-${row.range.start}-${row.range.end}",row.range,"recent",pending.due,pending.offset))}) }
    tasks.forEach { t -> Panel(vm.repo.quran.reference(t.range),"${t.category} · ${t.scheduledDate}",{open(t)}) };if(tasks.isEmpty()) Text("Aucune révision à effectuer aujourd’hui")
    Text("Historique");s.arr("reviewHistory").takeLast(40).reversed().forEach { v -> val e=v.jsonObject;Panel(vm.repo.quran.reference(range(e)),"${e.str("date")} · ${e.str("grade")}") }
} }
@Composable fun BookmarkScreen(vm: CoranViewModel,s: JsonObject,open: (Int)->Unit) {
    val bookmarks=remember(vm) { Bookmarks(vm.repo.quran) }
    PageList {
        val visible=bookmarks.visible(s)
        if(visible.isEmpty()) Text("Aucun marque-page enregistré")
        visible.forEach { b ->
            val id=b.num("verseId")
            Panel(vm.repo.quran.reference(VerseRange(id,id)),"Page ${b.num("page")}",{
                val source=s.obj("reader").str("mushaf","coranTest")
                vm.action { vm.repo.mutate { bookmarks.use(it,id,vm.repo.quran.sourcePage(id,source)) } }
                open(id)
            }) {
                TextButton(onClick={vm.action { vm.repo.mutate { bookmarks.delete(it,id) } }}) { Text("Supprimer") }
            }
        }
    }
}
@Composable fun AccountScreen(vm: CoranViewModel,user: String?) {
    var email by rememberSaveable { mutableStateOf("") };var password by remember { mutableStateOf("") };var newPassword by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) };var busy by remember { mutableStateOf(false) };val recovery by vm.repo.passwordRecovery.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
    fun run(block: suspend ()->Unit) { if(busy) return;busy=true;scope.launch { try { withContext(Dispatchers.IO) { block() } } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e;vm.repo.feedback(e.message?:"Opération impossible") } finally { busy=false } } }
    PageList {
        Text(if(user==null) "Retrouver mon compte" else "Compte connecté",style=MaterialTheme.typography.headlineSmall)
        if(user!=null) {
            Text(user)
            if(recovery) {
                Text("Choisir mon mot de passe",style=MaterialTheme.typography.titleMedium);Text("Utilise au moins 8 caractères. Ton mot de passe reste privé.")
                OutlinedTextField(newPassword,{newPassword=it},label={Text("Nouveau mot de passe")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
                Button(enabled=!busy&&newPassword.length>=8,onClick={run { vm.repo.changePassword(newPassword);newPassword="" }}) { Text("Enregistrer mon mot de passe") }
            }
            Button(enabled=!busy,onClick={run { vm.repo.sync() }}) { Text("Synchroniser") };Button(enabled=!busy,onClick={run { vm.repo.logout() }}) { Text("Déconnexion") }
        } else {
            OutlinedTextField(email,{email=it},label={Text("E-mail")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email));OutlinedTextField(password,{password=it},label={Text("Mot de passe")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
            Row { Checkbox(register,{register=it});Text("Créer un compte",Modifier.padding(top=12.dp)) }
            Button(enabled=!busy&&email.isNotBlank()&&password.length>=(if(register) 6 else 1),onClick={run { vm.repo.login(email,password,register);password="" }}) { Text(if(register) "Inscription" else "Connexion") }
            TextButton(enabled=!busy&&email.contains('@'),onClick={run { vm.repo.resendSignupConfirmation(email) }}) { Text("Renvoyer le courriel de confirmation") }
            TextButton(enabled=!busy&&email.contains('@'),onClick={run { vm.repo.resetPassword(email) }}) { Text("Mot de passe oublié") }
        }
        if(busy) CircularProgressIndicator(Modifier.size(24.dp))
    }
}
@Composable fun SettingsScreen(vm: CoranViewModel,s: JsonObject,navigate: (String)->Unit) { var key by rememberSaveable { mutableStateOf("") };var name by rememberSaveable { mutableStateOf(s.obj("profile").str("firstName")) };PageList {
    AdminEntry(vm,navigate)
    Text("Profil");OutlinedTextField(name,{name=it},label={Text("Prénom")});Button(onClick={vm.action { vm.repo.mutate { touch(it.with("profile" to it.obj("profile").with("firstName" to JsonPrimitive(name)))) } }}) { Text("Enregistrer") }
    ReminderSettings(vm,s);Text("Apparence");listOf("white" to "Blanc","classic" to "Vert","feminine" to "Rose","lilac" to "Lilas et Perle","night" to "Bleu Nuit et Or").forEach { (id,label)->FilterChip(selected=s.str("theme","white")==id,onClick={vm.action { vm.repo.mutate { touch(it.with("theme" to JsonPrimitive(id))) } }},label={Text(label)}) }
    AppearanceOptions(vm,s)
    Text("Édition du Coran");listOf("coranTest" to "Mushaf QPC","traditional" to "Mushaf traditionnel","tajweed" to "Tajwid simplifié","tajweedPages" to "Mushaf Tajwid","coran_1441" to "Coran 1441 (à télécharger)").forEach { (id,label)->FilterChip(selected=s.obj("reader").str("mushaf","coranTest")==id,onClick={vm.action { vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("mushaf" to JsonPrimitive(id)))) } }},label={Text(label)}) }
    Text("Projet Supabase existant");Text(SUPABASE_URL,fontSize=12.sp);OutlinedTextField(key,{key=it},label={Text("Clé publique publishable / anon")},visualTransformation=PasswordVisualTransformation());Button(onClick={vm.action { vm.repo.configure(key);key="" }}) { Text("Configurer la connexion") }
    listOf("Objectif","Marques-pages","Téléchargements","Signaler un problème").forEach { Panel(it,onClick={navigate(it)}) }
} }

@Composable fun ReaderScreen(vm: CoranViewModel,s: JsonObject,page: Int,onPage: (Int)->Unit,sessionId: String?,task: ReviewTask?) {
    val context=LocalContext.current;val q=vm.repo.quran;val source=s.obj("reader").str("mushaf","coranTest");val current by RecitationService.current.collectAsStateWithLifecycle();val isPlaying by RecitationService.playing.collectAsStateWithLifecycle();val passageProgress by RecitationService.passageProgress.collectAsStateWithLifecycle();val activePreferences by RecitationService.activePreferences.collectAsStateWithLifecycle()
    var selected by rememberSaveable(page,source) { mutableStateOf<Int?>(null) };var reciterId by rememberSaveable { mutableStateOf(s.obj("audioPreferences").str("reciterId","ar.shaatree")) };var french by rememberSaveable { mutableStateOf(false) };var showAudio by rememberSaveable { mutableStateOf(false) };var startInput by rememberSaveable { mutableStateOf("") };var endInput by rememberSaveable { mutableStateOf("") }
    val session=s.arr("sessions").map { it.jsonObject }.firstOrNull { it.str("id")==sessionId }
    val payload by produceState<Triple<JsonObject?,android.graphics.Bitmap?,JsonArray?>>(Triple(null,null,null),page,source) { value=withContext(Dispatchers.IO) { if(source=="coranTest") Triple(q.qcfData(context,page),null,null) else if(source=="coran_1441") madaniImage(context,page) else { val folder=if(source=="tajweedPages"||source=="tajweed") "mushaf-tajweed" else "mushaf";val bitmap=context.assets.open("$folder/page${page.toString().padStart(3,'0')}.png").use { BitmapFactory.decodeStream(it) };val filename=if(folder=="mushaf") "bounds.json" else "mushaf-tajweed-bounds.json";val regions=context.assets.open(filename).bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject[page.toString()]?.jsonArray };Triple(null,bitmap,regions) } } }
    val pageRange=if(source=="coranTest"&&payload.first!=null) { val ids=payload.first!!.arr("lines").flatMap { it.jsonObject.arr("words") }.map { val a=it.jsonArray;q.id(a[1].jsonPrimitive.int,a[2].jsonPrimitive.int) };VerseRange(ids.min(),ids.max()) } else q.sourceRange(page,source)
    LaunchedEffect(page,source,payload.first) { if(source!="coranTest"||payload.first!=null) vm.action { vm.repo.mutate { val now=Instant.now().toString();touch(it.with("lastRead" to json("page" to page,"verseId" to pageRange.start,"readAt" to now),"readPages" to element((it.arr("readPages").map { p->p.jsonPrimitive.int }+page).distinct()))) } } }
    LaunchedEffect(current?.verseId) { if(current!=null&&s.obj("reader").flag("followAudio",true)) { val next=q.sourcePage(current!!.verseId,source);if(next!=page) onPage(next) } }
    fun command(action: String) { context.startService(Intent(context,RecitationService::class.java).setAction(action)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) { TextButton(onClick={onPage(page+1)}) { Text("Suivante") };Text("$page / 604",Modifier.weight(1f),textAlign=TextAlign.Center);TextButton(onClick={onPage(page-1)}) { Text("Précédente") };TextButton(onClick={french=!french}) { Text(if(french) "Arabe" else "Français") } }
        if(source=="coran_1441"&&payload.second==null) { Column(Modifier.weight(1f).padding(20.dp)) { Text("Télécharge le Coran 1441 depuis Réglages → Téléchargements");Button(onClick={QuranDownloadWorker.enqueue(context)}) { Text("Lancer le téléchargement") } } } else if(source=="tajweed"&&!french) TajwidReader(q,pageRange,selected,{selected=it},Modifier.weight(1f)) else if(french) LazyColumn(Modifier.weight(1f).padding(16.dp)) { items(pageRange.ids) { id -> Column(Modifier.fillMaxWidth().clickable { selected=id }.background(if(selected==id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(10.dp)) { Text("${q.verse(id).surah}:${q.verse(id).ayah} · ${q.french(id)}",lineHeight=25.sp);val notes=q.frenchNotes(id);if(notes.isNotBlank()) Text(notes,Modifier.padding(top=5.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) } } } else AndroidView(factory={MushafView(it)},modifier=Modifier.fillMaxWidth().weight(1f),update={ v -> if(v.page!=page||v.source!=source||v.tag!=payload) { v.load(q,page,source,payload.first,payload.second,payload.third);v.tag=payload };v.paper=paperColor(s.obj("reader").str("paper"));v.selected=selected;v.playing=current?.verseId;v.bookmarks=s.obj("bookmarks").values.map { it.jsonObject }.filter { it["deletedAt"]==null }.map { it.num("verseId") }.toSet();v.difficulties=s.obj("difficultyMarkers").keys.mapNotNull { it.toIntOrNull() }.toSet();v.onVerse={selected=it};v.onPage=onPage;v.invalidate() })
        if(selected!=null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("${q.verse(selected!!).surah}:${q.verse(selected!!).ayah}",Modifier.padding(8.dp));TextButton(onClick={vm.action { vm.repo.mutate { Bookmarks(q).save(it,selected!!,source,page) } }}) { Text("Marquer") };TextButton(onClick={startInput=selected.toString();endInput=selected.toString();showAudio=true}) { Text("Écouter") };TextButton(onClick={selected=null}) { Text("Fermer") } }
        if(task?.consolidationOffset!=null) {
            val pending=vm.repo.review.consolidations(s).any { row -> row.range.ids.any { it in task.range.ids }&&row.steps.firstOrNull { it.completed==null }?.offset==task.consolidationOffset }
            Button(enabled=pending,onClick={vm.action { command("STOP");vm.repo.mutate { vm.repo.review.completeConsolidation(it,task.range,targetOffset=task.consolidationOffset) } }},modifier=Modifier.fillMaxWidth().padding(8.dp)) { Text(if(pending) "Valider la consolidation · J+${task.consolidationOffset}" else "Consolidation validée") }
        } else if(session!=null||task!=null) Row(Modifier.fillMaxWidth().padding(8.dp)) { Button(onClick={vm.action { val through=selected?:q.studyEndpoint(page,if(session!=null) range(session) else task!!.range,source);vm.repo.mutate { if(session!=null) vm.repo.program.complete(it,session.str("id"),through) else vm.repo.review.grade(it,task!!,through,"perfect") } }},modifier=Modifier.weight(1f)) { Text("Valider jusqu’au verset sélectionné") };if(task!=null) TextButton(onClick={vm.action { vm.repo.mutate { vm.repo.review.grade(it,task,selected?:q.studyEndpoint(page,task.range,source),"rework") } }}) { Text("À retravailler") } }
        ReaderAudioControls(q,current,isPlaying,passageProgress,activePreferences,{showAudio=true},::command)
        if(showAudio) ReaderAudioDialog(vm,if(startInput.isBlank()&&current!=null) RecitationService.activeRange.value?:pageRange else VerseRange(startInput.toIntOrNull()?:session?.num("start")?:task?.range?.start?:pageRange.start,endInput.toIntOrNull()?:session?.num("end")?:task?.range?.end?:pageRange.end),pageRange,reciterId,{reciterId=it},{showAudio=false;startInput="";endInput=""})
    }
}
