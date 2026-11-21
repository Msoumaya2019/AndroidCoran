package com.msoumaya.androidcoran
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class TabletLayoutTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun fixture(block: ()->Unit) {
  val repo=(compose.activity.application as CoranApplication).repository;runBlocking { repo.awaitReady() };assertNull(repo.user.value);val saved=repo.state.value
  runBlocking { repo.mutate { defaultState().with("reader" to defaultState().obj("reader").with("tablet" to TabletPreferences(mode="on").toJson()),"lastRead" to json("verseId" to 1,"page" to 1)) } }
  try { compose.waitForIdle();block() } catch(e: Throwable) { runCatching { captureReaderState("tablet",compose.onAllNodes(isRoot(),useUnmergedTree=true).onFirst().printToString()) };throw e } finally {
   if(compose.onAllNodesWithContentDescription("Afficher les commandes").fetchSemanticsNodes().isNotEmpty()) compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
   runBlocking { repo.mutate { latest -> allReaderInk(latest).filter { ink -> allReaderInk(saved).none { it.id==ink.id } }.fold(latest) { acc,ink -> deleteReaderInk(acc,ink.id,annotationTime(ink.updatedAt)) } } }
            runBlocking { repo.mutate { saved.with("reader" to saved.obj("reader").with("tablet" to TabletPreferences.from(saved.obj("reader").obj("tablet")).toJson())) } }
  }
 }
 @Test fun navigationRetainsReferenceTabsRatherThanPreviousSideRail() = fixture {
  compose.onNodeWithTag("tablet-navigation").assertDoesNotExist()
  compose.onNodeWithText("Programme",useUnmergedTree=true).performClick();compose.onNodeWithText("Mon programme").assertExists()
  compose.activityRule.scenario.recreate();compose.waitUntil(10000) { compose.onNode(hasText("Programme") and hasClickAction()).isDisplayed() };compose.onNode(hasText("Programme") and hasClickAction()).assertIsDisplayed();compose.onNodeWithText("Mon programme").assertExists()
 }
 @Test fun readerToolsAndNotesFollowWindowOrientationAndHideInFullscreen() = fixture {
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithTag("tablet-compact-header").assertIsDisplayed()
  val h=compose.onNodeWithTag("tablet-compact-header").fetchSemanticsNode().boundsInRoot.height/compose.activity.resources.displayMetrics.density
  assertTrue(h<=100f);compose.onNodeWithTag("tablet-header-navigation").assertIsDisplayed();compose.onNodeWithTag("tablet-header-tabs").assertIsDisplayed()
  compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist();compose.onNodeWithTag("tablet-tools-vertical").assertDoesNotExist()
  val before=compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot
  compose.onNodeWithTag("tablet-tab-annoter").performClick();compose.onNodeWithTag("tablet-tools-floating").assertIsDisplayed()
  compose.onNodeWithTag("tablet-tab-lecture").performClick();compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist();assertEquals(before,compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot)
  compose.onNodeWithTag("tablet-tab-notes").performClick();compose.onNodeWithTag("tablet-notes-overlay").assertIsDisplayed()
  compose.onNodeWithText("Fermer",substring=false).performClick()
  captureReaderState("tablet-compact-final",compose.onRoot(useUnmergedTree=true).printToString())
  compose.onNodeWithContentDescription("Lecture plein écran").performClick();compose.onNodeWithTag("tablet-compact-header").assertDoesNotExist()
  compose.activityRule.scenario.recreate();compose.onNodeWithTag("tablet-compact-header").assertDoesNotExist()
  compose.onNodeWithContentDescription("Quitter la lecture plein écran").performClick();compose.onNodeWithTag("tablet-compact-header").assertIsDisplayed()
 }
 @Test fun doublePageUsesEvenLeftOddRightAndSinglePageInPortrait() = fixture {
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithTag("tablet-reader-options").performClick();compose.onNodeWithText("Double page",substring=false).performClick();compose.waitUntil(10000) { (compose.activity.application as CoranApplication).repository.state.value.obj("reader").obj("tablet").str("spread")=="double" };compose.waitForIdle()
  val decor=compose.activity.window.decorView;val d=compose.activity.resources.displayMetrics.density
  val expected=tabletLayout(TabletPreferences(mode="on",spread="double"),"tablet",decor.width/d,decor.height/d).doublePage
  compose.onNodeWithTag("printed-page-1").assertExists()
  if(expected) { compose.waitUntil(10000) { compose.onAllNodesWithTag("printed-page-2").fetchSemanticsNodes().isNotEmpty() };compose.onNodeWithTag("printed-page-2").assertExists();assertTrue(compose.onNodeWithTag("printed-page-2").fetchSemanticsNode().boundsInRoot.left<compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot.left) }
  else compose.onNodeWithTag("printed-page-2").assertDoesNotExist()
  captureReaderState("tablet-spread",compose.onRoot(useUnmergedTree=true).printToString())
 }
 @Test fun notebookFiltersAndMasteryPreserveActualMemorization() = fixture {
  val repo=(compose.activity.application as CoranApplication).repository
  val ink=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f),InkPoint(.3f,.3f))),listOf(1)).copy(text="Ma note tablette "+System.nanoTime(),favorite=true)
  runBlocking { repo.mutate { saveReaderInk(it,ink) } }
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithTag("tablet-tab-revisions").performClick()
  compose.onNodeWithText(ink.text).assertExists()
  compose.onNodeWithText("Difficiles",substring=false).performClick();compose.onNodeWithText("Aucune annotation pour ces filtres").assertIsDisplayed()
  compose.onNodeWithText("Difficiles",substring=false).performClick()
  compose.onNode(hasText("Passage maîtrisé") and hasAnyAncestor(hasText(ink.text))).performScrollTo().performClick()
  compose.waitUntil(10000) { allReaderInk(repo.state.value).firstOrNull { it.id==ink.id }?.masteredAt!=null }
  assertFalse(known(repo.state.value,1))
  compose.onNodeWithText("Page (1–604)",substring=false).performTextReplacement("2")
  compose.onNodeWithText("Aucune annotation pour ces filtres").assertIsDisplayed()
  compose.onNodeWithText("Fermer",substring=false).performClick()
 }

 @Test fun fullscreenCanEnableFingerDrawingAndPreservesItsPreference() = fixture {
  val repo=(compose.activity.application as CoranApplication).repository
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithTag("tablet-reader-options").performClick();compose.onNodeWithText("Autoriser le doigt",substring=false).performClick()
  compose.waitUntil(10000) {!TabletPreferences.from(repo.state.value.obj("reader").obj("tablet")).stylusOnly}
  compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist()
  compose.onNodeWithTag("tablet-tab-annoter").performClick();compose.onNodeWithContentDescription("Sélection").performClick();compose.onNodeWithTag("tablet-tools-floating").assertIsDisplayed()
  compose.onNodeWithContentDescription("Lecture plein écran").performClick();compose.activityRule.scenario.recreate();compose.onNodeWithTag("tablet-tools-floating").assertIsDisplayed()
  compose.onNodeWithContentDescription("Quitter la lecture plein écran").performClick();compose.onNodeWithTag("tablet-tab-lecture").performClick();compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist()
 }
 @Test fun disabledTabletModeKeepsTheSmartphoneReaderControls() = fixture {
  val repo=(compose.activity.application as CoranApplication).repository
  runBlocking {repo.mutate {it.with("reader" to it.obj("reader").with("tablet" to TabletPreferences(mode="off").toJson()))}}
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick();compose.onNodeWithTag("tablet-compact-header").assertDoesNotExist()
  compose.onNodeWithContentDescription("Afficher les commandes").performClick();compose.onNodeWithText("Choisir une sourate ou une page").assertIsDisplayed();compose.onNodeWithText("Français",substring=false).assertIsDisplayed()
 }


}
