package com.msoumaya.androidcoran.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import com.msoumaya.androidcoran.MainActivity
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import java.time.ZonedDateTime

object LocalReminders {
    private const val CHANNEL="learning"
    const val ACTION_REMIND="com.msoumaya.androidcoran.LEARNING_REMINDER"
    const val ACCOUNT_EXTRA="reminderAccount"
    val rescheduleActions=setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_MY_PACKAGE_REPLACED)
    @Synchronized fun schedule(context: Context,enabled: Boolean,account: String) {
        val prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE)
        if(!enabled||prefs.getString("account",null)!=account) context.getSystemService(NotificationManager::class.java).cancel(19)
        val manager=context.getSystemService(AlarmManager::class.java)
        val intent=PendingIntent.getBroadcast(context,19,Intent(context,ReminderReceiver::class.java).setAction(ACTION_REMIND).putExtra(ACCOUNT_EXTRA,account),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Cancel the old version's action-less alarm as well when upgrading.
        PendingIntent.getBroadcast(context,19,Intent(context,ReminderReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { manager.cancel(it);it.cancel() }
        manager.cancel(intent)
        val next=if(enabled) nextLearningReminder(ZonedDateTime.now()).toInstant().toEpochMilli() else 0L
        if(enabled) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,intent)
        prefs.edit().putBoolean("enabled",enabled).putString("account",account).putLong("nextAt",next).commit()
    }
    fun notify(context: Context,account: String=context.getSharedPreferences("reminders",Context.MODE_PRIVATE).getString("account","guest")?:"guest"): Boolean {
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return false
        val manager=context.getSystemService(NotificationManager::class.java)
        if(!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Apprentissage du Coran",NotificationManager.IMPORTANCE_HIGH))
        val intent=PendingIntent.getActivity(context,19,Intent(context,MainActivity::class.java).putExtra("destination","Programme").putExtra(ACCOUNT_EXTRA,account).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(19,Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Ton programme du Coran").setContentText("Retrouve ton passage du jour et prends un moment pour apprendre.").setContentIntent(intent).setAutoCancel(true).build())
        return true
    }
    @Synchronized fun handleBroadcast(context: Context,intent: Intent) {
        if(intent.action!=ACTION_REMIND&&intent.action !in rescheduleActions) return
        val prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE)
        val account=prefs.getString("account","guest")?:"guest"
        if(intent.action==ACTION_REMIND&&intent.getStringExtra(ACCOUNT_EXTRA)!=account) return
        val store=LocalStore(context)
        val enabled=try { prefs.getBoolean("enabled",false)&&store.load(account)?.data?.obj("notifications")?.flag("learning")==true } finally { store.close() }
        if(enabled&&intent.action==ACTION_REMIND) notify(context,account)
        schedule(context,enabled,account)
    }
}
class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action!=LocalReminders.ACTION_REMIND&&intent.action !in LocalReminders.rescheduleActions) return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try { LocalReminders.handleBroadcast(context.applicationContext,intent) }
            catch(e: Exception) { if(e is CancellationException) throw e;android.util.Log.w("CoranReminders","Reprogrammation locale indisponible") }
            finally { pending?.finish() }
        }
    }
}
