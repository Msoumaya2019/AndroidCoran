package com.msoumaya.androidcoran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.ui.*
import org.junit.Rule
import org.junit.Test

class AdminQuizEditorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun questionDraftAndAnswerValidationSurviveSavedStateRestoration() {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoranApplication
        val vm=CoranViewModel(app)
        val restore=StateRestorationTester(compose)
        restore.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { AdminQuizPanel(vm) } } }
        compose.onNodeWithText("Créer").performClick()
        compose.onNodeWithText("Enregistrer").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Question").performScrollTo().performTextInput("Question de test")
        compose.onNodeWithText("Source obligatoire").performScrollTo().performTextInput("Source de test")
        listOf("A","B","C").forEach { id -> compose.onNodeWithText("Réponse $id").performScrollTo().performTextInput("Choix $id") }
        compose.waitForIdle()
        compose.onNodeWithText("Enregistrer").performScrollTo().assertIsEnabled()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Question de test").assertExists()
        compose.onNodeWithText("Choix B").assertExists()
        compose.onNodeWithText("Enregistrer").performScrollTo().assertIsEnabled()
    }
}
