package com.msoumaya.androidcoran

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.msoumaya.androidcoran.ui.ConversationTypingIndicator
import org.junit.Rule
import org.junit.Test

class ConversationTypingIndicatorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun indicatorAppearsAndDisappearsWithRemoteActivity() {
        val active=mutableStateOf(false)
        compose.setContent { MaterialTheme { ConversationTypingIndicator(active.value) } }
        compose.onNodeWithText("Écrit un message…").assertDoesNotExist()
        compose.runOnIdle { active.value=true }
        compose.onNodeWithText("Écrit un message…").assertIsDisplayed()
        compose.runOnIdle { active.value=false }
        compose.onNodeWithText("Écrit un message…").assertDoesNotExist()
    }
}
