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
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

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
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    var draftOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) { draftOwner=vm.repo.user.value;draft=uri.toString() } }
    LaunchedEffect(owner) { if(draft!=null&&draftOwner!=owner) { draft=null;draftOwner=null } }
    draft?.let { raw -> key(raw) {
        var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        LaunchedEffect(raw) {
            try { bitmap=decodeAvatar(context,android.net.Uri.parse(raw)) } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Image illisible");draft=null }
        }
        DisposableEffect(bitmap) { val loaded=bitmap;onDispose { loaded?.recycle() } }
        AvatarCropDialog(bitmap,busy,onClose={draft=null},onConfirm={ selection ->
            scope.launch {
                busy=true
                try { vm.repo.uploadAvatar(context,android.net.Uri.parse(raw),selection,draftOwner);draft=null;vm.repo.feedback("Photo enregistrée");changed() }
                catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Enregistrement impossible") }
                finally { busy=false }
            }
        })
    } }
    AvatarPreview(vm.repo,profilePath,Modifier.size(100.dp))
    Row {
        TextButton(enabled=!busy,onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) { Text("Choisir une photo") }
        if(!profilePath.isNullOrBlank()) TextButton(enabled=!busy,onClick={vm.action { vm.repo.removeAvatar();vm.repo.feedback("Photo supprimée");changed() }}) { Text("Supprimer la photo") }
    }
}
