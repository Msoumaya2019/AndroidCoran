package com.msoumaya.androidcoran

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.audio.QuranAudioCache
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioCacheTest {
    @Test fun fullyReadMediaRemainsAvailableWhenUpstreamDisappears() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val source=File.createTempFile("cache-test-", ".mp3",context.cacheDir)
        val bytes=ByteArray(32768) { (it % 251).toByte() }
        source.writeBytes(bytes)
        val spec=DataSpec.Builder().setUri(Uri.fromFile(source)).build()
        fun read(): ByteArray {
            val input=QuranAudioCache.dataSourceFactory(context).createDataSource()
            val output=ByteArrayOutputStream()
            try {
                input.open(spec)
                val buffer=ByteArray(4096)
                while(true) { val count=input.read(buffer,0,buffer.size);if(count==C.RESULT_END_OF_INPUT) break;output.write(buffer,0,count) }
                return output.toByteArray()
            } finally { input.close() }
        }
        try {
            assertArrayEquals(bytes,read())
            check(source.delete())
            assertArrayEquals(bytes,read())
        } finally { source.delete() }
    }
}
