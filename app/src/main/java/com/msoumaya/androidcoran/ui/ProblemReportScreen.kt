package com.msoumaya.androidcoran.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.problemTypes
import kotlinx.coroutines.CancellationException

@Composable fun ReportScreen(vm: CoranViewModel) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var description by rememberSaveable(owner) { mutableStateOf("") };var type by rememberSaveable { mutableStateOf("Bug") };var attachment by rememberSaveable(owner) { mutableStateOf<String?>(null) };var busy by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> attachment=uri?.toString() }
    PageList {
        Text("Signaler un dysfonctionnement",style=MaterialTheme.typography.headlineSmall)
        problemTypes.forEach { item -> FilterChip(selected=item==type,onClick={type=item},label={Text(item)}) }
        OutlinedTextField(description,{if(it.length<=500) description=it},label={Text("Description (500 caractères maximum)")},minLines=4)
        Row {
            TextButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) { Text(if(attachment==null) "Joindre une capture" else "Remplacer la capture") }
            if(attachment!=null) TextButton(onClick={attachment=null}) { Text("Retirer") }
        }
        if(attachment!=null) Text("Capture sélectionnée · JPEG ou PNG, 5 Mo maximum")
        if(owner==null) Text("Connecte-toi pour envoyer un signalement")
        Button(enabled=owner!=null&&description.isNotBlank()&&!busy,onClick={vm.action {
            busy=true
            try {
                vm.repo.reports.queue(type,description,attachment?.let(Uri::parse));description="";attachment=null
                try { vm.repo.reports.flush();vm.repo.feedback(if(owner?.let { vm.repo.reports.pending(it) }==true) "Signalement conservé, en attente de synchronisation" else "Signalement envoyé") }
                catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback("Signalement sauvegardé sur cet appareil. Il sera envoyé au retour de la connexion.") }
            } finally { busy=false }
        }}) { Text(if(busy) "Sauvegarde…" else "Envoyer le signalement") }
    }
}
