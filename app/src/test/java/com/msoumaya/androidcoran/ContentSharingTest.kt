package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test

class ContentSharingTest {
 @Test fun sharesOriginalFieldsInOrderAndKeepsArabic() {
  val content=json("title" to "Invocation","arabic_text" to "الحمد لله","phonetic_text" to "Al hamdu lillah","french_text" to "Louange à Allah","explanation" to "Explication","source" to "Source originale","reference" to "Référence")
  assertEquals("Invocation\n\nالحمد لله\n\nAl hamdu lillah\n\nLouange à Allah\n\nExplication\n\nSource : Source originale Référence",sharedContentText(content))
 }
 @Test fun missingOptionalFieldsDoNotBecomeNullInSharedText() {
  assertEquals("Rappel\n\nTexte\n\nSource : Livre ",sharedContentText(json("title" to "Rappel","french_text" to "Texte","source" to "Livre")))
 }
 @Test fun sharingNeverIncludesPrivateOrMediaFields() {
  val text=sharedContentText(json("french_text" to "Texte","user_id" to "private-owner","audio_url" to "signed-secret","image_url" to "private-image"))
  assertFalse(text.contains("private"));assertFalse(text.contains("signed-secret"))
 }
}
