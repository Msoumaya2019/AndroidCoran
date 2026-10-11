package com.msoumaya.androidcoran

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import com.msoumaya.androidcoran.data.LocalReminders
import com.msoumaya.androidcoran.domain.ReminderNavigation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.msoumaya.androidcoran.ui.CoranApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    private val reminder=MutableStateFlow<ReminderNavigation?>(null)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { val request by reminder.collectAsState();CoranApp(reminderRequest=request,onReminderHandled={id -> if(reminder.value?.id==id) reminder.value=null}) };consumeReminderIntent(intent);consumeAuthIntent(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);consumeReminderIntent(intent);consumeAuthIntent(intent) }
    private fun consumeReminderIntent(incoming: Intent) {
        val account=incoming.getStringExtra(LocalReminders.ACCOUNT_EXTRA)?:return
        val destination=incoming.getStringExtra("destination")
        incoming.removeExtra(LocalReminders.ACCOUNT_EXTRA);incoming.removeExtra("destination")
        if(destination=="Programme"&&account.isNotBlank()) reminder.value=ReminderNavigation(account)
    }
    private fun consumeAuthIntent(incoming: Intent) {
        val uri=incoming.data ?: return
        if(uri.scheme!="coranmemoire"||uri.host!="auth") return
        val raw=uri.toString();incoming.data=null
        val repo=(application as CoranApplication).repository
        lifecycleScope.launch { try { repo.consumeAuthLink(raw) } catch(e: Exception) { if(e is CancellationException) throw e;repo.feedback("Lien expiré, incomplet ou connexion indisponible. Demande un nouveau courriel si nécessaire.") } }
    }
}
