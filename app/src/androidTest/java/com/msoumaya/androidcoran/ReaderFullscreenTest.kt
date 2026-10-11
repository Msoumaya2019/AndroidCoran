package com.msoumaya.androidcoran

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ReaderFullscreenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun barsHidden(): Boolean {
        var hidden=false
        compose.runOnUiThread {
            val insets=ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
            hidden=insets!=null&&!insets.isVisible(WindowInsetsCompat.Type.statusBars())&&!insets.isVisible(WindowInsetsCompat.Type.navigationBars())
        }
        return hidden
    }
    private fun assertFullscreen() {
        compose.onNodeWithContentDescription("Afficher les commandes").assertIsDisplayed()
        compose.onNodeWithText("Choisir une sourate ou une page").assertDoesNotExist()
        compose.waitUntil(10000) { barsHidden() }
        compose.waitUntil(10000) {
            var fillsWindow=false
            compose.runOnUiThread {
                fun find(view: android.view.View): android.view.View? {
                    if(view is com.msoumaya.androidcoran.ui.MushafView) return view
                    if(view is android.view.ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
                    return null
                }
                val decor=compose.activity.window.decorView
                val page=find(decor)
                fillsWindow=page!=null&&page.height>=decor.height-1&&page.width>=decor.width-1
            }
            fillsWindow
        }
    }
    private fun fixture(block: ()->Unit) {
        val repo=(compose.activity.application as CoranApplication).repository
        runBlocking { repo.awaitReady() }
        assertNull(repo.user.value)
        val saved=repo.state.value
        val today=LocalDate.now()
        val learning=json("id" to "fullscreen-learning","start" to 6236,"end" to 6236,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val known=markKnowledge(defaultState(),VerseRange(1,3),"perfect",today).with("sessions" to element(listOf(learning)),"lastRead" to json("verseId" to 1,"page" to 1))
        runBlocking { repo.mutate { repo.review.prepare(repo.review.setQuantity(known,"hizb",today),today) } }
        try { compose.waitForIdle();block() } finally {
            if(compose.onAllNodesWithContentDescription("Afficher les commandes").fetchSemanticsNodes().isNotEmpty()||compose.onAllNodesWithContentDescription("Masquer les commandes").fetchSemanticsNodes().isNotEmpty()) compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            runBlocking { repo.mutate { saved } }
        }
    }
    @Test fun freeReadingHidesBarsAndRestoresThemOnExit() = fixture {
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("1 / 604").assertExists()
        compose.onNodeWithText("Suivante").performClick()
        compose.onNodeWithText("2 / 604").assertExists()
        compose.onNodeWithContentDescription("Masquer les commandes").performClick()
        assertFullscreen()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Lire le Coran").assertExists()
        compose.waitUntil(10000) { !barsHidden() }
    }
    @Test fun learningKeepsValidationAvailableBehindFullscreenControls() = fixture {
        val repo=(compose.activity.application as CoranApplication).repository
        compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
        compose.onNodeWithText(repo.quran.reference(VerseRange(6236,6236))).performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Terminer mon apprentissage").assertExists()
        compose.onNodeWithContentDescription("Masquer les commandes").performClick()
        assertFullscreen()
    }
    @Test fun overdueLearningCanBeOpenedAndCompletedInFullscreen() = fixture {
        val repo=(compose.activity.application as CoranApplication).repository
        val yesterday=LocalDate.now().minusDays(1).toString()
        val id="catchup-learning"
        val session=json("id" to id,"start" to 6235,"end" to 6236,"date" to yesterday,"scheduledDate" to yesterday,"status" to "todo")
        runBlocking { repo.mutate { it.with("sessions" to element(listOf(session)),"studyProgress" to json()) } }
        compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
        compose.onNodeWithText("À rattraper").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("$yesterday · ${repo.quran.reference(VerseRange(6235,6236))}").performScrollTo().performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Terminer mon apprentissage").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("À rattraper").assertExists()
        assertEquals("todo",repo.state.value.arr("sessions").first().jsonObject.str("status"))
        assertEquals(yesterday,repo.state.value.arr("sessions").first().jsonObject.str("scheduledDate"))
        compose.onNodeWithText(yesterday+" · "+repo.quran.reference(VerseRange(6235,6236))).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Terminer mon apprentissage").performClick()
        compose.onNodeWithText("J’ai tout appris").performScrollTo().performClick()
        compose.onNodeWithText("Valider tout l’apprentissage").performScrollTo().performClick()
        compose.waitUntil(10000) { repo.state.value.arr("sessions").any { it.jsonObject.str("id")==id&&it.jsonObject.str("status")=="done" } }
        compose.onNodeWithText("À rattraper").assertDoesNotExist()
        assertTrue(known(repo.state.value,6235));assertTrue(known(repo.state.value,6236))
    }
    @Test fun revisionKeepsGradesAvailableBehindFullscreenControls() = fixture {
        val repo=(compose.activity.application as CoranApplication).repository
        compose.onNodeWithText("Révisions").performScrollTo().performClick()
        compose.onNodeWithText(repo.quran.reference(VerseRange(1,3))).performScrollTo().performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Quelques hésitations").assertExists()
        compose.onNodeWithText("À retravailler").assertExists()
        compose.onNodeWithText("Terminer mon apprentissage").assertDoesNotExist()
    }
    @Test fun recreationRestoresFullscreenReader() = fixture {
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        assertFullscreen()
        compose.activityRule.scenario.recreate()
        assertFullscreen()
    }
}

