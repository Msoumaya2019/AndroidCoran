package com.msoumaya.androidcoran

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.msoumaya.androidcoran.domain.json
import com.msoumaya.androidcoran.ui.TodayContentCarousel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TodayContentCarouselTest {
 @get:Rule val rule=createComposeRule()
 @Test fun bothDailyPagesOpenTheirOwnContent() {
  val opened=mutableListOf<String>()
  rule.setContent { MaterialTheme { TodayContentCarousel(listOf(json("id" to "invocation","type" to "invocation","french_text" to "Texte de l’invocation"),json("id" to "reminder","type" to "reminder","french_text" to "Texte du rappel")),false,"",{opened+=it},{}) } }
  rule.onNodeWithText("Texte du rappel").performClick();assertEquals(listOf("reminder"),opened)
  rule.onNodeWithTag("daily-content-pager").performTouchInput { swipeLeft() };rule.waitForIdle()
  rule.onNodeWithText("Texte de l’invocation").performClick();assertEquals(listOf("reminder","invocation"),opened)
 }
 @Test fun unavailableContentOffersRetryWithoutOpeningAnInventedId() {
  var retries=0;var opens=0
  rule.setContent { MaterialTheme { TodayContentCarousel(emptyList(),false,"Hors connexion",{opens++},{retries++}) } }
  rule.onNodeWithText("Hors connexion").assertIsDisplayed();rule.onNodeWithText("Réessayer").performClick()
  assertEquals(1,retries);assertEquals(0,opens)
 }
}
