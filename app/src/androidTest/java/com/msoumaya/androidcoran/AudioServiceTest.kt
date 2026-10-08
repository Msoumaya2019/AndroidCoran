package com.msoumaya.androidcoran

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msoumaya.androidcoran.audio.RecitationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioServiceTest {
    @Test fun media3ReallyPlaysAndRepeatsTheRequestedVerse() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("PLAY_RANGE").putExtra("start",6236).putExtra("end",6236).putExtra("reciter","ar.alafasy").putExtra("count",3).putExtra("each",true)) }
            try { runBlocking { withTimeout(60000) { RecitationService.playing.first { it };val position=RecitationService.current.first { it?.repetition==3 };assertEquals(6236,position!!.verseId) } } }
            finally { scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("STOP")) } }
        }
    }
}
