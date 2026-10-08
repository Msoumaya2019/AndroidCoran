package com.msoumaya.androidcoran

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class AuthLinkIntentTest {
    @Test fun existingRedirectOpensNativeActivityAndRejectsExpiredLinkWithoutRetainingItsData() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val intent=Intent(Intent.ACTION_VIEW,Uri.parse("coranmemoire://auth#error=expired&error_description=dummy-private-value")).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals(MainActivity::class.java.name,context.packageManager.resolveActivity(intent,0)?.activityInfo?.name)
        context.startActivity(intent)
        val repo=(context.applicationContext as CoranApplication).repository
        val notice=runBlocking { withTimeout(10000) { repo.notice.first { it.startsWith("Lien expiré") } } }
        assertFalse(notice.contains("dummy-private-value"))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().single()
            try { assertNull(activity.intent.data) } finally { activity.finish() }
        }
        instrumentation.waitForIdleSync()
    }
}
