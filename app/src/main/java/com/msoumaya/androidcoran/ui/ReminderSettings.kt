package com.msoumaya.androidcoran.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.data.LocalReminders
import com.msoumaya.androidcoran.data.NotificationTestWorker
import kotlinx.serialization.json.*

@Composable fun ReminderSettings(vm: CoranViewModel,s: JsonObject) {
    val context=LocalContext.current
    var testing by rememberSaveable { mutableStateOf(false) }
    var permissionOwner by rememberSaveable { mutableStateOf<String?>(null) }
    fun enable(enabled: Boolean) { vm.action { vm.repo.mutate { touch(it.with("notifications" to it.obj("notifications").with("learning" to JsonPrimitive(enabled)))) };vm.repo.refreshReminders() } }
    fun test() {
        if(NotificationTestWorker.enqueue(context,vm.repo.user.value?:"guest")) vm.repo.feedback("Notification de test programmée. Android peut différer son affichage.")
        else vm.repo.feedback("Autorise les notifications dans les réglages Android pour effectuer ce test.")
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(permissionOwner==(vm.repo.user.value?:"guest")) {
            if(testing) { if(granted) test() } else enable(granted)
            if(!granted) vm.repo.feedback("Les notifications sont désactivées dans les autorisations Android")
        }
        permissionOwner=null
    }
    Text("Rappel d’apprentissage à 19 h")
    Switch(checked=s.obj("notifications").flag("learning"),onCheckedChange={ enabled ->
        if(enabled&&Build.VERSION.SDK_INT>=33&&!LocalReminders.permissionGranted(context)) {
            testing=false;permissionOwner=vm.repo.user.value?:"guest";permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else enable(enabled)
    })
    Text("Android peut différer un rappel selon ses règles d’économie de batterie.")
    TextButton(onClick={
        if(Build.VERSION.SDK_INT>=33&&!LocalReminders.permissionGranted(context)) {
            testing=true;permissionOwner=vm.repo.user.value?:"guest";permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else test()
    }) { Text("Tester une notification locale") }
}
