package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReaderStudyActionsTest {
 @get:Rule val rule=createComposeRule()
 @Test fun difficultyToggleChangesLabelAndKeepsOtherVerseActions() {
  val q=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository.quran
  var bookmark=0;var listen=0;var close=0;var toggles=0
  rule.setContent { var marked by remember { mutableStateOf(false) };MaterialTheme { SelectedVerseActions(q,6236,marked,{bookmark++},{listen++},{marked=!marked;toggles++},{close++}) } }
  rule.onNodeWithText("Marquer comme difficile").performClick()
  rule.onNodeWithText("Retirer des révisions prioritaires").assertExists().performClick()
  rule.onNodeWithText("Marquer comme difficile").assertExists()
  rule.onNodeWithText("Marquer").performClick();rule.onNodeWithText("Écouter").performClick();rule.onNodeWithText("Fermer").performClick()
  assertEquals(2,toggles);assertEquals(1,bookmark);assertEquals(1,listen);assertEquals(1,close)
 }
 @Test fun allThreeRevisionGradesReachDistinctCallbacks() {
  val grades=mutableListOf<String>()
  rule.setContent { MaterialTheme { ReviewValidationActions { grades+=it } } }
  rule.onNodeWithText("Quelques hésitations").performClick()
  rule.onNodeWithText("À retravailler").performClick()
  rule.onNodeWithText("Valider jusqu’au verset sélectionné").performClick()
  assertEquals(listOf("hesitant","rework","perfect"),grades)
 }
}
