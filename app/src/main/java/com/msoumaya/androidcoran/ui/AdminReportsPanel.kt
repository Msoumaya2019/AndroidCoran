package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import io.github.jan.supabase.storage.storage

@Composable fun AdminReportsPanel(vm: CoranViewModel) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var busy by remember { mutableStateOf(false) };var image by remember { mutableStateOf<String?>(null) }
    fun act(block: suspend ()->Unit) { if(busy) return;busy=true;vm.action { try { check(AdminService(vm.repo).authorized());block() } finally { busy=false } } }
    suspend fun load() { rows=vm.repo.query("app_problem_reports",size=100) }
    LaunchedEffect(Unit) { act { load() } }
    Text("Problèmes de l’application",style=MaterialTheme.typography.headlineSmall)
    Text("Les captures sont privées : auteur et administrateurs uniquement.")
    Button(onClick={act { load() }},enabled=!busy) { Text("Actualiser") }
    rows.forEach { row -> Panel("${row.str("type")} · ${if(row.str("status")=="open") "À traiter" else "Traité"}",row.str("description")) {
        Text("${row.str("created_at")} · ${row.str("platform")} · v${row.str("app_version")}")
        Text("Compte : ${row.str("user_id")}")
        if(row.str("screenshot_path").isNotBlank()) TextButton(enabled=!busy,onClick={act { image=vm.repo.db().storage.from("problem-report-screenshots").createSignedUrl(row.str("screenshot_path"),kotlin.time.Duration.parse("5m")) }}) { Text("Voir la capture") }
        TextButton(enabled=!busy,onClick={act { vm.repo.updateRows("app_problem_reports",json("status" to if(row.str("status")=="open") "resolved" else "open"),mapOf("id" to row.str("id")));load() }}) { Text(if(row.str("status")=="open") "Marquer comme traité" else "Rouvrir") }
    } }
    if(rows.isEmpty()) Text("Aucun signalement chargé")
    image?.let { url -> androidx.compose.ui.window.Dialog(onDismissRequest={image=null}) { Surface { Column(Modifier.padding(16.dp)) { RemoteImage(url,"Capture du signalement",Modifier.fillMaxWidth().height(450.dp));TextButton(onClick={image=null}) { Text("Fermer") } } } } }
}
