package com.msoumaya.androidcoran

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msoumaya.androidcoran.domain.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun readerRendersAndNavigatesToAnotherPage() {
        val repo=(compose.activity.application as CoranApplication).repository
        kotlinx.coroutines.runBlocking { repo.awaitReady();repo.mutate { it.with("lastRead" to json("verseId" to 1,"page" to 1)) } }
        compose.waitForIdle()
        compose.onNodeWithText("Lire le Coran").performClick()
        compose.onNodeWithText("1 / 604").assertExists()
        compose.onNodeWithText("Suivante").performClick()
        compose.onNodeWithText("2 / 604").assertExists()
    }
    @Test fun canonicalAssetsAndNativeFontsAreReadableOnAndroid() {
        val context=compose.activity
        val q=Quran(context)
        assertEquals(6236,q.verses.size)
        for(page in listOf(1,2,100,604)) { assertEquals(page,q.qcfData(context,page).num("page"));assertNotNull(android.graphics.Typeface.createFromAsset(context.assets,"fonts/$page.ttf")) }
        assertNotNull(android.graphics.BitmapFactory.decodeStream(context.assets.open("mushaf/page604.png")))
    }
    @Test fun localAccountStoresRemainSeparateAfterRestart() {
        val store=com.msoumaya.androidcoran.data.LocalStore(compose.activity)
        val a=defaultState().with("userId" to kotlinx.serialization.json.JsonPrimitive("test-a"))
        val b=defaultState().with("userId" to kotlinx.serialization.json.JsonPrimitive("test-b"))
        store.save("test-a",com.msoumaya.androidcoran.data.StoredState(a,null,null,true))
        store.save("test-b",com.msoumaya.androidcoran.data.StoredState(b,null,null,false))
        store.close()
        val reopened=com.msoumaya.androidcoran.data.LocalStore(compose.activity)
        assertEquals(a,reopened.load("test-a")!!.data);assertTrue(reopened.load("test-a")!!.pending)
        assertEquals(b,reopened.load("test-b")!!.data);assertFalse(reopened.load("test-b")!!.pending)
        reopened.close()
    }
    @Test fun revisionDoesNotReusePreviousLearningSession() {
        val repo=(compose.activity.application as CoranApplication).repository
        assertNull(repo.user.value)
        var saved=defaultState()
        val today=java.time.LocalDate.now()
        val learning=json("id" to "navigation-learning","start" to 6236,"end" to 6236,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val known=markKnowledge(defaultState(),VerseRange(1,3),"perfect",today).with("sessions" to element(listOf(learning)))
        kotlinx.coroutines.runBlocking { repo.awaitReady();saved=repo.state.value;repo.mutate { repo.review.prepare(repo.review.setQuantity(known,"hizb",today),today) } }
        try {
            compose.waitForIdle()
            compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
            compose.onNodeWithText(repo.quran.reference(VerseRange(6236,6236))).performClick()
            compose.onNodeWithText("Terminer mon apprentissage").assertExists()
            compose.onNodeWithText("Quelques hésitations").assertDoesNotExist()
            compose.onNodeWithContentDescription("Retour").performClick()
            compose.onNodeWithText("Accueil",useUnmergedTree=true).performClick()
            compose.onNodeWithText("Révisions").performScrollTo().performClick()
            compose.onNodeWithText(repo.quran.reference(VerseRange(1,3))).performScrollTo().performClick()
            compose.onNodeWithText("Quelques hésitations").assertExists()
            compose.onNodeWithText("À retravailler").assertExists()
        } finally { compose.waitForIdle();kotlinx.coroutines.runBlocking { repo.mutate { saved } } }
    }
    @Test fun partialLearningPersistsAndResumesAtNextVerse() {
        val repo=(compose.activity.application as CoranApplication).repository
        assertNull(repo.user.value)
        val today=java.time.LocalDate.now();var saved=defaultState()
        val session=json("id" to "partial-learning","start" to 1,"end" to 7,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val initial=defaultState().with("sessions" to element(listOf(session)),"goal" to json("ranges" to listOf(json("start" to 1,"end" to 7))))
        kotlinx.coroutines.runBlocking { repo.awaitReady();saved=repo.state.value;repo.mutate { initial } }
        try {
            compose.waitForIdle()
            compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
            compose.onNodeWithText(repo.quran.reference(VerseRange(1,7))).performClick()
            compose.onNodeWithText("Terminer mon apprentissage").performClick()
            compose.onNodeWithContentDescription("Dernier verset appris").performScrollTo().performClick()
            compose.onNodeWithText("Verset 3").performClick()
            compose.onNodeWithText("Valider jusqu’au verset 3").performScrollTo().performClick()
            compose.waitUntil(10000) { repo.state.value.obj("studyProgress").obj("learning:partial-learning").num("through")==3 }
            compose.onNodeWithText("Apprentissage à continuer").assertExists();compose.onNodeWithText("Reprendre mon apprentissage").performScrollTo().performClick()
            compose.onNodeWithText("Terminer mon apprentissage").performClick()
            compose.onNodeWithContentDescription("Dernier verset appris").performScrollTo().performClick()
            compose.onNodeWithText("Verset 3").assertDoesNotExist()
            compose.onNodeWithText("Verset 4").assertExists()
            val store=com.msoumaya.androidcoran.data.LocalStore(compose.activity)
            try { assertEquals(3,store.load("guest")!!.data.obj("studyProgress").obj("learning:partial-learning").num("through")) } finally { store.close() }
        } finally { compose.waitForIdle();kotlinx.coroutines.runBlocking { repo.mutate { saved } } }
    }

    @Test fun readerCanDeferUnstartedAndPartialLearningWithoutLosingProgress() {
        val repo=(compose.activity.application as CoranApplication).repository
        assertNull(repo.user.value)
        val today=java.time.LocalDate.now();var saved=defaultState()
        val session=json("id" to "defer-learning","start" to 1,"end" to 7,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val initial=defaultState().with("sessions" to element(listOf(session)),"goal" to json("ranges" to listOf(json("start" to 1,"end" to 7))))
        kotlinx.coroutines.runBlocking { repo.awaitReady();saved=repo.state.value;repo.mutate { initial } }
        try {
            compose.waitForIdle();compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
            compose.onNodeWithText(repo.quran.reference(VerseRange(1,7))).performClick()
            compose.onNodeWithText("Je dois encore le travailler").performClick()
            compose.waitUntil(10000) { repo.state.value.arr("sessions")[0].toString().contains("postponed") }
            assertFalse(known(repo.state.value,1));assertTrue(repo.state.value.obj("studyProgress").isEmpty())
            val partial=repo.program.complete(initial,"defer-learning",3,today)
            kotlinx.coroutines.runBlocking { repo.mutate { partial } }
            compose.waitForIdle();compose.onNodeWithText("Reprendre mon apprentissage").performScrollTo().performClick()
            compose.onNodeWithText("Reporter cette séance").performClick()
            compose.onNodeWithText("Apprentissage à continuer").assertExists()
            assertEquals(partial.obj("studyProgress"),repo.state.value.obj("studyProgress"))
            assertEquals(partial.arr("sessions"),repo.state.value.arr("sessions"))
            compose.onNodeWithText("Reprendre mon apprentissage").performScrollTo().performClick()
            compose.onNodeWithText("Terminer mon apprentissage").assertExists()
        } finally { compose.waitForIdle();kotlinx.coroutines.runBlocking { repo.mutate { saved } } }
    }

    @Test fun changingSurahClearsLearningContextAndDirectPageKeepsIt() {
        val repo=(compose.activity.application as CoranApplication).repository;assertNull(repo.user.value)
        val today=java.time.LocalDate.now();var saved=defaultState()
        val session=json("id" to "surah-navigation","start" to 1,"end" to 7,"date" to today.toString(),"scheduledDate" to today.toString(),"status" to "todo")
        val initial=defaultState().with("sessions" to element(listOf(session)))
        kotlinx.coroutines.runBlocking { repo.awaitReady();saved=repo.state.value;repo.mutate { initial } }
        try {
            compose.waitForIdle();compose.onNodeWithText("Programme",useUnmergedTree=true).performClick()
            compose.onNodeWithText(repo.quran.reference(VerseRange(1,7))).performClick()
            compose.onNodeWithText("Choisir une sourate ou une page").performClick()
            compose.onNodeWithText("Page 1 à 604").performTextReplacement("3")
            compose.onNodeWithText("Aller à la page").performClick()
            compose.onNodeWithText("3 / 604").assertExists();compose.onNodeWithText("Terminer mon apprentissage").assertExists()
            compose.onNodeWithText("Choisir une sourate ou une page").performClick()
            compose.onNodeWithText(repo.quran.surahs[1].name).performClick()
            compose.onNodeWithText("2 / 604").assertExists();compose.onNodeWithText("Terminer mon apprentissage").assertDoesNotExist()
            assertEquals(initial.arr("sessions"),repo.state.value.arr("sessions"));assertFalse(known(repo.state.value,1))
        } finally { compose.waitForIdle();kotlinx.coroutines.runBlocking { repo.mutate { saved } } }
    }
}
