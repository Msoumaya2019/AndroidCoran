package com.msoumaya.androidcoran
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VerseFloatingBarTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun fixture(block: (com.msoumaya.androidcoran.data.Repository)->Unit) {
  val repo=(compose.activity.application as CoranApplication).repository
  runBlocking {repo.awaitReady()};assertNull(repo.user.value);val saved=repo.state.value
  runBlocking {repo.mutate {defaultState().with("reader" to defaultState().obj("reader").with("tablet" to TabletPreferences(mode="off",pageCurl=false).toJson()),"lastRead" to json("verseId" to 1,"page" to 1))}}
  try {compose.waitForIdle();compose.onNodeWithText("Lire le Coran").performScrollTo().performClick();compose.waitUntil(10000) {var ready=false;compose.runOnUiThread {ready=findReaderView<MushafView>(compose.activity.window.decorView)?.selectionAtPaper()!=null};ready};block(repo)}
  catch(e: Throwable) {runCatching {captureReaderState("floating-bar",compose.onAllNodes(isRoot(),useUnmergedTree=true).onFirst().printToString())};throw e}
  finally {compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()};runBlocking {repo.mutate {saved}}}
 }
 private fun MushafView.selectionAtPaper(): Pair<Pair<Float,Float>,Int>? {
  for(y in height/5 until height*4/5 step 12) for(x in width/4 until width*3/4 step 12) selectionAt(x.toFloat(),y.toFloat())?.let {return (x.toFloat() to y.toFloat()) to it.num("verseId")}
  return null
 }
 private fun select(long: Boolean=true): Int {
  var id=0;var point=0f to 0f;val down=SystemClock.uptimeMillis()
  compose.runOnUiThread {val v=findReaderView<MushafView>(compose.activity.window.decorView)!!;val hit=v.selectionAtPaper()!!;point=hit.first;id=hit.second;val e=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,point.first,point.second,0);try {v.dispatchTouchEvent(e)} finally {e.recycle()}}
  if(long) SystemClock.sleep(android.view.ViewConfiguration.getLongPressTimeout()+120L)
  compose.runOnUiThread {val v=findReaderView<MushafView>(compose.activity.window.decorView)!!;val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,point.first,point.second,0);try {v.dispatchTouchEvent(e)} finally {e.recycle()}}
  compose.waitForIdle();return id
 }
 @Test fun longPressShowsFiveActionsWithoutResizingPaperAndDifficultyToggles()=fixture {repo ->
  val before=compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot
  select(false);SystemClock.sleep(350);compose.onNodeWithTag("verse-floating-bar").assertDoesNotExist()
  val id=select();compose.onNodeWithTag("verse-floating-bar").assertIsDisplayed()
  listOf("Écouter","Difficile","Note","Enregistrer ma récitation").forEach {compose.onNodeWithText(it,substring=false).assertIsDisplayed()}
  compose.onNodeWithText("Choisir une sourate ou une page").assertDoesNotExist()
  assertEquals(before,compose.onNodeWithTag("printed-page-1").fetchSemanticsNode().boundsInRoot)
  val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
  compose.onNodeWithText("Enregistrer ma récitation",substring=false).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {it(results)}
  assertTrue(results.isNotEmpty());assertEquals("Enregistrer ma récitation".length,results.first().getLineEnd(results.first().lineCount-1,true))
  compose.onNodeWithText("Difficile",substring=false).performClick();compose.waitUntil(10000) {repo.state.value.obj("difficultyMarkers").obj(id.toString())["user"].let {it!=null&&it!=JsonNull}}
  compose.onNodeWithText("Difficile",substring=false).performClick();compose.waitUntil(10000) {repo.state.value.obj("difficultyMarkers").obj(id.toString())["user"].let {it==null||it==JsonNull}}
  captureReaderState("floating-bar-final",compose.onRoot(useUnmergedTree=true).printToString())
  compose.onNodeWithContentDescription("Fermer les options du verset").performClick();compose.onNodeWithTag("verse-floating-bar").assertDoesNotExist()
  compose.runOnUiThread {assertNull(findReaderView<MushafView>(compose.activity.window.decorView)!!.selected)}
 }
 @Test fun noteActionCreatesDraftForTheSelectedVerse()=fixture {repo ->
  val original=allReaderInk(repo.state.value);select();compose.onNodeWithText("Note",substring=false).performClick()
  compose.onNodeWithText("Titre de la note").assertIsDisplayed();compose.onNodeWithContentDescription("Zone de note manuscrite").assertIsDisplayed()
  assertEquals(original,allReaderInk(repo.state.value));compose.onNodeWithText("Fermer",substring=false).performClick()
 }
 @Test fun recordingActionIsDirectAndTargetsExactlyTheSelectedVerse()=fixture {repo ->
  val id=select();compose.onNodeWithText("Enregistrer ma récitation",substring=false).performClick()
  compose.onNodeWithText("Ma récitation",substring=false).assertIsDisplayed();compose.onNodeWithText(repo.quran.reference(VerseRange(id,id))).assertIsDisplayed()
  compose.onNodeWithText("Commencer",substring=false).assertIsDisplayed();compose.onNodeWithText("Fermer",substring=false).performClick()
 }
}
