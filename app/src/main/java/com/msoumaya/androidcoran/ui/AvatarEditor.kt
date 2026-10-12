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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable fun AvatarPreview(repo: Repository,path: String?,modifier: Modifier=Modifier.size(56.dp)) {
    val owner by repo.user.collectAsStateWithLifecycle()
    val url by produceState<String?>(null,path,owner) {
        value=null
        if(!path.isNullOrBlank()) try { value=repo.signedAvatar(path) } catch(e: Exception) { if(e is CancellationException) throw e }
    }
    url?.let { RemoteImage(it,"Photo de profil",modifier.clip(CircleShape),ContentScale.Crop) }
}
@Composable fun AvatarEditor(vm: CoranViewModel,profilePath: String?,changed: ()->Unit,pendingEmail: String?=null) {
    val context=LocalContext.current
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    var draftEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var draftOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) { draftOwner=vm.repo.user.value;draftEmail=pendingEmail?.let(::avatarEmail);draft=uri.toString() } }
    LaunchedEffect(owner,pendingEmail) { if(draft!=null&&(draftOwner!=owner||draftEmail!=pendingEmail?.let(::avatarEmail))) { draft=null;draftOwner=null } }
    draft?.let { raw -> key(raw) {
        var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        LaunchedEffect(raw) {
            var loaded: android.graphics.Bitmap?=null
            try {
                withContext(Dispatchers.IO) { loaded=decodeAvatar(context,android.net.Uri.parse(raw)) }
                currentCoroutineContext().ensureActive()
                bitmap=loaded;loaded=null
            } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Image illisible");draft=null }
            finally { loaded?.recycle() }
        }
        DisposableEffect(bitmap) { val loaded=bitmap;onDispose { loaded?.recycle() } }
        AvatarCropDialog(bitmap,busy,onClose={draft=null},onConfirm={ selection ->
            scope.launch {
                busy=true
                try { if(draftEmail!=null) vm.repo.stageAvatar(draftEmail!!,android.net.Uri.parse(raw),selection) else vm.repo.uploadAvatar(context,android.net.Uri.parse(raw),selection,draftOwner);draft=null;vm.repo.feedback(if(pendingEmail!=null) "Photo conservée pour ce compte" else "Photo enregistrée");changed() }
                catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Enregistrement impossible") }
                finally { busy=false }
            }
        })
    } }
    if(pendingEmail!=null) PendingAvatarPreview(vm.repo,pendingEmail) else AvatarPreview(vm.repo,profilePath,Modifier.size(100.dp))
    Row {
        TextButton(enabled=!busy&&(pendingEmail==null||validAvatarEmail(pendingEmail)),onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) { Text(if(pendingEmail!=null) "Ajouter une photo (facultatif)" else "Choisir une photo") }
        if(pendingEmail!=null) TextButton(enabled=!busy&&validAvatarEmail(pendingEmail),onClick={vm.action { vm.repo.removePendingAvatar(pendingEmail) }}) { Text("Retirer la photo choisie") }
        if(!profilePath.isNullOrBlank()) TextButton(enabled=!busy,onClick={vm.action { vm.repo.removeAvatar();vm.repo.feedback("Photo supprimée");changed() }}) { Text("Supprimer la photo") }
    }
}

@Composable private fun PendingAvatarPreview(repo: Repository,email: String) {
 val version by repo.pendingAvatarVersion.collectAsStateWithLifecycle()
 var bitmap by remember(email) { mutableStateOf<android.graphics.Bitmap?>(null) }
 LaunchedEffect(email,version) {
  bitmap=null
  var loaded: android.graphics.Bitmap?=null
  try {
   withContext(Dispatchers.IO) { repo.pendingAvatars.load(email)?.let { loaded=android.graphics.BitmapFactory.decodeByteArray(it,0,it.size) } }
   currentCoroutineContext().ensureActive();bitmap=loaded;loaded=null
  } catch(e: Exception) { if(e is CancellationException) throw e;repo.feedback("Photo locale indisponible") }
  finally { loaded?.recycle() }
 }
 DisposableEffect(bitmap) { val loaded=bitmap;onDispose { loaded?.recycle() } }
 bitmap?.let { androidx.compose.foundation.Image(it.asImageBitmap(),"Photo choisie pour ce compte",Modifier.size(100.dp).clip(CircleShape),contentScale=ContentScale.Crop) }
 if(!validAvatarEmail(email)) Text("Saisis ton e-mail pour choisir une photo.")
}
