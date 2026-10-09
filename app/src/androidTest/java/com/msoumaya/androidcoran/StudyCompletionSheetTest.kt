package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.StudyCompletionSheet
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class StudyCompletionSheetTest {
 @get:Rule val rule=createComposeRule()
 private val repo get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication).repository
 @Test fun explicitVerseSurvivesRestorationAndWholeLearningOverridesIt() {
  val q=repo.quran;var selected:Pair<Int,String>?=null
  val restore=StateRestorationTester(rule)
  restore.setContent { MaterialTheme { StudyCompletionSheet(q,repo.study,"learning",VerseRange(1,7),1,"coranTest",1,onClose={},onValidate={id,g->selected=id to g}) } }
  rule.onNodeWithContentDescription("Dernier verset appris").performScrollTo().performClick()
  rule.onNodeWithText("Verset 3").performClick()
  restore.emulateSavedInstanceStateRestore()
  rule.onNodeWithText("Arrêt exact : ${q.reference(VerseRange(1,3))}").assertExists()
  assertNull(selected)
  rule.onNodeWithText("Valider jusqu’au verset 3").performScrollTo().performClick()
  assertEquals(3 to "perfect",selected)
  rule.onNodeWithText("J’ai tout appris").performScrollTo().performClick()
  rule.onNodeWithContentDescription("Dernier verset appris").assertDoesNotExist()
  rule.onNodeWithText("Valider tout l’apprentissage").performScrollTo().performClick()
  assertEquals(7 to "perfect",selected)
 }
 @Test fun pageSelectionValidatesOnlyTheCompletedPageEndpoint() {
  val q=repo.quran;val range=VerseRange(1,q.pages[1].end);var selected:Int?=null
  rule.setContent { MaterialTheme { StudyCompletionSheet(q,repo.study,"learning",range,0,"traditional",1,onClose={},onValidate={id,_->selected=id}) } }
  rule.onNodeWithText("Page",substring=false).performScrollTo().performClick()
  rule.onNodeWithContentDescription("Dernière page apprise").assertExists()
  rule.onNodeWithText("Valider jusqu’à la page 1").performScrollTo().performClick()
  assertEquals(q.pages[0].end,selected)
 }
 @Test fun surahAndVerseChoicesUseLocalAyahNumbersWithinRemainingRange() {
  val q=repo.quran;var selected:Int?=null
  rule.setContent { MaterialTheme { StudyCompletionSheet(q,repo.study,"learning",VerseRange(6,10),6,"traditional",1,onClose={},onValidate={id,_->selected=id}) } }
  rule.onNodeWithContentDescription("Sourate").performScrollTo().performClick()
  rule.onNodeWithText("${q.surahs[1].name} (2)").performClick()
  rule.onNodeWithContentDescription("Dernier verset appris").performScrollTo().performClick()
  rule.onNodeWithText("Verset 2").performClick()
  rule.onNodeWithText("Valider jusqu’au verset 2").performScrollTo().performClick()
  assertEquals(9,selected)
 }
 @Test fun revisionGradeIsExplicitAndCancelDoesNotSubmit() {
  var selected:Pair<Int,String>?=null;var closed=0
  rule.setContent { MaterialTheme { StudyCompletionSheet(repo.quran,repo.study,"revision",VerseRange(1,7),0,"traditional",1,onClose={closed++},onValidate={id,g->selected=id to g}) } }
  rule.onNodeWithText("Annuler").performScrollTo().performClick()
  assertNull(selected);assertEquals(1,closed)
  rule.onNodeWithText("Quelques hésitations").performScrollTo().performClick()
  rule.onNodeWithText("Valider toute la révision").performScrollTo().performClick()
  assertEquals(7 to "hesitant",selected)
 }
}
