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
  try { compose.waitForIdle();block() } catch(e: Throwable) { captureReaderState("tablet",compose.onRoot(useUnmergedTree=true).printToString());throw e } finally {
   if(compose.onAllNodesWithContentDescription("Afficher les commandes").fetchSemanticsNodes().isNotEmpty()) compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
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
  compose.onNodeWithTag("tablet-reader-actions").assertIsDisplayed()
  val density=compose.activity.resources.displayMetrics.density;val decor=compose.activity.window.decorView
  val sidebars=decor.width/density>=1000&&decor.height/density>=480&&decor.width>decor.height
  compose.onNodeWithTag(if(sidebars) "tablet-tools-vertical" else "tablet-tools-floating").assertIsDisplayed()
  compose.onNodeWithText("Mes annotations",substring=false).performScrollTo().performClick()
  compose.onNodeWithTag(if(sidebars) "tablet-notes-sidebar" else "tablet-notes-overlay").assertIsDisplayed()
  captureReaderState("tablet-reader",compose.onRoot(useUnmergedTree=true).printToString())
  compose.onNodeWithText("Fermer",substring=false).performClick()
  compose.onNodeWithText("Lecture plein écran",substring=false).performScrollTo().performClick()
  compose.onNodeWithTag("tablet-reader-actions").assertDoesNotExist();compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist();compose.onNodeWithTag("tablet-tools-vertical").assertDoesNotExist()
  compose.activityRule.scenario.recreate();compose.onNodeWithTag("tablet-reader-actions").assertDoesNotExist()
  compose.onNodeWithText("Quitter la lecture plein écran").performClick();compose.onNodeWithTag("tablet-reader-actions").assertIsDisplayed()
 }
 @Test fun doublePageUsesEvenLeftOddRightAndSinglePageInPortrait() = fixture {
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithText("Double page",substring=false).performScrollTo().performClick();compose.waitUntil(10000) { (compose.activity.application as CoranApplication).repository.state.value.obj("reader").obj("tablet").str("spread")=="double" };compose.waitForIdle()
  val decor=compose.activity.window.decorView;val d=compose.activity.resources.displayMetrics.density
  val expected=tabletLayout(TabletPreferences(mode="on",spread="double"),"tablet",decor.width/d,decor.height/d).doublePage
  compose.onNodeWithTag("printed-page-1").assertExists()
  if(expected) { compose.waitUntil(10000) { compose.onAllNodesWithTag("printed-page-2").fetchSemanticsNodes().isNotEmpty() };compose.onNodeWithTag("printed-page-2").assertExists();assertTrue(compose.onNodeWithTag("printed-page-2").fetchSemanticsNode().boundsInRoot.left<compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot.left) }
  else compose.onNodeWithTag("printed-page-2").assertDoesNotExist()
  captureReaderState("tablet-spread",compose.onRoot(useUnmergedTree=true).printToString())
 }
 @Test fun notebookFiltersAndMasteryPreserveActualMemorization() = fixture {
  val repo=(compose.activity.application as CoranApplication).repository
  val ink=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f),InkPoint(.3f,.3f))),listOf(1)).copy(text="Ma note tablette",favorite=true)
  runBlocking { repo.mutate { saveReaderInk(it,ink) } }
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithText("Mon cahier de révision",substring=false).performScrollTo().performClick()
  compose.onNodeWithText("Ma note tablette").assertExists()
  compose.onNodeWithText("Difficiles",substring=false).performClick();compose.onNodeWithText("Aucune annotation pour ces filtres").assertIsDisplayed()
  compose.onNodeWithText("Difficiles",substring=false).performClick()
  compose.onNodeWithText("Passage maîtrisé",substring=false).performScrollTo().performClick()
  compose.waitUntil(10000) { allReaderInk(repo.state.value).firstOrNull { it.id==ink.id }?.masteredAt!=null }
  assertFalse(known(repo.state.value,1))
  compose.onNodeWithText("Page (1–604)",substring=false).performTextReplacement("2")
  compose.onNodeWithText("Aucune annotation pour ces filtres").assertIsDisplayed()
  compose.onNodeWithText("Fermer",substring=false).performClick()
 }

}
