package com.msoumaya.androidcoran
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.msoumaya.androidcoran.data.encodeAvatar
import com.msoumaya.androidcoran.domain.AvatarCrop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class SignupAvatarTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun pendingPhotoPersistsForItsEmailAndCanBeRemovedBeforeSignup() {
  val repo=(compose.activity.application as CoranApplication).repository
  runBlocking { repo.awaitReady() };assertNull(repo.user.value)
  val email="native-avatar-test@example.invalid"
  val old=repo.pendingAvatars.load(email)
  val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE)
  try {
   repo.pendingAvatars.save(email,encodeAvatar(image,AvatarCrop()))
   compose.onNodeWithContentDescription("Compte").performClick()
   compose.onNodeWithText("E-mail").performTextReplacement(email)
   compose.onNode(isToggleable()).performClick()
   compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Photo choisie pour ce compte").fetchSemanticsNodes().isNotEmpty() }
   compose.activityRule.scenario.recreate();compose.waitForIdle()
   compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Photo choisie pour ce compte").fetchSemanticsNodes().isNotEmpty() }
   compose.onNodeWithContentDescription("Photo choisie pour ce compte").assertExists()
   compose.onNodeWithText("E-mail").performScrollTo().performTextReplacement("another-native-avatar@example.invalid")
   compose.onNodeWithContentDescription("Photo choisie pour ce compte").assertDoesNotExist()
   assertNotNull(repo.pendingAvatars.load(email))
   compose.onNodeWithText("E-mail").performTextReplacement(email)
   compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Photo choisie pour ce compte").fetchSemanticsNodes().isNotEmpty() }
   compose.onNodeWithText("Retirer la photo choisie").performScrollTo().performClick()
   compose.waitUntil(10000) { repo.pendingAvatars.load(email)==null }
   compose.onNodeWithContentDescription("Photo choisie pour ce compte").assertDoesNotExist()
  } finally { image.recycle();runBlocking { repo.removePendingAvatar(email) };if(old!=null) repo.pendingAvatars.save(email,old) }
 }
}

