package com.msoumaya.androidcoran
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
@RunWith(AndroidJUnit4::class)
class TabletLayoutTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun tabletUsesSideNavigationAndBoundsItsContentAfterRecreation() {
  val repo=(compose.activity.application as CoranApplication).repository;runBlocking { repo.awaitReady() }
  compose.onNodeWithTag("tablet-navigation").assertIsDisplayed()
  val bounds=compose.onNodeWithTag("app-content").fetchSemanticsNode().boundsInRoot
  val density=compose.activity.resources.displayMetrics.density
  assertTrue(bounds.width<=1040*density+2)
  compose.onNodeWithText("Programme",useUnmergedTree=true).performClick();compose.onNodeWithText("Mon programme").assertExists()
  compose.activityRule.scenario.recreate();compose.onNodeWithTag("tablet-navigation").assertIsDisplayed();compose.onNodeWithText("Mon programme").assertExists()
 }
 @Test fun tabletReaderHidesNavigationAndKeepsHandwritingEntry() {
  val repo=(compose.activity.application as CoranApplication).repository;runBlocking { repo.awaitReady() }
  compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
  compose.onNodeWithTag("tablet-navigation").assertDoesNotExist()
  compose.onNodeWithContentDescription("Annoter au stylet").assertIsDisplayed()
  compose.onNodeWithContentDescription("Afficher les commandes").assertIsDisplayed()
  compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
  compose.onNodeWithTag("tablet-navigation").assertIsDisplayed()
 }
}
