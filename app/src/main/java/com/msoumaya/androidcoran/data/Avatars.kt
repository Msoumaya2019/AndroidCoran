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
suspend fun Repository.uploadAvatar(context: Context,uri: Uri) {
    val owner=user.value?:error("Connexion nécessaire")
    val bytes=withContext(Dispatchers.IO) {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth>0&&bounds.outHeight>0) { "Image illisible" }
        val options=BitmapFactory.Options().apply { inSampleSize=1 };while(maxOf(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1024) options.inSampleSize*=2
        val original=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }?:error("Image illisible")
        val side=minOf(original.width,original.height)
        val crop=Bitmap.createBitmap(original,(original.width-side)/2,(original.height-side)/2,side,side)
        val scaled=Bitmap.createScaledBitmap(crop,512,512,true)
        val result=ByteArrayOutputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG,70,output);output.toByteArray() }
        if(scaled!==crop) scaled.recycle();if(crop!==original) crop.recycle();original.recycle()
        require(result.size<=2_097_152) { "Photo trop volumineuse" };result
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
