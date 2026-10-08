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
}
