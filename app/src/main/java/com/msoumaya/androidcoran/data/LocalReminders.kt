package com.msoumaya.androidcoran.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import com.msoumaya.androidcoran.MainActivity
import com.msoumaya.androidcoran.domain.obj
import com.msoumaya.androidcoran.domain.flag
import java.time.ZonedDateTime

object LocalReminders {
    private const val CHANNEL="learning"
    fun schedule(context: Context,enabled: Boolean,account: String) {
        context.getSharedPreferences("reminders",Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).putString("account",account).apply()
        val manager=context.getSystemService(AlarmManager::class.java);val intent=PendingIntent.getBroadcast(context,19,Intent(context,ReminderReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(intent)
        if(enabled) { val now=ZonedDateTime.now();var next=now.withHour(19).withMinute(0).withSecond(0).withNano(0);if(!next.isAfter(now)) next=next.plusDays(1);manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),intent) }
    }
    fun notify(context: Context) {
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        val manager=context.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel(CHANNEL,"Apprentissage du Coran",NotificationManager.IMPORTANCE_DEFAULT))
        val intent=PendingIntent.getActivity(context,19,Intent(context,MainActivity::class.java).putExtra("destination","Programme"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(19,Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Ton programme du Coran").setContentText("Retrouve ton passage du jour et prends un moment pour apprendre.").setContentIntent(intent).setAutoCancel(true).build())
    }
}
class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE);val account=prefs.getString("account","guest")?:"guest";val store=LocalStore(context);val enabled=prefs.getBoolean("enabled",false)&&store.load(account)?.data?.obj("notifications")?.flag("learning")==true;store.close()
        if(enabled&&intent.action!=Intent.ACTION_BOOT_COMPLETED) LocalReminders.notify(context)
        LocalReminders.schedule(context,enabled,account)
    }
}
