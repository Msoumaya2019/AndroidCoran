package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.AdminService
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.util.Locale

@Composable fun AdminEntry(vm: CoranViewModel,navigate: (String)->Unit) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var allowed by remember(owner) { mutableStateOf(false) }
    LaunchedEffect(owner) { if(owner!=null) vm.action { allowed=AdminService(vm.repo).authorized() } }
    if(allowed) Button(onClick={navigate("Administration")}) { Text("Administration") }
}

@Composable fun AdminScreen(vm: CoranViewModel) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val service=remember(vm) { AdminService(vm.repo) }
    var allowed by remember(owner) { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("Comptes") }
    var search by rememberSaveable { mutableStateOf("") }
    var accounts by remember(owner) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var total by remember(owner) { mutableIntStateOf(0) }
    fun load(offset: Int) {
        if(busy) return
        busy=true
        vm.action { try {
            val rows=service.accounts(offset,search)
            if(vm.repo.user.value==owner) {
                accounts=if(offset==0) rows else (accounts+rows).distinctBy { it.str("user_id") }
                if(rows.isNotEmpty()) total=rows.first().num("total_accounts") else if(offset==0) total=0
            }
        } finally { busy=false } }
    }
    LaunchedEffect(owner) { if(owner!=null) vm.action { allowed=service.authorized();if(allowed) load(0) } }
    PageList {
        Text("Administration")
        if(!allowed) Text("Connexion avec un compte administrateur nécessaire.") else {
            TextButton(onClick={page="Contenus"}) { Text("Administrer les contenus") }
            TextButton(onClick={page="Récitations"}) { Text("Corriger les récitations") }
            TextButton(onClick={page="Signalements"}) { Text("Traiter les signalements") }
            TextButton(onClick={page="Quiz"}) { Text("Administrer les quiz") }
            Row {
                FilterChip(selected=page=="Comptes",onClick={page="Comptes"},label={Text("Comptes")})
                FilterChip(selected=page=="Notifications",onClick={page="Notifications"},label={Text("Historique notifications")})
            }
            if(page=="Contenus") AdminContentsPanel(vm) else if(page=="Récitations") AdminRecitationsPanel(vm) else if(page=="Signalements") AdminReportsPanel(vm) else if(page=="Quiz") AdminQuizPanel(vm) else if(page=="Comptes") {
                Text("$total comptes inscrits")
                OutlinedTextField(search,{search=it},label={Text("Prénom ou adresse e-mail")},singleLine=true)
                Button(onClick={load(0)},enabled=!busy) { Text("Rechercher / actualiser") }
                Text("Données d’apprentissage synchronisées. Les versets simplement consultés ne sont pas comptés.")
                accounts.forEach { row ->
                    val state=defaultState().with("knowledge" to row.obj("knowledge"),"goal" to (row["goal"] as? JsonObject?:defaultState().obj("goal")))
                    val progress=vm.repo.program.progress(state)
                    Panel(row.str("first_name"),row.str("email").ifBlank { "Adresse non renseignée" }) {
                        Text("Inscrit le ${row.str("created_at").take(10)}")
                        if(row.str("synced_at").isNotBlank()) {
                            Text("${knownIds(state).size} versets mémorisés · ${String.format(Locale.FRANCE,"%.1f",progress.first*100)} % du Coran")
                            if(row["goal"] is JsonObject) {
                                Text("Objectif : ${state.obj("goal").str("label")} · ${String.format(Locale.FRANCE,"%.1f",progress.second*100)} %")
                                LinearProgressIndicator(progress={progress.second.coerceIn(0f,1f)})
                            }
                            Text("Rythme : ${paceLabels[row.str("pace")]?:"Non renseigné"}")
                            Text("Dernière synchronisation : ${row.str("synced_at")}")
                        } else Text("Apprentissage pas encore synchronisé.")
                    }
                }
                if(busy) CircularProgressIndicator() else if(accounts.size<total) Button(onClick={load(accounts.size)}) { Text("Afficher les comptes suivants") }
                if(!busy&&accounts.isEmpty()) Text("Aucun compte correspondant.")
            } else AdminNotificationsPanel(vm)
        }
    }
}
