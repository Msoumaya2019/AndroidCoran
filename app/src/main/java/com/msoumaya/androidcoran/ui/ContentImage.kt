package com.msoumaya.androidcoran.ui
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.ContentMedia
import kotlinx.coroutines.CancellationException
@Composable fun ContentImage(vm: CoranViewModel,url: String,title: String,modifier: Modifier=Modifier) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var resolved by remember(url,owner) { mutableStateOf<String?>(null) }
    LaunchedEffect(url,owner) { try { resolved=ContentMedia(vm.repo).resolve(url) } catch(e: Exception) { if(e is CancellationException) throw e;vm.repo.feedback(e.message?:"Image indisponible") } }
    resolved?.let { RemoteImage(it,title,modifier) }
}
