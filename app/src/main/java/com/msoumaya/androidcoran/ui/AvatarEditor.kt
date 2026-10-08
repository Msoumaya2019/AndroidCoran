package com.msoumaya.androidcoran.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.CancellationException

@Composable fun AvatarPreview(repo: Repository,path: String?,modifier: Modifier=Modifier.size(56.dp)) {
    val owner by repo.user.collectAsStateWithLifecycle()
    val url by produceState<String?>(null,path,owner) {
        value=null
        if(!path.isNullOrBlank()) try { value=repo.signedAvatar(path) } catch(e: Exception) { if(e is CancellationException) throw e }
    }
    url?.let { RemoteImage(it,"Photo de profil",modifier.clip(CircleShape),ContentScale.Crop) }
}
@Composable fun AvatarEditor(vm: CoranViewModel,profilePath: String?,changed: ()->Unit) {
    val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) vm.action { vm.repo.uploadAvatar(context,uri);vm.repo.feedback("Photo enregistrée");changed() } }
    AvatarPreview(vm.repo,profilePath,Modifier.size(100.dp))
    Row {
        TextButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) { Text("Choisir une photo") }
        if(!profilePath.isNullOrBlank()) TextButton(onClick={vm.action { vm.repo.removeAvatar();vm.repo.feedback("Photo supprimée");changed() }}) { Text("Supprimer la photo") }
    }
}
