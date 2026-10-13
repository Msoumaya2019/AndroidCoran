package com.msoumaya.androidcoran.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import com.msoumaya.androidcoran.domain.archiveResumeOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import com.msoumaya.androidcoran.domain.MushafArchive
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Downloads only the original Coran 1441 archive; each line is installed atomically. */
class QuranDownloadWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    companion object {
        const val NAME="quran-1441-download"
        const val SIZE=102608011L
        fun root(context: Context)=File(context.filesDir,"quran/coran_1441")
        fun ready(context: Context)=File(root(context),"ready-v1").isFile && (1..604).all { page -> (1..15).all { line -> File(root(context),"%03d-%02d.png".format(page,line)).isFile } }
        fun enqueue(context: Context) { WorkManager.getInstance(context).enqueueUniqueWork(NAME,ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<QuranDownloadWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,java.util.concurrent.TimeUnit.SECONDS).build()) }
    }
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if(ready(applicationContext)) return@withContext Result.success()
        val directory=root(applicationContext).apply { mkdirs() };val archive=File(directory,"download.zip.part")
        var extracting=false
        try {
            if(archive.length()>SIZE) check(archive.delete()) { "Archive temporaire invalide" }
            var downloaded=archive.length()
            if(downloaded!=SIZE) {
                val connection=(URL("https://files.quran.app/hafs/madani_1441/zips/images_1440.zip").openConnection() as java.net.HttpURLConnection).apply { connectTimeout=15000;readTimeout=30000;if(downloaded>0) setRequestProperty("Range","bytes=$downloaded-") }
                try {
                    val code=connection.responseCode
                    downloaded=archiveResumeOffset(code,connection.getHeaderField("Content-Range"),downloaded,SIZE)
                    java.io.FileOutputStream(archive,downloaded>0).use { output -> connection.inputStream.use { input -> val buffer=ByteArray(128*1024);while(true) { if(isStopped) return@withContext Result.retry();val n=input.read(buffer);if(n<0) break;output.write(buffer,0,n);downloaded+=n;check(downloaded<=SIZE);setProgress(workDataOf("phase" to "Téléchargement","percent" to (downloaded*100/SIZE).toInt())) } } }
                } finally { connection.disconnect() }
            }
            check(archive.length()==SIZE) { "Archive incomplète" }
            extracting=true
            val jobContext=coroutineContext
            MushafArchive(directory).install(archive,validate={file ->
                val options=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
                android.graphics.BitmapFactory.decodeFile(file.path,options)
                options.outWidth==1440&&options.outHeight==232
            },checkActive={jobContext.ensureActive();if(isStopped) throw CancellationException("Téléchargement suspendu")},progress={count ->
                if(count%30==0||count==9060) setProgressAsync(workDataOf("phase" to "Installation","percent" to count*100/9060))
            })
            archive.delete();Result.success()
        } catch(e: CancellationException) { throw e
        } catch(e: Exception) { if(extracting) archive.delete();if(runAttemptCount<4) Result.retry() else Result.failure(workDataOf("error" to (e.message?:"Téléchargement impossible"))) }
    }
}
