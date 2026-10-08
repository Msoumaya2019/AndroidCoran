package com.msoumaya.androidcoran
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.*
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.audio.QuranAudioCache
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
@androidx.annotation.OptIn(UnstableApi::class)
class QuranPrefetchTest {
 @Test fun publicVersePrefetchIsCompleteAndReadableWithoutAnyUpstream()=runBlocking {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    val repo=(context.applicationContext as CoranApplication).repository
    val url=verseAudioUrl(repo.quran.verse(6235),reciters.first { it.id=="ar.alafasy" })
    withTimeout(60000) { QuranAudioCache.prefetch(context,url) }
    val offline=DataSource.Factory { object: DataSource {
      override fun addTransferListener(listener: TransferListener) {}
      override fun open(spec: DataSpec): Long=error("Aucun accès réseau autorisé après préchargement")
      override fun read(buffer: ByteArray,offset: Int,length: Int): Int=error("Pas de lecture amont")
      override fun getUri(): android.net.Uri?=null
      override fun close() {}
    } }
    val source=QuranAudioCache.dataSourceFactory(context).setUpstreamDataSourceFactory(offline).createDataSource()
    try { source.open(DataSpec.Builder().setUri(url).build());var total=0L;val buffer=ByteArray(8192);while(true) { val read=source.read(buffer,0,buffer.size);if(read==C.RESULT_END_OF_INPUT) break;total+=read };assertTrue(total>1024) } finally { source.close() }
 }
}
