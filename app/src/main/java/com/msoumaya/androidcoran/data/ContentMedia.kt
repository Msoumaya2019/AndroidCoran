package com.msoumaya.androidcoran.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ContentMedia(private val repo: Repository) {
    suspend fun resolve(url: String): String {
        val path=contentMediaPath(url,SUPABASE_URL)?:return url
        val owner=repo.user.value
        val signed=repo.db().storage.from("daily-content-media").createSignedUrl(path,kotlin.time.Duration.parse("1h"))
        check(owner==repo.user.value) { "Le compte a changé" }
        return signed
    }
    suspend fun upload(context: Context,uri: Uri,kind: String): String {
        check(AdminService(repo).authorized());val owner=repo.user.value?:error("Connexion nécessaire")
        val type=context.contentResolver.getType(uri)
        val extension=if(kind=="image") when(type) { "image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";else->error("Choisis une image JPEG, PNG ou WebP") } else {
            val name=context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else "" }?:""
            name.substringAfterLast('.').lowercase().also { require(it in listOf("mp3","m4a","aac")) { "Choisis un fichier MP3, M4A ou AAC" } }
        }
        val mime=if(kind=="image") type!! else when(extension) { "mp3"->"audio/mpeg";"aac"->"audio/aac";else->"audio/mp4" }
        val limit=if(kind=="image") 5*1024*1024 else 30*1024*1024
        val bytes=withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input);val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true) { val read=input.read(buffer);if(read<0) break;require(output.size()+read<=limit) { "Fichier trop volumineux" };output.write(buffer,0,read) };output.toByteArray() } }
        check(owner==repo.user.value);val path="$owner/${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension"
        repo.db().storage.from("daily-content-media").upload(path,bytes) { contentType=ContentType.parse(mime) }
        check(owner==repo.user.value)
        return "$SUPABASE_URL/storage/v1/object/public/daily-content-media/$path"
    }
    suspend fun cleanUnused(urls: List<String>) {
        check(AdminService(repo).authorized())
        urls.distinct().forEach { url -> val path=contentMediaPath(url,SUPABASE_URL)?:return@forEach
            val image=repo.query("daily_contents",mapOf("image_url" to url),size=1)
            val audio=repo.query("daily_contents",mapOf("audio_url" to url),size=1)
            if(image.isEmpty()&&audio.isEmpty()) repo.db().storage.from("daily-content-media").delete(path)
        }
    }
}
