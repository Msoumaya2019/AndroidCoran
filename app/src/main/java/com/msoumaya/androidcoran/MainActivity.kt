package com.msoumaya.androidcoran

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.msoumaya.androidcoran.ui.CoranApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { CoranApp() };consumeAuthIntent(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);consumeAuthIntent(intent) }
    private fun consumeAuthIntent(incoming: Intent) {
        val uri=incoming.data ?: return
        if(uri.scheme!="coranmemoire"||uri.host!="auth") return
        val raw=uri.toString();incoming.data=null
        val repo=(application as CoranApplication).repository
        lifecycleScope.launch { try { repo.consumeAuthLink(raw) } catch(e: Exception) { if(e is CancellationException) throw e;repo.feedback("Lien expiré, incomplet ou connexion indisponible. Demande un nouveau courriel si nécessaire.") } }
    }
}
