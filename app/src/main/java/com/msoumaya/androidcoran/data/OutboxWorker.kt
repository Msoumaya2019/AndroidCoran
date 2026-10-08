package com.msoumaya.androidcoran.data

import android.content.Context
import androidx.work.*
import com.msoumaya.androidcoran.CoranApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class OutboxWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val repo=(applicationContext as CoranApplication).repository;repo.awaitReady()
        val owner=inputData.getString("owner")?:return Result.failure()
        if(repo.user.value!=owner) return Result.success()
        var failed=false
        suspend fun attempt(block: suspend ()->Unit) { try { block() } catch(e: Exception) { if(e is CancellationException) throw e;failed=true } }
        attempt { repo.sync() }
        if(repo.quiz.pending(owner)) attempt { repo.quiz.refresh() }
        attempt { repo.reports.flush() }
        return if(failed||repo.pendingState(owner)||repo.quiz.pending(owner)||repo.reports.pending(owner)) Result.retry() else Result.success()
    }
    companion object {
        fun enqueue(context: Context,owner: String) {
            val request=OneTimeWorkRequestBuilder<OutboxWorker>().setInputData(workDataOf("owner" to owner))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("sync-outbox-$owner",ExistingWorkPolicy.KEEP,request)
        }
    }
}
