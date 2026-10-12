package com.msoumaya.androidcoran.domain
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AvatarAccount(val id: String,val email: String)
fun avatarEmail(email: String)=email.trim().lowercase(Locale.ROOT)
fun validAvatarEmail(email: String)=Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+").matches(avatarEmail(email))
class PendingAvatars(private val directory: File) {
 private val syncLock=Mutex()
 private fun hash(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
 private fun path(email: String): File { require(validAvatarEmail(email));return File(directory,hash(avatarEmail(email).toByteArray(Charsets.UTF_8))+".jpg") }
 @Synchronized fun save(email: String,bytes: ByteArray) {
  require(bytes.size in 4..2097152&&bytes[0]==0xff.toByte()&&bytes[1]==0xd8.toByte()) { "Photo JPEG invalide" }
  val destination=path(email);check(directory.isDirectory||directory.mkdirs())
  val temporary=File.createTempFile("pending-",".tmp",directory)
  try {
   temporary.outputStream().use { it.write(bytes);it.flush() }
   try { Files.move(temporary.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING) }
   catch(_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING) }
  } finally { temporary.delete() }
 }
 @Synchronized fun load(email: String): ByteArray? {
  if(!validAvatarEmail(email)) return null
  val file=path(email);if(!file.isFile) return null
  require(file.length() in 4..2097152) { "Photo locale invalide" }
  return file.readBytes()
 }
 @Synchronized fun remove(email: String) { if(validAvatarEmail(email)) { val file=path(email);check(!file.exists()||file.delete()) } }
 @Synchronized private fun acknowledge(email: String,bytes: ByteArray) { val current=load(email);if(current!=null&&hash(current)==hash(bytes)) remove(email) }
 suspend fun sync(expected: AvatarAccount,current: ()->AvatarAccount?,upload: suspend (ByteArray,String)->Unit): Boolean = syncLock.withLock {
  fun matches(): Boolean { val actual=current()?:return false;return actual.id==expected.id&&avatarEmail(actual.email)==avatarEmail(expected.email) }
  if(!validAvatarEmail(expected.email)||!matches()) return@withLock false
  val bytes=load(expected.email)?:return@withLock false
  if(!matches()) return@withLock false
  upload(bytes,expected.id)
  if(!matches()) return@withLock false
  acknowledge(expected.email,bytes);true
 }
}
