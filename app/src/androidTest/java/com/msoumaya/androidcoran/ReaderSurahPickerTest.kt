package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.ui.ReaderSurahPicker
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReaderSurahPickerTest {
 @get:Rule val rule=createComposeRule()
 private val q get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository.quran
 @Test fun pageBoundsAreCheckedAndInputRestores() {
  var page=0;val restore=StateRestorationTester(rule)
  restore.setContent { MaterialTheme { ReaderSurahPicker(q,1,"coranTest",{}, {page=it},{}) } }
  rule.onNodeWithText("Page 1 à 604").performTextReplacement("605")
  rule.onNodeWithText("Aller à la page").performClick();assertEquals(0,page)
  rule.onNodeWithText("Choisis une page entre 1 et 604.").assertExists()
  rule.onNodeWithText("Page 1 à 604").performTextReplacement("1.5")
  rule.onNodeWithText("Aller à la page").performClick();assertEquals(0,page)
  rule.onNodeWithText("Page 1 à 604").performTextReplacement("604")
  restore.emulateSavedInstanceStateRestore()
  rule.onNodeWithText("604").assertExists()
  rule.onNodeWithText("Aller à la page").performClick();assertEquals(604,page)
 }
 @Test fun selectingSurahUsesCanonicalFirstVerseAndCloseDoesNotNavigate() {
  var verse=0;var closed=false
  rule.setContent { MaterialTheme { ReaderSurahPicker(q,1,"coranTest",{closed=true},{},{verse=it}) } }
  rule.onNodeWithText(q.surahs[1].name).performClick();assertEquals(q.surahs[1].range.start,verse)
  rule.onNodeWithText("Fermer").performClick();assertTrue(closed)
 }
}
