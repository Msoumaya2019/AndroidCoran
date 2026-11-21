package com.msoumaya.androidcoran

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
        var windowMetrics=""
        try { compose.waitUntil(10000) {
            var fillsWindow=false
            compose.runOnUiThread {
                fun find(view: android.view.View): android.view.View? {
                    if(view is com.msoumaya.androidcoran.ui.MushafView) return view
                    if(view is android.view.ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
                    return null
                }
                val decor=compose.activity.window.decorView
                val page=find(decor)
                windowMetrics="decor="+decor.width+"x"+decor.height+" page="+page?.width+"x"+page?.height+" destroyed="+compose.activity.isDestroyed+" parents="+generateSequence(page as android.view.View?) { it.parent as? android.view.View }.joinToString("/") { it.javaClass.simpleName+":"+it.width+"x"+it.height+":"+it.paddingTop+","+it.paddingBottom }
                fillsWindow=page!=null&&page.height>=decor.height-1&&page.width>=decor.width-1
            }
            fillsWindow
        } } catch(e: Throwable) { var active="";compose.activityRule.scenario.onActivity { active=" actual="+it.window.decorView.width+"x"+it.window.decorView.height+" same="+(it===compose.activity) };throw AssertionError("Fullscreen bounds: "+windowMetrics+active,e) }
    }
    private fun fixture(block: ()->Unit) {
        val repo=(compose.activity.application as CoranApplication).repository
        runBlocking { repo.awaitReady() }
        assertNull(repo.user.value)
        val saved=repo.state.value
        val today=LocalDate.now()
        val learning=json("id" to "fullscreen-learning","start" to 6236,"end" to 6236,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val known=markKnowledge(defaultState().with("reader" to defaultState().obj("reader").with("tablet" to TabletPreferences(mode="off").toJson())),VerseRange(1,3),"perfect",today).with("sessions" to element(listOf(learning)),"lastRead" to json("verseId" to 1,"page" to 1))
        runBlocking { repo.mutate { repo.review.prepare(repo.review.setQuantity(known,"hizb",today),today) } }
        try { compose.waitForIdle();block() } catch(e: Throwable) { runCatching { captureReaderState("reader",compose.onAllNodes(isRoot(),useUnmergedTree=true).onFirst().printToString()) };throw e } finally {
            if(compose.onAllNodesWithContentDescription("Afficher les commandes").fetchSemanticsNodes().isNotEmpty()||compose.onAllNodesWithContentDescription("Masquer les commandes").fetchSemanticsNodes().isNotEmpty()) compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            runBlocking { repo.mutate { latest -> allReaderInk(latest).filter { ink -> allReaderInk(saved).none { it.id==ink.id } }.fold(latest) { acc,ink -> deleteReaderInk(acc,ink.id,annotationTime(ink.updatedAt)) } } }
            runBlocking { repo.mutate { saved.with("reader" to saved.obj("reader").with("tablet" to TabletPreferences.from(saved.obj("reader").obj("tablet")).toJson())) } }
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
        compose.onNodeWithTag("programme-list").performScrollToNode(hasText("Ouvrir la lecture du jour"))
        compose.onNodeWithText("Ouvrir la lecture du jour").performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Terminer mon apprentissage").assertExists()
        compose.onNodeWithContentDescription("Masquer les commandes").performClick()
        assertFullscreen()
    }
    @Test fun programmeDailyReviewOpensFullscreenWithGrades() = fixture {
        compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
        compose.onNodeWithTag("programme-list").performScrollToNode(hasTestTag("programme-daily-review"))
        compose.onNodeWithTag("programme-daily-review").performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Quelques hésitations").assertExists()
        compose.onNodeWithText("À retravailler").assertExists()
        compose.onNodeWithText("Terminer mon apprentissage").assertDoesNotExist()
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
        // Exercise the card’s accessibility action while daily content may still relayout.
        compose.onNodeWithText("Révisions").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Révision quotidienne").assertExists()
        compose.waitUntil(10000) { compose.onAllNodesWithText(repo.quran.reference(VerseRange(1,3))).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(repo.quran.reference(VerseRange(1,3))).performScrollTo().performClick()
        assertFullscreen()
        compose.onNodeWithContentDescription("Afficher les commandes").performClick()
        compose.onNodeWithText("Quelques hésitations").assertExists()
        compose.onNodeWithText("À retravailler").assertExists()
        compose.onNodeWithText("Terminer mon apprentissage").assertDoesNotExist()
    }
    @Test fun handwrittenOutlineAndNotePersistThroughReaderRecreationAndCanBeDeleted() = fixture {
        val repo=(compose.activity.application as CoranApplication).repository
        runBlocking { repo.mutate { it.with("reader" to it.obj("reader").with("tablet" to TabletPreferences(mode="on").toJson())) } }
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        compose.waitUntil(10000) { var ready=false;compose.runOnUiThread { ready=(findReaderView<com.msoumaya.androidcoran.ui.MushafView>(compose.activity.window.decorView)?.tag as? Triple<*,*,*>)?.first!=null };ready }
        val existingIds=readerInk(repo.state.value,1).map { it.id }.toSet()
        compose.onNodeWithText("Annoter",substring=false).performScrollTo().performClick()
        compose.waitUntil(10000) { var enabled=false;compose.runOnUiThread { enabled=findReaderView<com.msoumaya.androidcoran.ui.MushafView>(compose.activity.window.decorView)?.annotationMode==true };enabled };compose.waitForIdle()
        compose.runOnUiThread { val target=findReaderView<com.msoumaya.androidcoran.ui.MushafView>(compose.activity.window.decorView)!!;sendReaderStylus(target,listOf(.4f to .3f,.6f to .3f,.6f to .4f,.4f to .4f,.4f to .3f)) }
        compose.waitUntil(10000) { readerInk(repo.state.value,1).any { it.id !in existingIds } }
        val id=readerInk(repo.state.value,1).single { it.id !in existingIds }.id
        compose.onNodeWithText("Mes annotations",substring=false).performScrollTo().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("reader-annotation-"+id))
        compose.onNode(hasText("Page 1") and hasAnyAncestor(hasTestTag("reader-annotation-"+id)),useUnmergedTree=true).performClick()
        compose.onNodeWithText("Modifier",substring=false).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Zone de note manuscrite").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Autoriser le doigt ou un stylet non reconnu").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Zone de note manuscrite").performTouchInput { swipe(centerLeft,centerRight,300) }
        compose.onNodeWithText("Note personnelle").performScrollTo().performClick()
        compose.onNodeWithText("Enregistrer",substring=false).performClick()
        compose.waitUntil(10000) { readerInk(repo.state.value,1).single { it.id==id }.note.isNotEmpty() }
        compose.onNodeWithText("Lecture plein écran",substring=false).performScrollTo().performClick()
        compose.activityRule.scenario.recreate();assertFullscreen()
        assertTrue(readerInk(repo.state.value,1).single { it.id==id }.note.isNotEmpty())
        compose.onNodeWithText("Quitter la lecture plein écran").performClick()
        compose.onNodeWithText("Mes annotations",substring=false).performScrollTo().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("reader-annotation-"+id))
        compose.onNode(hasText("Page 1") and hasAnyAncestor(hasTestTag("reader-annotation-"+id)),useUnmergedTree=true).performClick()
        compose.onNodeWithText("Modifier",substring=false).performClick()
        compose.onNodeWithContentDescription("Supprimer l’annotation").performClick()
        compose.onNodeWithText("Supprimer",substring=false).performClick()
        compose.waitUntil(10000) { readerInk(repo.state.value,1).none { it.id==id } }
        assertTrue(repo.state.value.obj("readerAnnotations").obj(id).str("deletedAt").isNotBlank())
    }
    @Test fun recreationRestoresFullscreenReader() = fixture {
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        assertFullscreen()
        compose.activityRule.scenario.recreate()
        assertFullscreen()
    }
}
