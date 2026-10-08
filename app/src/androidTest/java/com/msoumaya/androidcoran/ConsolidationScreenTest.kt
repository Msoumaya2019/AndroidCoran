package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
class ConsolidationScreenTest {
 @get:Rule val rule=createComposeRule()
 @Test fun futureConsolidationOpensReaderWithExplicitOffset() {
  val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication
  val vm=CoranViewModel(app);val at=LocalDate.now()
  val state=vm.repo.review.prepare(markKnowledge(defaultState().with("onboardingDone" to JsonPrimitive(true)),VerseRange(1,3),"perfect",at),at)
  var opened:ReviewTask?=null
  rule.setContent { MaterialTheme { RevisionScreen(vm,state,{opened=it}) } }
  rule.onNodeWithText("Nouveaux versets à consolider").assertExists()
  rule.onNodeWithText(vm.repo.quran.reference(VerseRange(1,3))).performClick()
  rule.runOnIdle { assertEquals(1,opened?.consolidationOffset);assertEquals(VerseRange(1,3),opened?.range) }
 }
 @Test fun readerUsesFullConsolidationInsteadOfPartialGrading() {
  val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication
  val vm=CoranViewModel(app);val state=markKnowledge(defaultState().with("onboardingDone" to JsonPrimitive(true)),VerseRange(1,3),"perfect")
  val task=ReviewTask("consolidation-1-3",VerseRange(1,3),"recent",LocalDate.now().plusDays(1).toString(),1)
  rule.setContent { MaterialTheme { ReaderScreen(vm,state,1,{},null,task) } }
  rule.onNodeWithText("Valider la consolidation · J+1").assertIsEnabled()
  rule.onNodeWithText("Valider jusqu’au verset sélectionné").assertDoesNotExist()
  rule.onNodeWithText("À retravailler").assertDoesNotExist()
 }
}
