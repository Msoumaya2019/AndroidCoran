package com.msoumaya.androidcoran

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LearningReminderTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun permission() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName,Manifest.permission.POST_NOTIFICATIONS)
    }
    private fun fixture(block: ()->Unit) {
        val repo=(compose.activity.application as CoranApplication).repository
        runBlocking { repo.awaitReady() };assertNull(repo.user.value)
        val saved=repo.state.value
        runBlocking { repo.mutate { defaultState().with("notifications" to json("learning" to true)) };repo.refreshReminders() }
        compose.waitForIdle()
        try { block() } finally {
            compose.activity.getSystemService(NotificationManager::class.java).cancel(19)
            compose.activity.getSystemService(NotificationManager::class.java).cancel(20)
            androidx.work.WorkManager.getInstance(compose.activity).cancelUniqueWork(NotificationTestWorker.NAME).result.get()
            runBlocking { repo.mutate { saved };repo.refreshReminders() }
            compose.waitForIdle()
        }
    }
    @Test fun tappingPostedReminderOpensProgramAndConsumesItsAccountExtras()=fixture {
        val context=compose.activity
        assertTrue(LocalReminders.notify(context,"guest"))
        compose.waitUntil(10000) { context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id==19 } }
        val notification=context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id==19 }
        notification.notification.contentIntent.send()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Mon programme").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mon programme").assertExists()
        compose.runOnIdle { assertFalse(compose.activity.intent.hasExtra(LocalReminders.ACCOUNT_EXTRA));assertFalse(compose.activity.intent.hasExtra("destination")) }
    }
    @Test fun reminderIntentIsHandledWhenActivityIsCreatedAgain()=fixture {
        compose.runOnUiThread { compose.activity.intent=Intent(compose.activity.intent).putExtra("destination","Programme").putExtra(LocalReminders.ACCOUNT_EXTRA,"guest") }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Mon programme").assertExists()
    }
    @Test fun anotherAccountsReminderCannotSwitchTheCurrentProgram()=fixture {
        val context=compose.activity
        assertTrue(LocalReminders.notify(context,"different-account"))
        compose.waitUntil(10000) { context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id==19 } }
        context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id==19 }.notification.contentIntent.send()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Ce rappel appartient à un autre compte.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ce rappel appartient à un autre compte.").assertExists()
        compose.onNodeWithText("Mon programme").assertDoesNotExist()
        assertNull((compose.activity.application as CoranApplication).repository.user.value)
    }
    @Test fun bootAndClockEventsRescheduleWithoutPostingAnImmediateReminder()=fixture {
        val context=compose.activity
        val prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE)
        for(action in LocalReminders.rescheduleActions) {
            prefs.edit().putLong("nextAt",0).commit()
            LocalReminders.handleBroadcast(context,Intent(action))
            val next=Instant.ofEpochMilli(prefs.getLong("nextAt",0)).atZone(ZoneId.systemDefault())
            assertTrue(next.toInstant()>Instant.now());assertEquals(19,next.hour);assertEquals(0,next.minute)
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id==19 })
        }
    }
    @Test fun disablingReminderCancelsNotificationAndRejectsStaleDelivery()=fixture {
        val context=compose.activity
        assertTrue(LocalReminders.notify(context,"guest"))
        val repo=(context.application as CoranApplication).repository
        runBlocking { repo.mutate { it.with("notifications" to it.obj("notifications").with("learning" to JsonPrimitive(false))) };repo.refreshReminders() }
        LocalReminders.handleBroadcast(context,Intent(LocalReminders.ACTION_REMIND).putExtra(LocalReminders.ACCOUNT_EXTRA,"guest"))
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id==19 })
        assertEquals(0L,context.getSharedPreferences("reminders",Context.MODE_PRIVATE).getLong("nextAt",-1))
    }
    @Test fun expiredAccountsAlarmCannotNotifyOrReplaceCurrentSchedule()=fixture {
        val context=compose.activity
        val prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE)
        val next=prefs.getLong("nextAt",0)
        LocalReminders.handleBroadcast(context,Intent(LocalReminders.ACTION_REMIND).putExtra(LocalReminders.ACCOUNT_EXTRA,"old-account"))
        assertEquals(next,prefs.getLong("nextAt",0));assertEquals("guest",prefs.getString("account",null))
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id==19 })
    }
    @Test fun deliveryPostsOneReminderAndSchedulesTheNextDay()=fixture {
        val context=compose.activity
        LocalReminders.handleBroadcast(context,Intent(LocalReminders.ACTION_REMIND).putExtra(LocalReminders.ACCOUNT_EXTRA,"guest"))
        val manager=context.getSystemService(NotificationManager::class.java)
        compose.waitUntil(10000) { manager.activeNotifications.any { it.id==19 } }
        assertEquals(1,manager.activeNotifications.count { it.id==19 })
        assertEquals("Ton programme du Coran",manager.activeNotifications.single { it.id==19 }.notification.extras.getString("android.title"))
        assertTrue(context.getSharedPreferences("reminders",Context.MODE_PRIVATE).getLong("nextAt",0)>System.currentTimeMillis())
    }

    @Test fun testNotificationDoesNotReplaceLearningReminderOrNavigateToProgram()=fixture {
        val context=compose.activity
        assertTrue(LocalReminders.notify(context,"guest"));assertTrue(LocalReminders.notifyTest(context))
        val manager=context.getSystemService(NotificationManager::class.java)
        compose.waitUntil(10000) { manager.activeNotifications.count { it.id==19||it.id==20 }==2 }
        val test=manager.activeNotifications.single { it.id==20 }
        assertEquals("Test des notifications",test.notification.extras.getString("android.title"))
        test.notification.contentIntent.send()
        compose.waitForIdle()
        compose.onNodeWithText("Mon programme").assertDoesNotExist()
        assertTrue(context.getSharedPreferences("reminders",Context.MODE_PRIVATE).getBoolean("enabled",false))
    }
    @Test fun workManagerActuallyDeliversTheDelayedTestNotification()=fixture {
        val context=compose.activity
        val before=System.currentTimeMillis()
        assertTrue(NotificationTestWorker.enqueue(context,"guest"))
        val manager=context.getSystemService(NotificationManager::class.java)
        compose.waitUntil(30000) { manager.activeNotifications.any { it.id==20 } }
        assertTrue(System.currentTimeMillis()-before>=5000)
        assertEquals("Test des notifications",manager.activeNotifications.single { it.id==20 }.notification.extras.getString("android.title"))
    }

}
