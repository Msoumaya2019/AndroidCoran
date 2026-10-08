package com.msoumaya.androidcoran.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

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
        try {
            var downloaded=archive.length()
            if(downloaded!=SIZE) {
                val connection=(URL("https://files.quran.app/hafs/madani_1441/zips/images_1440.zip").openConnection() as java.net.HttpURLConnection).apply { connectTimeout=15000;readTimeout=30000;if(downloaded>0) setRequestProperty("Range","bytes=$downloaded-") }
                try {
                    val code=connection.responseCode
                    check(code==200||code==206) { "Réponse de téléchargement $code" }
                    if(code==200) downloaded=0 else check(connection.getHeaderField("Content-Range")?.startsWith("bytes $downloaded-")==true) { "Reprise de téléchargement incohérente" }
                    java.io.FileOutputStream(archive,downloaded>0).use { output -> connection.inputStream.use { input -> val buffer=ByteArray(128*1024);while(true) { if(isStopped) return@withContext Result.retry();val n=input.read(buffer);if(n<0) break;output.write(buffer,0,n);downloaded+=n;check(downloaded<=SIZE);setProgress(workDataOf("phase" to "Téléchargement","percent" to (downloaded*100/SIZE).toInt())) } } }
                } finally { connection.disconnect() }
            }
            check(archive.length()==SIZE) { "Archive incomplète" }
            val installed=mutableSetOf<String>();var expanded=0L
            ZipInputStream(archive.inputStream().buffered()).use { zip -> while(true) { if(isStopped) return@withContext Result.retry();val entry=zip.nextEntry?:break;val match=Regex("^width_1440/(\\d+)/(\\d+)\\.png$").matchEntire(entry.name);if(!entry.isDirectory&&match!=null) { val page=match.groupValues[1].toInt();val line=match.groupValues[2].toInt();val name="%03d-%02d.png".format(page,line);check(page in 1..604&&line in 1..15&&installed.add(name));val file=File(directory,name);val temp=File(directory,"$name.tmp");var bytes=0L;temp.outputStream().use { output -> val buffer=ByteArray(65536);while(true) { val n=zip.read(buffer);if(n<0) break;bytes+=n;expanded+=n;check(bytes<=8_000_000&&expanded<=1_000_000_000);output.write(buffer,0,n) } };val options=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true };android.graphics.BitmapFactory.decodeFile(temp.path,options);check(options.outWidth==1440&&options.outHeight==232);check(temp.renameTo(file));setProgress(workDataOf("phase" to "Installation","percent" to installed.size*100/9060)) };zip.closeEntry() } }
            check(installed.size==9060);File(directory,"ready-v1").writeText("9060");archive.delete();Result.success()
        } catch(e: Exception) { if(runAttemptCount<4) Result.retry() else Result.failure(workDataOf("error" to (e.message?:"Téléchargement impossible"))) }
    }
}
