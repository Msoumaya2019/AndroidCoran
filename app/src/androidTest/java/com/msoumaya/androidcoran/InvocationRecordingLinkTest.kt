package com.msoumaya.androidcoran

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.msoumaya.androidcoran.domain.json
import com.msoumaya.androidcoran.ui.InvocationRecordingLink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InvocationRecordingLinkTest {
    @get:Rule val compose=createComposeRule()
    @Test fun snapshotIsVisibleAndClickOpensExactContent() {
        var opened=""
        compose.setContent { InvocationRecordingLink(json("invocation_id" to "exact-id","invocation_snapshot" to json("arabic_text" to "الحمد لله"))) { opened=it } }
        compose.onNodeWithText("الحمد لله").assertIsDisplayed()
        assertEquals("",opened)
        compose.onNodeWithText("Voir l’invocation").performClick()
        assertEquals("exact-id",opened)
    }
    @Test fun snapshotWithoutContentIdCannotOpenAnotherInvocation() {
        compose.setContent { InvocationRecordingLink(json("invocation_snapshot" to json("arabic_text" to "الحمد لله"))) {} }
        compose.onNodeWithText("الحمد لله").assertIsDisplayed()
        compose.onNodeWithText("Voir l’invocation").assertDoesNotExist()
    }
}
