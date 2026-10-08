package com.msoumaya.androidcoran
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.audio.RecitationService
import com.msoumaya.androidcoran.data.AudioPreferences
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
class AudioRepeatPauseTest {
 @Test fun settingsRemainOnDiskAfterReopeningStore() = runBlocking {
    val context=InstrumentationRegistry.getInstrumentation().targetContext;val store=AudioPreferences(context);val previous=store.load()
    try { val settings=RepeatPreferences("custom","7",RepeatMode.EACH_VERSE,5,0.75f,false);store.save(settings);assertEquals(settings,AudioPreferences(context).load()) } finally { store.save(previous) }
 }
 @Test fun pausingDuringRepeatGapDoesNotLoseNextRepeatOrContinueWhilePaused() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario->
      scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("PLAY_RANGE").putExtra("start",6236).putExtra("end",6236).putExtra("reciter","ar.alafasy").putExtra("count",2).putExtra("each",true).putExtra("gap",2).putExtra("speed",1.25f)) }
      try {
        runBlocking { withTimeout(60000) { RecitationService.waiting.first { it } } }
        scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("TOGGLE")) }
        runBlocking { withTimeout(5000) { RecitationService.playing.first { !it } };delay(2500) }
        assertEquals(1,RecitationService.current.value!!.repetition)
        assertEquals(1,RecitationService.passageProgress.value!!.verseIndex)
        scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("TOGGLE")) }
        runBlocking { withTimeout(10000) { assertEquals(2,RecitationService.current.first { it?.repetition==2 }!!.repetition) } }
      } finally { scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction("STOP")) } }
    }
 }
 @Test fun replacingAPausedGapCancelsTheOldRangeAndStopClearsWaiting() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario->
      fun command(action:String,start:Int=6236,end:Int=6236) { scenario.onActivity { it.startService(Intent(it,RecitationService::class.java).setAction(action).putExtra("start",start).putExtra("end",end).putExtra("reciter","ar.alafasy").putExtra("count",2).putExtra("each",true).putExtra("gap",5)) } }
      try {
        command("PLAY_RANGE")
        runBlocking { withTimeout(60000) { RecitationService.waiting.first { it } } }
        command("TOGGLE")
        runBlocking { withTimeout(5000) { RecitationService.playing.first { !it } } }
        command("PLAY_RANGE",6235,6235)
        runBlocking { withTimeout(10000) { RecitationService.current.first { it?.verseId==6235 };RecitationService.waiting.first { !it } } }
        assertEquals(1,RecitationService.current.value!!.repetition)
        command("STOP")
        runBlocking { withTimeout(5000) { RecitationService.current.first { it==null } } }
        assertFalse(RecitationService.waiting.value)
      } finally { command("STOP") }
    }
 }

}
