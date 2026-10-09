package com.msoumaya.androidcoran
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.ui.RecitationSharingDialog
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class RecitationSharingDialogTest {
 @get:Rule val rule=createComposeRule()
 private val friends=acceptedRecitationFriends("me",listOf(json("id" to "link","requester_id" to "me","recipient_id" to "friend","status" to "accepted")),listOf(json("id" to "friend","display_name" to "Samira")))
 @Test fun choosingFriendDoesNotSendUntilExplicitConfirmation() {
  val sent=mutableListOf<String>();var dismissed=0
  rule.setContent { MaterialTheme { RecitationSharingDialog("Al Fâtiha",friends,false,false,"",{sent+=it},{dismissed++}) } }
  rule.onNodeWithText("Samira").performClick();assertTrue(sent.isEmpty())
  rule.onNodeWithText("Partager cette récitation ?").assertExists();rule.onNodeWithText("Annuler").performClick();assertTrue(sent.isEmpty());assertEquals(0,dismissed)
  rule.onNodeWithText("Samira").performClick();rule.onNodeWithText("Partager").performClick();assertEquals(listOf("link"),sent)
 }
 @Test fun busySubmissionBlocksDuplicateSendAndCancellation() {
  var busy by mutableStateOf(false);var sends=0;var dismissed=0
  rule.setContent { MaterialTheme { RecitationSharingDialog("Al Fâtiha",friends,false,busy,"",{sends++;busy=true},{dismissed++}) } }
  rule.onNodeWithText("Samira").performClick();rule.onNodeWithText("Partager").performClick()
  rule.onNodeWithText("Envoi…").assertIsNotEnabled();rule.onNodeWithText("Annuler").assertIsNotEnabled();assertEquals(1,sends);assertEquals(0,dismissed)
 }
 @Test fun noAcceptedFriendsOffersNoSendAction() {
  var sends=0
  rule.setContent { MaterialTheme { RecitationSharingDialog("Al Fâtiha",emptyList(),false,false,"",{sends++},{}) } }
  rule.onNodeWithText("Aucun ami accepté pour le moment.").assertExists();rule.onNodeWithText("Partager").assertDoesNotExist();assertEquals(0,sends)
 }
}
