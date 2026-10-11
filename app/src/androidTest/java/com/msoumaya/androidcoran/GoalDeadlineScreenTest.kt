package com.msoumaya.androidcoran
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class GoalDeadlineScreenTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun deadlineSurvivesRotationSavesAndCanBeClearedWithoutLosingMetadata() {
    val repo=(compose.activity.application as CoranApplication).repository
    runBlocking { repo.awaitReady() };assertNull(repo.user.value)
    val saved=repo.state.value
    runBlocking { repo.mutate { defaultState().with("goal" to defaultState().obj("goal").with("extension" to json("keep" to true))) } }
    try {
      compose.onNodeWithText("Mon objectif").performScrollTo().performClick()
      compose.onNodeWithText("Échéance (facultative)").performScrollTo().performTextReplacement("2026-02-29")
      compose.onNodeWithText("Indique une date valide au format AAAA-MM-JJ.").assertExists()
      compose.onNodeWithText("Enregistrer et créer mon programme").performScrollTo().performClick()
      compose.waitForIdle();assertEquals("",repo.state.value.obj("goal").str("deadline"))
      compose.onNodeWithText("Échéance (facultative)").performScrollTo().performTextReplacement("2028-02-29")
      compose.activityRule.scenario.recreate();compose.waitForIdle()
      compose.onNodeWithText("2028-02-29").assertExists()
      compose.onNodeWithText("Enregistrer et créer mon programme").performScrollTo().performClick()
      compose.waitUntil(10000) { repo.state.value.obj("goal").str("deadline")=="2028-02-29" }
      assertTrue(repo.state.value.obj("goal").obj("extension").flag("keep"))
      compose.onNodeWithText("Sans date").performScrollTo().performClick()
      compose.onNodeWithText("Enregistrer et créer mon programme").performScrollTo().performClick()
      compose.waitUntil(10000) { "deadline" !in repo.state.value.obj("goal") }
      assertTrue(repo.state.value.obj("goal").obj("extension").flag("keep"))
    } finally { runBlocking { repo.mutate { saved } } }
 }
}
