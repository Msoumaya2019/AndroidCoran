package com.msoumaya.androidcoran.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds

suspend fun Repository.signedAvatar(path: String)=db().storage.from("friend-avatars").createSignedUrl(path,60.seconds)
suspend fun Repository.uploadAvatar(context: Context,uri: Uri,selection: AvatarCrop=AvatarCrop(),expectedOwner: String?=user.value) {
    val owner=user.value?:error("Connexion nécessaire")
    check(owner==expectedOwner) { "Le compte a changé" }
    val bytes=withContext(Dispatchers.IO) {
        val bitmap=decodeAvatar(context,uri)
        try { encodeAvatar(bitmap,selection) } finally { bitmap.recycle() }
    }
    check(owner==user.value) { "Le compte a changé" }
    val path="$owner/avatar.jpg"
    db().storage.from("friend-avatars").upload(path,bytes) { upsert=true;contentType=ContentType.Image.JPEG }
    check(owner==user.value) { "Le compte a changé" }
    rpc("ensure_social_profile")
    updateRows("friend_profiles",json("avatar_path" to path),mapOf("id" to owner))
}
suspend fun Repository.removeAvatar() {
    val owner=user.value?:error("Connexion nécessaire")
    updateRows("friend_profiles",json("avatar_path" to null),mapOf("id" to owner))
    db().storage.from("friend-avatars").delete("$owner/avatar.jpg")
}
