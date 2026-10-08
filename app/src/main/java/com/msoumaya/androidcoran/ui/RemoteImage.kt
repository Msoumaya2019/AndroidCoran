package com.msoumaya.androidcoran.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Composable fun RemoteImage(url: String,description: String,modifier: Modifier=Modifier,scale: ContentScale=ContentScale.Fit) {
    val bitmap by produceState<Bitmap?>(null,url) {
        value=null
        if(!url.startsWith("https://")) return@produceState
        value=withContext(Dispatchers.IO) {
            val connection=URL(url).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout=15000;connection.readTimeout=15000
                if(connection.responseCode!=200) return@withContext null
                val bytes=connection.inputStream.use { input ->
                    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(output.size()<=8*1024*1024) { val count=input.read(buffer);if(count<0) break;output.write(buffer,0,count) }
                    output.toByteArray()
                }
                if(bytes.size>8*1024*1024) return@withContext null
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                val options=BitmapFactory.Options();while(maxOf(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1024) options.inSampleSize*=2
                BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            } catch(e: Exception) { if(e is CancellationException) throw e;null } finally { connection.disconnect() }
        }
    }
    bitmap?.let { Image(it.asImageBitmap(),description,modifier,contentScale=scale) }
}
