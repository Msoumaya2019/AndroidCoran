package com.msoumaya.androidcoran.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.Locale

@Composable fun AdminNotificationsPanel(vm: CoranViewModel) {
    var recipients by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var history by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var target by rememberSaveable { mutableStateOf<String?>(null) };var search by rememberSaveable { mutableStateOf("") };var title by rememberSaveable { mutableStateOf("") };var body by rememberSaveable { mutableStateOf("") };var request by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var busy by remember { mutableStateOf(false) };var confirm by remember { mutableStateOf(false) }
    fun changed() { request=UUID.randomUUID().toString() }
    suspend fun load() { check(AdminService(vm.repo).authorized());recipients=vm.repo.rpc("admin_notification_recipients").jsonArray.map { it.jsonObject };history=AdminService(vm.repo).notificationHistory() }
    LaunchedEffect(Unit) { vm.action { load() } }
    Text("Notifications personnalisées",style=MaterialTheme.typography.headlineSmall)
    Text("Service existant : les préférences de chaque destinataire sont respectées.")
    FilterChip(selected=target==null,enabled=!busy,onClick={changed();target=null},label={Text("Tous les élèves éligibles (${recipients.size})")})
    OutlinedTextField(search,{search=it},label={Text("Rechercher un élève")})
    recipients.filter { it.str("display_name").lowercase(Locale.FRENCH).contains(search.lowercase(Locale.FRENCH)) }.forEach { p -> FilterChip(selected=target==p.str("user_id"),enabled=!busy,onClick={changed();target=p.str("user_id")},label={Text("${p.str("display_name")} · ${p.num("device_count")} appareils")}) }
    OutlinedTextField(title,{if(it.length<=80) { changed();title=it }},enabled=!busy,label={Text("Titre · 3 à 80 caractères")})
    OutlinedTextField(body,{if(it.length<=500) { changed();body=it }},enabled=!busy,label={Text("Message · 3 à 500 caractères")},minLines=3)
    Panel(title.trim().ifBlank { "Aperçu" },body.trim())
    val eligible=if(target==null) recipients.isNotEmpty() else recipients.any { it.str("user_id")==target }
    Button(enabled=!busy&&eligible&&title.trim().length>=3&&body.trim().length>=3,onClick={confirm=true}) { Text(if(busy) "Envoi en cours…" else "Envoyer la notification") }
    Text("Derniers envois")
    history.forEach { row -> Panel(row.str("title"),row.str("body")) { Text("${row.str("created_at")} · ${row.num("recipient_count")} élèves · ${row.num("device_count")} appareils") } }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Envoyer cette notification ?")},text={Text("${if(target==null) "Tous les élèves éligibles" else recipients.firstOrNull { it.str("user_id")==target }?.str("display_name")?:"Un élève"}\n\n${title.trim()}\n${body.trim()}")},dismissButton={TextButton(onClick={confirm=false}) { Text("Annuler") }},confirmButton={TextButton(onClick={
        confirm=false;busy=true
        vm.action { try {
            check(AdminService(vm.repo).authorized())
            val payload=adminNotificationPayload(target,title,body,request)
            val result=vm.repo.rpc("send_admin_notification",payload).jsonArray.firstOrNull()?.jsonObject?:error("Aucun accusé de réception Supabase")
            title="";body="";changed();vm.repo.feedback("Envoi lancé : ${result.num("recipient_count")} élèves, ${result.num("device_count")} appareils")
            load()
        } finally { busy=false } }
    }) { Text("Envoyer") }})
}
