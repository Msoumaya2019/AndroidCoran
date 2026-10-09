package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.msoumaya.androidcoran.ui.RecitationDeleteDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class RecitationDeleteDialogTest {
 @get:Rule val rule=createComposeRule()
 @Test fun cancellationDoesNotDeleteAnyRecording() {
  var deleted=0;var dismissed=0
  rule.setContent { MaterialTheme { RecitationDeleteDialog("Al Fâtiha",false,"",{deleted++},{dismissed++}) } }
  rule.onNodeWithText("Supprimer cette récitation ?").assertExists();assertEquals(0,deleted)
  rule.onNodeWithText("Annuler").performClick();assertEquals(0,deleted);assertEquals(1,dismissed)
 }
 @Test fun explicitConfirmationBlocksRepeatedDeletionWhileBusy() {
  var busy by mutableStateOf(false);var deleted=0;var dismissed=0
  rule.setContent { MaterialTheme { RecitationDeleteDialog("Al Fâtiha",busy,"",{deleted++;busy=true},{dismissed++}) } }
  rule.onNodeWithText("Supprimer").performClick();assertEquals(1,deleted)
  rule.onNodeWithText("Suppression…").assertIsNotEnabled();rule.onNodeWithText("Annuler").assertIsNotEnabled();assertEquals(0,dismissed)
 }
}
