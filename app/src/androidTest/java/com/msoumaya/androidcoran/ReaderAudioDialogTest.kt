package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.data.AudioPreferences
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReaderAudioDialogTest {
 @get:Rule val rule=createComposeRule()
 @Test fun nativeControlsSaveCustomCountSpeedGapAndAutoStop() {
    val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication
    val store=AudioPreferences(app);val before=runBlocking { store.load() };runBlocking { store.save(RepeatPreferences()) }
    val visible=mutableStateOf(true)
    try {
      val vm=CoranViewModel(app)
      rule.setContent { MaterialTheme { if(visible.value) ReaderAudioDialog(vm,VerseRange(1,7),VerseRange(1,7),"ar.alafasy",{}, {}) } }
      rule.waitUntil(5000) { rule.onAllNodes(isEnabled() and hasText("Lire")).fetchSemanticsNodes().isNotEmpty() }
      rule.onNodeWithText("Nombre personnalisé").performScrollTo().performClick()
      rule.onNodeWithText("Nombre personnalisé (1 à 999)").performScrollTo().performTextReplacement("23")
      rule.onNodeWithText("1.25×").performScrollTo().performClick().assertIsSelected()
      rule.onNodeWithText("5s").performScrollTo().performClick().assertIsSelected()
      rule.onNodeWithText("Arrêter à la fin des écoutes").performScrollTo()
      rule.onNode(isToggleable() and isOn()).performClick()
      rule.waitForIdle()
      try { runBlocking { withTimeout(5000) { while(store.load()!=RepeatPreferences("custom","23",RepeatMode.PASSAGE,5,1.25f,false)) delay(50) } } } catch(e: Exception) { throw AssertionError("Réglages enregistrés : ${runBlocking { store.load() }}",e) }
      assertEquals(23,runBlocking { store.load().count })
    } finally { rule.runOnIdle { visible.value=false };rule.waitForIdle();runBlocking { store.save(before) } }
 }
}
