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
import com.msoumaya.androidcoran.ui.StudyResumeCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class StudyResumeCardTest {
 @get:Rule val rule=createComposeRule()
 private val repo get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository
 @Test fun verseDetailsAndResumeUseSavedProgress() {
  var resumes=0
  val record=json("id" to "x","mode" to "learning","start" to 1,"end" to 7,"through" to 3,"source" to "traditional")
  rule.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { StudyResumeCard(repo.quran,repo.study,record,{resumes++}) } } }
  rule.onNodeWithText("4 versets restants").assertExists()
  rule.onNodeWithText("3 / 7 versets · 43 %").assertExists()
  rule.onNodeWithText("Reprendre mon apprentissage").performClick();assertEquals(1,resumes)
  rule.onAllNodesWithText("Appris").assertCountEquals(3)
  rule.onAllNodesWithText("À apprendre").assertCountEquals(4)
  rule.onNodeWithText("Voir dans le Coran").performScrollTo().performClick();assertEquals(2,resumes)
 }
 @Test fun longRevisionPageDetailsExpandAndRestoreWithoutSubmitting() {
  val q=repo.quran;val record=json("id" to "r","mode" to "revision","start" to 1,"end" to q.pages[19].end,"through" to q.pages[1].start,"source" to "traditional")
  var resumes=0;val restore=StateRestorationTester(rule)
  restore.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { StudyResumeCard(q,repo.study,record,{resumes++}) } } }
  rule.onNodeWithText("Page 13").assertDoesNotExist()
  rule.onNodeWithText("À continuer").assertExists()
  rule.onNodeWithText("Voir tout").performScrollTo().performClick()
  rule.onNodeWithText("Page 20").assertExists()
  restore.emulateSavedInstanceStateRestore()
  rule.onNodeWithText("Page 20").assertExists()
  rule.onNodeWithText("Réduire").performScrollTo().performClick()
  rule.onNodeWithText("Page 13").assertDoesNotExist();assertEquals(0,resumes)
  rule.onNodeWithText("Reprendre ma révision").performScrollTo().performClick();assertEquals(1,resumes)
 }
}
