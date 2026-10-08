package com.msoumaya.androidcoran

import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.data.query
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate

class SupabasePublicIntegrationTest {
    @Test fun nativeSdkReadsPublicContentFromExistingProjectWithoutCachedFallback() {
        assumeTrue(BuildConfig.SUPABASE_PUBLIC_KEY.isNotBlank())
        val repo=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository
        runBlocking {
            withTimeout(30000) {
                android.util.Log.i("PublicIntegration", "Initialisation du SDK")
                repo.awaitReady()
                android.util.Log.i("PublicIntegration", "Lecture RPC publique")
                val rows=repo.rpc("daily_content_for_date",json("p_date" to LocalDate.now().toString()),authenticated=false).jsonArray
                assertTrue(rows.all { it is JsonObject })
                android.util.Log.i("PublicIntegration", "Lecture des catégories")
                val categories=repo.query("content_categories",eq=mapOf("is_active" to "true"),orderBy="display_order",size=5,authenticated=false)
                assertTrue(categories.all { it.str("id").isNotBlank() })
            }
        }
    }
}
