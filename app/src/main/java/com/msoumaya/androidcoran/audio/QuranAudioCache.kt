package com.msoumaya.androidcoran.audio

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.*

/** Process-wide cache: only public Quran media enters this factory. */
@UnstableApi
object QuranAudioCache {
    private var instance: SimpleCache? = null
    @Synchronized private fun cache(context: Context): SimpleCache = instance ?: SimpleCache(
        File(context.applicationContext.cacheDir, "quran-audio-v1"),
        LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024),
        StandaloneDatabaseProvider(context.applicationContext)
    ).also { instance = it }

    fun dataSourceFactory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context.applicationContext))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    suspend fun prefetch(context: Context,url: String) = withContext(Dispatchers.IO) {
        require(url.startsWith("https://"))
        val job=currentCoroutineContext().job
        lateinit var writer: CacheWriter
        writer=CacheWriter(dataSourceFactory(context).createDataSource(),DataSpec.Builder().setUri(url).build(),ByteArray(64*1024),CacheWriter.ProgressListener { _,_,_->if(!job.isActive) writer.cancel() })
        try { writer.cache() } catch(e: Exception) { currentCoroutineContext().ensureActive();throw e }
        currentCoroutineContext().ensureActive()
    }

    fun factory(context: Context): DefaultMediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory(context))
}
