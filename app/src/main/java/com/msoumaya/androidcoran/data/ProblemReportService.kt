package com.msoumaya.androidcoran.data

import android.content.Context
import android.net.Uri
import com.msoumaya.androidcoran.BuildConfig
import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.util.UUID

val problemTypes=listOf("Bug","Affichage","Audio","Notification","Autre")
class ProblemReportService(private val repo: Repository,private val store: LocalStore,private val context: Context) {
    private val lock=Mutex()
    fun pending(owner: String)=store.pending(owner,"report").isNotEmpty()
    suspend fun queue(type: String,description: String,attachment: Uri?): String {
        val owner=repo.user.value?:error("Connecte-toi pour envoyer un signalement")
        val text=description.trim();require(type in problemTypes&&text.length in 1..500) { "Décris le problème en 500 caractères maximum" }
        val id=UUID.randomUUID().toString();var file: File?=null;var mime: String?=null;var queued=false
        try {
            if(attachment!=null) withContext(Dispatchers.IO) {
                mime=context.contentResolver.getType(attachment);require(mime in listOf("image/jpeg","image/png")) { "Choisis une image JPEG ou PNG" }
                val directory=File(context.filesDir,"problem-reports/$owner").apply { mkdirs() }
                file=File(directory,"$id.${if(mime=="image/png") "png" else "jpg"}")
                context.contentResolver.openInputStream(attachment).use { input -> requireNotNull(input);file!!.outputStream().use { output -> val buffer=ByteArray(8192);var size=0;while(true) { val read=input.read(buffer);if(read<0) break;size+=read;require(size<=5*1024*1024) { "La capture doit faire moins de 5 Mo" };output.write(buffer,0,read) } } }
            }
            check(repo.user.value==owner) { "Le compte a changé" }
            val payload=json("id" to id,"user_id" to owner,"type" to type,"description" to text,"screenshot_path" to file?.let { "$owner/${it.name}" },"app_version" to BuildConfig.VERSION_NAME,"platform" to "android","created_at" to Instant.now().toString(),"status" to "open")
            store.enqueue(QueuedOperation(id,owner,"report",payload,file?.path,mime));queued=true;OutboxWorker.enqueue(context,owner);return id
        } catch(e: Exception) { if(!queued) file?.delete();throw e }
    }
    suspend fun flush() = lock.withLock {
        val owner=repo.user.value?:return@withLock
        for(operation in store.pending(owner,"report")) {
            check(repo.user.value==owner) { "Le compte a changé" }
            val path=operation.payload.str("screenshot_path")
            if(operation.attachment!=null&&path.isNotBlank()) {
                val already=try { repo.db().storage.from("problem-report-screenshots").createSignedUrl(path,kotlin.time.Duration.parse("5m"));true } catch(e: Exception) { if(e is CancellationException) throw e;false }
                if(!already) repo.db().storage.from("problem-report-screenshots").upload(path,File(operation.attachment).readBytes()) { contentType=ContentType.parse(operation.mime?:"image/jpeg") }
            }
            check(repo.user.value==owner) { "Le compte a changé" }
            val existing=repo.query("app_problem_reports",eq=mapOf("id" to operation.id,"user_id" to owner),size=1)
            if(existing.isEmpty()) repo.insert("app_problem_reports",operation.payload)
            check(repo.query("app_problem_reports",eq=mapOf("id" to operation.id,"user_id" to owner),size=1).isNotEmpty()) { "Confirmation du signalement en attente" }
            check(repo.user.value==owner) { "Le compte a changé" }
            store.acknowledge(owner,operation.id);operation.attachment?.let { File(it).delete() }
        }
    }
}
