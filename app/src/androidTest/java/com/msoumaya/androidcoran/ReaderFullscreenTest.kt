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
        compose.onNodeWithText("Révisions").performScrollTo().performClick()
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
        fun view(): com.msoumaya.androidcoran.ui.MushafView? {
            fun find(v: android.view.View): com.msoumaya.androidcoran.ui.MushafView? {
                if(v is com.msoumaya.androidcoran.ui.MushafView) return v
                if(v is android.view.ViewGroup) for(i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
                return null
            }
            return find(compose.activity.window.decorView)
        }
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        compose.waitUntil(10000) { var ready=false;compose.runOnUiThread { ready=(view()?.tag as? Triple<*,*,*>)?.first!=null };ready }
        val existingIds=readerInk(repo.state.value,1).map { it.id }.toSet()
        compose.onNodeWithContentDescription("Annoter au stylet").performClick()
        compose.waitUntil(10000) { var enabled=false;compose.runOnUiThread { enabled=view()?.annotationMode==true };enabled }
        compose.runOnUiThread {
            val target=view()!!;val down=android.os.SystemClock.uptimeMillis()
            for(i in 0..24) {
                val angle=i*2*Math.PI/24
                val x=target.width*.5f+(kotlin.math.cos(angle)*target.width*.1).toFloat()
                val y=target.height*.4f+(kotlin.math.sin(angle)*target.height*.04).toFloat()
                val action=when(i) { 0->android.view.MotionEvent.ACTION_DOWN;24->android.view.MotionEvent.ACTION_UP;else->android.view.MotionEvent.ACTION_MOVE }
                val event=android.view.MotionEvent.obtain(down,down+i*10,action,1,arrayOf(android.view.MotionEvent.PointerProperties().apply { id=0;toolType=android.view.MotionEvent.TOOL_TYPE_STYLUS }),arrayOf(android.view.MotionEvent.PointerCoords().apply { this.x=x;this.y=y;pressure=.8f;size=.1f }),0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_STYLUS,0)
                try { target.dispatchTouchEvent(event) } finally { event.recycle() }
            }
        }
        compose.waitUntil(10000) { readerInk(repo.state.value,1).any { it.id !in existingIds } }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Zone de note manuscrite").fetchSemanticsNodes().isNotEmpty() }
        val id=readerInk(repo.state.value,1).single { it.id !in existingIds }.id
        compose.onNodeWithContentDescription("Zone de note manuscrite").performTouchInput { swipe(centerLeft,centerRight,300) }
        compose.waitUntil(10000) { readerInk(repo.state.value,1).single { it.id==id }.note.isNotEmpty() }
        compose.onNodeWithText("Fermer",substring=false).performClick()
        compose.activityRule.scenario.recreate();assertFullscreen()
        assertEquals(id,readerInk(repo.state.value,1).single { it.id==id }.id)
        assertTrue(readerInk(repo.state.value,1).single { it.id==id }.note.isNotEmpty())
        val annotations=readerInk(repo.state.value,1);val ink=annotations.single { it.id==id }
        val label="Annotation "+(annotations.indexOf(ink)+1)+(if(ink.verses.isNotEmpty()) " · "+repo.quran.reference(VerseRange(ink.verses.min(),ink.verses.max())) else "")
        compose.onNodeWithContentDescription("Annotations de cette page ("+annotations.size+")").performClick()
        compose.onNodeWithText(label,substring=false).performClick()
        compose.onNodeWithText("Supprimer l’annotation").performClick()
        compose.onNodeWithText("Supprimer",substring=false).performClick()
        compose.waitUntil(10000) { readerInk(repo.state.value,1).none { it.id==id } }
        assertTrue(repo.state.value.obj("readerInk").obj(id).str("deletedAt").isNotBlank())
    }
    @Test fun recreationRestoresFullscreenReader() = fixture {
        compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
        assertFullscreen()
        compose.activityRule.scenario.recreate()
        assertFullscreen()
    }
}
