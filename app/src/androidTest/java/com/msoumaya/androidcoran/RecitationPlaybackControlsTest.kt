package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.msoumaya.androidcoran.ui.RecitationPlaybackControls
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class RecitationPlaybackControlsTest {
 @get:Rule val rule=createComposeRule()
 @Test fun inactiveRecordingStartsItsOwnFileAndCannotSeekOtherAudio() {
  var plays=0;var toggles=0;val seeks=mutableListOf<Long>()
  rule.setContent { MaterialTheme { RecitationPlaybackControls(false,true,45000,60000,{plays++},{toggles++},{seeks+=it}) } }
  rule.onNodeWithText("Position : 0:00 / 1:00").assertExists();rule.onNodeWithText("− 10 s").assertIsNotEnabled();rule.onNodeWithText("+ 10 s").assertIsNotEnabled()
  rule.onNodeWithText("Réécouter").performClick();assertEquals(1,plays);assertEquals(0,toggles);assertTrue(seeks.isEmpty())
 }
 @Test fun activeRecordingOffersPauseAndBothTenSecondSeeks() {
  var plays=0;var toggles=0;val seeks=mutableListOf<Long>()
  rule.setContent { MaterialTheme { RecitationPlaybackControls(true,true,15000,60000,{plays++},{toggles++},{seeks+=it}) } }
  rule.onNodeWithText("Position : 0:15 / 1:00").assertExists();rule.onNodeWithText("Pause").performClick();rule.onNodeWithText("− 10 s").performClick();rule.onNodeWithText("+ 10 s").performClick()
  assertEquals(0,plays);assertEquals(1,toggles);assertEquals(listOf(-10000L,10000L),seeks)
 }
 @Test fun pausedRecordingResumesWithoutReloadingItsFile() {
  var plays=0;var toggles=0
  rule.setContent { MaterialTheme { RecitationPlaybackControls(true,false,15000,60000,{plays++},{toggles++},{}) } }
  rule.onNodeWithText("Reprendre").performClick();assertEquals(0,plays);assertEquals(1,toggles)
 }
}
