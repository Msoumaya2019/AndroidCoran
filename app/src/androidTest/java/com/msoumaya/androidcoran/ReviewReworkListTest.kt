package com.msoumaya.androidcoran
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.ReviewReworkList
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReviewReworkListTest {
 @get:Rule val rule=createComposeRule()
 @Test fun markedPassagesExpandRestoreAndOpenOriginalTask() {
  val q=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository.quran
  val tasks=listOf(1,3,5,7,9,11).map { ReviewTask("p-$it",VerseRange(it,it),"priority","2026-10-09") }
  var opened: ReviewTask?=null;val restore=StateRestorationTester(rule)
  restore.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { ReviewReworkList(q,tasks,{opened=it}) } } }
  rule.onNodeWithText(q.reference(tasks.last().range)).assertDoesNotExist()
  rule.onNodeWithText("Voir tous les versets prioritaires").performScrollTo().performClick()
  restore.emulateSavedInstanceStateRestore()
  rule.onNodeWithText(q.reference(tasks.last().range)).performScrollTo().performClick()
  assertEquals(tasks.last(),opened)
  rule.onNodeWithText("Réduire").performScrollTo().performClick()
  rule.onNodeWithText(q.reference(tasks.last().range)).assertDoesNotExist()
 }
}
