package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.ReaderAudioControls
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReaderAudioControlsTest {
 @get:Rule val rule=createComposeRule()
 @Test fun reducedAndHiddenReaderRestoreWithoutStoppingPlayback() {
    val q=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository.quran
    val commands=mutableListOf<String>();var settings=0
    val restore=StateRestorationTester(rule)
    restore.setContent { MaterialTheme { ReaderAudioControls(q,AudioPosition(6236),false,AudioProgress(2300,1,1,0.5f),RepeatPreferences(),{settings++},{commands+=it}) } }
    rule.onNodeWithText("0:02").assertExists()
    rule.onNodeWithContentDescription("Réduire le lecteur").performClick()
    rule.onNodeWithContentDescription("Développer le lecteur").assertExists()
    rule.onNodeWithContentDescription("Verset suivant").performClick();assertEquals(listOf("NEXT_VERSE"),commands)
    rule.onNodeWithContentDescription("Masquer le lecteur").performClick()
    restore.emulateSavedInstanceStateRestore()
    rule.onNodeWithContentDescription("Rouvrir le lecteur audio").assertExists().performClick()
    rule.onNodeWithContentDescription("Développer le lecteur").performClick()
    rule.onNodeWithText("0:02").assertExists()
    rule.onNodeWithText("Audio").performClick();assertEquals(1,settings)
    assertFalse(commands.contains("STOP"))
 }
}
