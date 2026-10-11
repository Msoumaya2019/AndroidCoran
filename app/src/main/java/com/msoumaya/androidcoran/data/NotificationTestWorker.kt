package com.msoumaya.androidcoran.data

import android.content.Context
import androidx.work.*
import com.msoumaya.androidcoran.CoranApplication
import com.msoumaya.androidcoran.domain.canOpenLearningReminder
import java.util.concurrent.TimeUnit

class NotificationTestWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val owner=inputData.getString("account")?:return Result.success()
        val repo=(applicationContext as CoranApplication).repository
        repo.awaitReady()
        if(canOpenLearningReminder(owner,repo.user.value)) LocalReminders.notifyTest(applicationContext)
        return Result.success()
    }
    companion object {
        const val NAME="local-notification-test"
        fun enqueue(context: Context,account: String): Boolean {
            if(!LocalReminders.permissionGranted(context)) return false
            val request=OneTimeWorkRequestBuilder<NotificationTestWorker>().setInputData(workDataOf("account" to account)).setInitialDelay(5,TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME,ExistingWorkPolicy.REPLACE,request)
            return true
        }
    }
}
