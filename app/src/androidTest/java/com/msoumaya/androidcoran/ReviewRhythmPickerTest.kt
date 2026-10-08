package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.ReviewRhythmPicker
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReviewRhythmPickerTest {
 @get:Rule val rule=createComposeRule()
 @Test fun quantityAndCycleChoicesClosePickerAndRestoreExpandedState() {
  var days:Int?=null;var quantity:String?=null
  val restore=StateRestorationTester(rule)
  restore.setContent { MaterialTheme { ReviewRhythmPicker(json("cycleDays" to 7),{days=it},{quantity=it}) } }
  rule.onNodeWithText("Cycle de 7 jours").assertExists()
  rule.onNodeWithText("1 Hizb / jour").assertDoesNotExist()
  rule.onNodeWithText("Modifier le rythme").performClick()
  restore.emulateSavedInstanceStateRestore()
  rule.onNodeWithText("1 Hizb / jour").performClick()
  assertEquals("hizb",quantity);assertNull(days)
  rule.onNodeWithText("1 Hizb / jour").assertDoesNotExist()
  rule.onNodeWithText("Modifier le rythme").performClick()
  rule.onNodeWithText("30 j").performClick()
  assertEquals(30,days);rule.onNodeWithText("30 j").assertDoesNotExist()
 }
 @Test fun quantitySummaryUsesPersistedBackendSettings() {
  rule.setContent { MaterialTheme { ReviewRhythmPicker(json("mode" to "quantity","dailyQuantity" to "juz2","cycleDays" to 7),{},{}) } }
  rule.onNodeWithText("2 Juz / jour").assertExists()
  rule.onNodeWithText("Cycle de 7 jours").assertDoesNotExist()
 }
}
