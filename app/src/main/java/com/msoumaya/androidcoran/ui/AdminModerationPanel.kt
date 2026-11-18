package com.msoumaya.androidcoran.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
@Composable fun AdminModerationPanel(vm: CoranViewModel) {
 val owner by vm.repo.user.collectAsStateWithLifecycle()
 var rows by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) };var people by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
 var reports by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
 var busy by remember { mutableStateOf(false) };var target by remember { mutableStateOf<JsonObject?>(null) }
 fun load() { if(busy) return;busy=true;vm.action { try {
  val result=AdminService(vm.repo).messages();val incoming=vm.repo.query("friend_message_reports",eq=mapOf("status" to "open"),size=100);val profiles=vm.repo.query("friend_profiles",ids="id" to (result.map { it.str("sender_id") }+incoming.map { it.str("reporter_id") }).distinct(),orderBy="display_name",size=300)
  if(vm.repo.user.value==owner) { rows=result;people=profiles;reports=incoming }
 } finally { busy=false } } }
 LaunchedEffect(owner) { if(owner!=null) load() }
 Text("Messages récents",style=MaterialTheme.typography.titleLarge)
 Button(enabled=!busy,onClick=::load) { Text("Actualiser la modération") }
 Text("Signalements ouverts",style=MaterialTheme.typography.titleMedium)
 reports.forEach { report ->
  val message=rows.firstOrNull { it.str("id")==report.str("message_id") }
  Panel("Signalement de "+(people.firstOrNull { it.str("id")==report.str("reporter_id") }?.str("display_name")?:report.str("reporter_id")),report.str("reason")) {
   if(message!=null) { Text(adminMessageDestination(message));Text(message.str("body")) }
   else Text("Message absent des 100 messages récents")
   TextButton(onClick={vm.action { check(AdminService(vm.repo).authorized());vm.repo.rpc("resolve_friend_report",json("p_report" to report.str("id")));load() }}) { Text("Classer sans suppression") }
  }
 }
 rows.forEach { row -> Panel("De : "+(people.firstOrNull { it.str("id")==row.str("sender_id") }?.str("display_name")?:row.str("sender_id")),adminMessageDestination(row)) {
  Text(row.str("created_at"));Text(row.str("body"))
  if(row.str("deleted_at").isBlank()) TextButton(onClick={target=row}) { Text("Supprimer le message") }
 } }
 target?.let { row -> AlertDialog(onDismissRequest={target=null},title={Text("Supprimer ce message ?")},confirmButton={TextButton(onClick={target=null;vm.action { check(AdminService(vm.repo).authorized());vm.repo.rpc("delete_friend_message",json("p_message" to row.str("id")));load() }}) { Text("Supprimer") }},dismissButton={TextButton(onClick={target=null}) { Text("Annuler") }}) }
 if(busy) CircularProgressIndicator()
}
