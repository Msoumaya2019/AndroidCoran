package com.msoumaya.androidcoran.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.data.LocalReminders
import com.msoumaya.androidcoran.data.NotificationTestWorker
import kotlinx.serialization.json.*

@Composable fun ReminderSettings(vm: CoranViewModel,s: JsonObject) {
    val context=LocalContext.current
    fun enable(enabled: Boolean) { vm.action { vm.repo.mutate { touch(it.with("notifications" to it.obj("notifications").with("learning" to JsonPrimitive(enabled)))) };vm.repo.refreshReminders() } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { enable(it);if(!it) vm.repo.feedback("Les notifications sont désactivées dans les autorisations Android") }
    Text("Rappel d’apprentissage à 19 h");Switch(checked=s.obj("notifications").flag("learning"),onCheckedChange={if(it&&Build.VERSION.SDK_INT>=33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else enable(it) })
    Text("Android peut différer un rappel selon ses règles d’économie de batterie.")
    TextButton(onClick={if(NotificationTestWorker.enqueue(context,vm.repo.user.value?:"guest")) vm.repo.feedback("Notification de test programmée. Android peut différer son affichage.") else vm.repo.feedback("Autorise les notifications dans les réglages Android pour effectuer ce test.")}) { Text("Tester une notification locale") }
}
