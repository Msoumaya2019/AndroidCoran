package com.msoumaya.androidcoran.data
import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import com.msoumaya.androidcoran.domain.*
@androidx.annotation.WorkerThread
fun decodeAvatar(context: Context,uri: Uri): Bitmap {
 val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
 context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
 require(bounds.outWidth>0&&bounds.outHeight>0) { "Image illisible" }
 val options=BitmapFactory.Options().apply { inSampleSize=1 };while(maxOf(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1024) options.inSampleSize*=2
 val bitmap=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }?:error("Image illisible")
 try {
   val orientation=try { context.contentResolver.openInputStream(uri).use { input->if(input==null) 1 else ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,1) } } catch(_: java.io.IOException) { 1 }
   return orientAvatar(bitmap,orientation)
 } catch(e: Exception) { bitmap.recycle();throw e }
}
fun orientAvatar(bitmap: Bitmap,orientation: Int): Bitmap {
 val matrix=Matrix()
 when(orientation) {
  2->matrix.setScale(-1f,1f)
  3->matrix.setRotate(180f)
  4->matrix.setScale(1f,-1f)
  5->{ matrix.setRotate(90f);matrix.postScale(-1f,1f) }
  6->matrix.setRotate(90f)
  7->{ matrix.setRotate(-90f);matrix.postScale(-1f,1f) }
  8->matrix.setRotate(-90f)
  else->return bitmap
 }
 val result=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
 if(result!==bitmap) bitmap.recycle()
 return result
}
fun encodeAvatar(bitmap: Bitmap,selection: AvatarCrop): ByteArray {
 val rect=avatarCropRect(bitmap.width,bitmap.height,selection)
 val cropped=Bitmap.createBitmap(bitmap,rect.left,rect.top,rect.side,rect.side)
 var scaled: Bitmap?=null
 try {
   val resized=Bitmap.createScaledBitmap(cropped,512,512,true);scaled=resized
   val bytes=java.io.ByteArrayOutputStream().use { output->check(resized.compress(Bitmap.CompressFormat.JPEG,65,output));output.toByteArray() }
   require(bytes.size<=2097152) { "Photo trop volumineuse" };return bytes
 } finally { if(scaled!==cropped) scaled?.recycle();if(cropped!==bitmap) cropped.recycle() }
}
