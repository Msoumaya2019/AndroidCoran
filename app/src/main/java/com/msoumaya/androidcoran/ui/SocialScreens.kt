package com.msoumaya.androidcoran.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.time.*

@Composable fun FriendsScreen(vm: CoranViewModel) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val service=remember(vm) { SocialService(vm.repo) }
    var snapshot by remember(owner) { mutableStateOf<SocialSnapshot?>(null) }
    var roomId by rememberSaveable(owner) { mutableStateOf<String?>(null) }
    var group by rememberSaveable(owner) { mutableStateOf(false) }
    var other by rememberSaveable(owner) { mutableStateOf<String?>(null) }
    var title by rememberSaveable(owner) { mutableStateOf("") }
    var settings by rememberSaveable(owner) { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var groupName by rememberSaveable { mutableStateOf("") }
    fun refresh() { vm.action { snapshot=service.snapshot() } }
    fun open(id: String,isGroup: Boolean,name: String,user: String?=null) { roomId=id;group=isGroup;title=name;other=user }
    LaunchedEffect(owner) { if(owner!=null) refresh() }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(owner,lifecycle) {
        fun online(active: Boolean) { if(owner!=null) vm.action { if(vm.repo.user.value==owner) vm.repo.rpc("set_social_online",json("p_active" to active)) } }
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_START) online(true) else if(event==Lifecycle.Event.ON_STOP) online(false) }
        lifecycle.addObserver(observer);if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) online(true)
        onDispose { lifecycle.removeObserver(observer);online(false) }
    }
    if(owner==null) { PageList { Text("Connecte-toi pour retrouver tes amis et conversations") };return }
    if(roomId!=null) { ConversationScreen(vm,service,ChatRoom(roomId!!,group,other,title),snapshot) { roomId=null;refresh() };return }
    if(settings&&snapshot!=null) { SocialProfileScreen(vm,snapshot!!.profile) { settings=false;refresh() };return }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Row { TextButton(onClick={refresh()}) { Text("Actualiser") };TextButton(onClick={settings=true}) { Text("Mon profil et confidentialité") } }
            Text("Code d’invitation : ${snapshot?.profile?.str("invite_code")?:"…"}")
            OutlinedTextField(code,{code=it},label={Text("Code de mon ami")})
            Button(onClick={vm.action { vm.repo.rpc("request_friend",json("p_code" to code.trim()));code="";refresh() }},enabled=code.isNotBlank()) { Text("Ajouter un ami") }
            snapshot?.suspension?.let { Text("Accès social suspendu : ${it.str("reason")}",color=MaterialTheme.colorScheme.error) }
            TextButton(onClick={vm.action { val id=vm.repo.rpc("open_admin_contact").jsonPrimitive.content;open(id,true,"Contact administrateur") }}) { Text("Contacter un administrateur") }
        }
        items(snapshot?.links?:emptyList(),key={it.str("id")}) { link ->
            val otherId=if(link.str("requester_id")==owner) link.str("recipient_id") else link.str("requester_id")
            val name=snapshot?.profiles?.firstOrNull { it.str("id")==otherId }?.str("display_name")?:"Ami"
            val inbox=snapshot?.inbox?.firstOrNull { it.str("link_id")==link.str("id") }
            Panel(name,if(inbox!=null) "${inbox.str("body")} · ${inbox.num("unread_count")} non lu(s)" else link.str("status"),if(link.str("status")=="accepted") ({open(link.str("id"),false,name,otherId)}) else null) {
                if(inbox?.flag("is_online")==true) Text("En ligne")
                if(link.str("status")=="pending"&&link.str("recipient_id")==owner) Row {
                    TextButton(onClick={vm.action { vm.repo.rpc("accept_friend",json("p_link" to link.str("id")));refresh() }}) { Text("Accepter") }
                    TextButton(onClick={vm.action { vm.repo.rpc("decline_friend",json("p_link" to link.str("id")));refresh() }}) { Text("Refuser") }
                }
                if(link.str("status")=="blocked"&&link.str("blocked_by")==owner) TextButton(onClick={vm.action { vm.repo.rpc("unblock_friend",json("p_other" to otherId));refresh() }}) { Text("Débloquer") }
            }
        }
        item { Text("Groupes",style=MaterialTheme.typography.titleLarge);OutlinedTextField(groupName,{groupName=it},label={Text("Nom du groupe")});Button(enabled=groupName.isNotBlank(),onClick={vm.action { vm.repo.rpc("create_friend_group",json("p_name" to groupName.trim()));groupName="";refresh() }}) { Text("Créer un groupe") } }
        items(snapshot?.groups?:emptyList(),key={it.str("id")}) { row ->
            val member=snapshot?.members?.firstOrNull { it.str("group_id")==row.str("id") }
            val accepted=member?.str("accepted_at")?.isNotBlank()==true||row.str("owner_id")==owner
            Panel(row.str("name"),if(accepted) "Conversation du groupe" else "Invitation",if(accepted) ({open(row.str("id"),true,row.str("name"))}) else null) {
                if(!accepted) Row {
                    TextButton(onClick={vm.action { vm.repo.rpc("accept_group_invite",json("p_group" to row.str("id")));refresh() }}) { Text("Rejoindre") }
                    TextButton(onClick={vm.action { vm.repo.rpc("decline_group_invite",json("p_group" to row.str("id")));refresh() }}) { Text("Refuser") }
                }
            }
        }
    }
}

@Composable private fun ConversationScreen(vm: CoranViewModel,service: SocialService,room: ChatRoom,snapshot: SocialSnapshot?,back: ()->Unit) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var messages by remember(room.id,owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var cursor by remember(room.id,owner) { mutableStateOf<String?>(null) }
    var hasMore by remember(room.id,owner) { mutableStateOf(false) }
    var body by rememberSaveable(room.id,owner) { mutableStateOf("") }
    var kind by rememberSaveable(room.id,owner) { mutableStateOf("text") }
    var details by rememberSaveable(room.id) { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) };var reason by rememberSaveable { mutableStateOf("") }
    var otherRead by remember { mutableStateOf("") }
    val context=LocalContext.current
    suspend fun load(older: Boolean=false) {
        val result=service.messages(room,if(older) cursor else null)
        messages=if(older) (result.messages+messages).distinctBy { it.str("id") } else (messages.filter { it.str("created_at")<(result.cursor?:"") }+result.messages).distinctBy { it.str("id") }.sortedBy { it.str("created_at") }
        if(older||cursor==null) { cursor=result.cursor;hasMore=result.hasMore }
        service.markRead(room)
        if(!room.group&&room.otherId!=null) otherRead=vm.repo.query("friend_message_reads",eq=mapOf("link_id" to room.id,"user_id" to room.otherId),orderBy="last_read_at").firstOrNull()?.str("last_read_at")?:""
    }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(room.id,owner) { if(owner!=null) runCatching { load() }.onFailure { if(it is CancellationException) throw it;vm.repo.feedback(it.message?:"Messages indisponibles") } }
    LaunchedEffect(room.id,owner,lifecycle) {
        if(owner!=null) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try { vm.repo.observeTable("friend_messages").collect { load() } }
            catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback("Actualisation en direct indisponible. Utilise Actualiser.") }
        }
    }
    if(details) { ConversationDetails(vm,service,room,snapshot,{details=false},back);return }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row { TextButton(onClick=back) { Text("Retour") };TextButton(onClick={details=true}) { Text(room.title) };TextButton(onClick={vm.action { load() }}) { Text("Actualiser") } }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(hasMore) item { TextButton(onClick={vm.action { load(true) }}) { Text("Messages plus anciens") } }
            items(messages,key={it.str("id")}) { row ->
                val deleted=row.str("deleted_at").isNotBlank()
                Panel(if(row.str("sender_id")==owner) "Moi" else snapshot?.profiles?.firstOrNull { it.str("id")==row.str("sender_id") }?.str("display_name")?:"Membre",if(deleted) "Message supprimé" else row.str("body")) {
                    Text(row.str("created_at"),style=MaterialTheme.typography.labelSmall)
                    if(!deleted&&row.str("kind")=="recitation"&&row.obj("recitation").str("storage_path").isNotBlank()) TextButton(onClick={vm.action { val url=vm.repo.signedRecitation(row.obj("recitation").str("storage_path"));context.startService(Intent(context,RecitationService::class.java).setAction("PLAY_URL").putExtra("url",url)) }}) { Text("Écouter la récitation") }
                    if(row.str("sender_id")==owner&&otherRead.isNotBlank()&&row.str("created_at")<=otherRead) Text("Lu")
                    if(!deleted) Row {
                        TextButton(onClick={vm.action { service.hide(row.str("id"));messages=messages.filter { it.str("id")!=row.str("id") } }}) { Text("Masquer") }
                        if(row.str("sender_id")==owner) TextButton(onClick={vm.action { vm.repo.rpc("delete_friend_message",json("p_message" to row.str("id")));load() }}) { Text("Supprimer") }
                        TextButton(onClick={report=row.str("id")}) { Text("Signaler") }
                    }
                }
            }
        }
        Row { listOf("text" to "Message","encouragement" to "Encouragement","progress" to "Progrès").forEach { (id,label)->FilterChip(selected=kind==id,onClick={kind=id},label={Text(label)}) } }
        OutlinedTextField(body,{if(it.length<=2000) body=it},label={Text("Message")},modifier=Modifier.fillMaxWidth())
        Button(enabled=body.isNotBlank(),onClick={vm.action { service.send(room,body,kind);body="";load() }}) { Text("Envoyer") }
    }
    if(report!=null) AlertDialog(onDismissRequest={report=null},title={Text("Signaler un message")},text={OutlinedTextField(reason,{reason=it},label={Text("Motif")})},confirmButton={TextButton(enabled=reason.isNotBlank(),onClick={val id=report!!;vm.action { vm.repo.rpc("report_friend_message",json("p_message" to id,"p_reason" to reason.trim()));report=null;reason="";vm.repo.feedback("Signalement envoyé") }}) { Text("Envoyer") }},dismissButton={TextButton(onClick={report=null}) { Text("Annuler") }})
}

@Composable private fun ConversationDetails(vm: CoranViewModel,service: SocialService,room: ChatRoom,snapshot: SocialSnapshot?,back: ()->Unit,close: ()->Unit) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var goals by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var appointments by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var members by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var overview by remember { mutableStateOf<JsonObject?>(null) }
    var target by rememberSaveable { mutableStateOf("3") };var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) };var time by rememberSaveable { mutableStateOf("19:00") }
    var confirm by remember { mutableStateOf<Pair<String,JsonObject>?>(null) }
    fun refresh() { vm.action { if(room.group) members=service.members(room) else { goals=service.goals(room);appointments=service.appointments(room);if(room.otherId!=null) overview=rpcObject(vm.repo.rpc("friend_overview",json("p_other" to room.otherId))) } } }
    LaunchedEffect(room.id) { refresh() }
    PageList {
        TextButton(onClick=back) { Text("Retour à la conversation") }
        Text(room.title,style=MaterialTheme.typography.headlineSmall)
        if(!room.group) {
            overview?.let { Panel("Progression partagée",it.str("goal_label")) { Text("${it.num("weekly_verses")} versets · ${it.num("weekly_sessions")} séances cette semaine");Text("Objectif : ${it["goal_percent"]?:0} % · Coran : ${it["quran_percent"]?:0} %") } }
            OutlinedTextField(target,{target=it},label={Text("Séances cette semaine (1–14)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            Button(onClick={vm.action { val count=target.toInt();require(count in 1..14);val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong());service.proposeGoal(room,monday.toString(),count);refresh() }}) { Text("Proposer un objectif partagé") }
            goals.forEach { goal -> Panel("Semaine du ${goal.str("week_start")}","${goal.num("target_sessions")} séances · ${if(goal.str("accepted_at").isBlank()) "En attente" else "Accepté"}") { if(goal.str("accepted_at").isBlank()&&goal.str("proposed_by")!=owner) TextButton(onClick={vm.action { vm.repo.rpc("accept_shared_goal",json("p_goal" to goal.str("id")));refresh() }}) { Text("Accepter") } } }
            Text("Rendez-vous de révision");CalendarDateField(date,{date=it});ClockTimeField(time,{time=it})
            Button(onClick={vm.action { val starts=LocalDateTime.of(LocalDate.parse(date),LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant();service.proposeAppointment(room,starts.toString());refresh() }}) { Text("Proposer un rendez-vous") }
            appointments.forEach { row -> Panel(Instant.parse(row.str("starts_at")).atZone(ZoneId.systemDefault()).toLocalDateTime().toString(),if(row.str("accepted_at").isBlank()) "En attente" else "Accepté") {
                if(row.str("accepted_at").isBlank()&&row.str("proposed_by")!=owner) TextButton(onClick={vm.action { vm.repo.rpc("accept_review_appointment",json("p_appointment" to row.str("id")));refresh() }}) { Text("Accepter") }
                TextButton(onClick={confirm="cancel_review_appointment" to json("p_appointment" to row.str("id"))}) { Text("Annuler le rendez-vous") }
            } }
            TextButton(onClick={confirm="remove_friend" to json("p_link" to room.id)}) { Text("Retirer cet ami") }
            TextButton(onClick={confirm="block_friend" to json("p_other" to room.otherId)}) { Text("Bloquer cet utilisateur") }
        } else {
            val mine=members.firstOrNull { it.str("user_id")==owner };val canModerate=mine?.str("role") in listOf("owner","moderator")
            Text("Membres du groupe")
            members.forEach { member -> Panel(member.obj("profile").str("display_name"),"${member.str("role")} · ${if(member.str("accepted_at").isBlank()) "invité" else "membre"}") {
                if(canModerate&&member.str("user_id")!=owner&&member.str("role")!="owner") {
                    TextButton(onClick={confirm="remove_group_member" to json("p_group" to room.id,"p_member" to member.str("user_id"))}) { Text("Retirer") }
                    if(mine?.str("role")=="owner") TextButton(onClick={vm.action { vm.repo.rpc("set_group_moderator",json("p_group" to room.id,"p_member" to member.str("user_id"),"p_enabled" to (member.str("role")!="moderator")));refresh() }}) { Text(if(member.str("role")=="moderator") "Retirer le rôle modérateur" else "Nommer modérateur") }
                }
            } }
            if(canModerate) {
                Text("Inviter un ami")
                snapshot?.links?.filter { it.str("status")=="accepted" }?.forEach { link -> val friend=if(link.str("requester_id")==owner) link.str("recipient_id") else link.str("requester_id");val name=snapshot.profiles.firstOrNull { it.str("id")==friend }?.str("display_name")?:"Ami";TextButton(onClick={vm.action { vm.repo.rpc("invite_group_member",json("p_group" to room.id,"p_friend" to friend));refresh() }}) { Text(name) } }
            }
            if(mine?.str("role")=="owner") TextButton(onClick={confirm="delete_friend_group" to json("p_group" to room.id)}) { Text("Supprimer le groupe") }
        }
    }
    confirm?.let { operation -> AlertDialog(onDismissRequest={confirm=null},title={Text("Confirmer cette action")},text={Text("Cette action sera appliquée à cette conversation ou à ses membres.")},confirmButton={TextButton(onClick={vm.action { vm.repo.rpc(operation.first,operation.second);confirm=null;if(operation.first in listOf("remove_friend","block_friend","delete_friend_group")) close() else refresh() }}) { Text("Confirmer") }},dismissButton={TextButton(onClick={confirm=null}) { Text("Annuler") }}) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CalendarDateField(value: String,onChange: (String)->Unit) {
    var show by remember { mutableStateOf(false) };TextButton(onClick={show=true}) { Text("Date : $value") }
    if(show) { val state=rememberDatePickerState(initialSelectedDateMillis=LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());DatePickerDialog(onDismissRequest={show=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) };show=false}) { Text("Choisir") }},dismissButton={TextButton(onClick={show=false}) { Text("Annuler") }}) { DatePicker(state) } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ClockTimeField(value: String,onChange: (String)->Unit) {
    var show by remember { mutableStateOf(false) };TextButton(onClick={show=true}) { Text("Heure : $value") }
    if(show) { val time=LocalTime.parse(value);val state=rememberTimePickerState(time.hour,time.minute,true);AlertDialog(onDismissRequest={show=false},title={Text("Heure du rendez-vous")},text={TimePicker(state)},confirmButton={TextButton(onClick={onChange(LocalTime.of(state.hour,state.minute).toString());show=false}) { Text("Choisir") }},dismissButton={TextButton(onClick={show=false}) { Text("Annuler") }}) }
}

@Composable private fun SocialProfileScreen(vm: CoranViewModel,profile: JsonObject,back: ()->Unit) {
    var name by rememberSaveable { mutableStateOf(profile.str("display_name")) };var online by rememberSaveable { mutableStateOf(profile.flag("share_online")) };var location by rememberSaveable { mutableStateOf(profile.flag("share_location")) };var progress by rememberSaveable { mutableStateOf(profile.flag("share_progress")) }
    PageList {
        TextButton(onClick=back) { Text("Retour aux amis") }
        AvatarEditor(vm,profile.str("avatar_path"),back)
        OutlinedTextField(name,{name=it},label={Text("Nom affiché")})
        Row { Checkbox(online,{online=it});Text("Partager mon statut en ligne") }
        Row { Checkbox(location,{location=it});Text("Partager ma localisation") }
        Row { Checkbox(progress,{progress=it});Text("Partager ma progression") }
        Button(onClick={vm.action { require(name.trim().isNotBlank());vm.repo.updateRows("friend_profiles",json("display_name" to name.trim(),"share_online" to online,"share_location" to location,"share_progress" to progress),mapOf("id" to (vm.repo.user.value?:error("Connexion nécessaire"))));back() }}) { Text("Enregistrer") }
    }
}
