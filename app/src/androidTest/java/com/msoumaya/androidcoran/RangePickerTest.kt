package com.msoumaya.androidcoran

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.RangePicker
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RangePickerTest {
    @get:Rule val compose=createComposeRule()
    @Test fun hizbSelectionUsesCanonicalRangeAndRejectsInvalidNumber() {
        val q=Quran(InstrumentationRegistry.getInstrumentation().targetContext)
        var selection: VerseRange?=null
        compose.setContent { MaterialTheme { RangePicker(q,"Appliquer") { selection=it } } }
        compose.onNodeWithText("Hizb").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("30")
        compose.onNodeWithText("Appliquer").performClick()
        compose.runOnIdle { assertEquals(q.hizbs[29],selection);selection=null }
        compose.onNode(hasSetTextAction()).performTextReplacement("61")
        compose.onNodeWithText("Appliquer").performClick()
        compose.onNodeWithText("Sélection invalide : vérifie les numéros.").assertExists()
        compose.runOnIdle { assertNull(selection) }
    }
}
