package com.msoumaya.androidcoran
import android.graphics.*
import android.view.MotionEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResizableNotesAndroidTest {
 @get:Rule val compose=createComposeRule()
 private fun sample()=newReaderInk(1,InkStroke(listOf(InkPoint(.2f,.3f))),listOf(1)).copy(text="Une courte note.",categories=listOf("Note personnelle"))
 @Test fun savedNoteOpensWithoutEditingControlsAndSizesToItsContent() {
  val current=mutableStateOf(sample())
  compose.setContent {MaterialTheme {ReaderInkEditor(current.value,{},{},{},startEditing=false)}}
  compose.onNodeWithText("Une courte note.").assertIsDisplayed();compose.onNodeWithText("Modifier",substring=false).assertIsDisplayed()
  compose.onNodeWithText("Clavier",substring=false).assertDoesNotExist();compose.onNodeWithText("Enregistrer",substring=false).assertDoesNotExist();compose.onNodeWithContentDescription("Zone de note manuscrite").assertDoesNotExist()
  val small=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot
  compose.runOnIdle {current.value=current.value.copy(id="long-note",text="Cette phrase de révision est à relire avec attention. ".repeat(180))}
  val large=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot
  assertTrue("Long content should open a taller window",large.height>small.height)
 }
 @Test fun resizeAndRestorePreserveKeyboardDraftAndExplicitSave() {
  val restore=StateRestorationTester(compose);val initial=sample();var saved: ReaderInk?=null
  restore.setContent {MaterialTheme {ReaderInkEditor(initial,{saved=it},{},{},startEditing=false)}}
  compose.onNodeWithContentDescription("Agrandir la note").performClick()
  compose.onNodeWithContentDescription("Réduire la note").performClick()
  compose.onNodeWithText("Modifier",substring=false).performClick();compose.onNodeWithText("Clavier",substring=false).performClick()
  compose.onNodeWithText(initial.text).performTextReplacement("Mon brouillon conservé")
  val before=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot
  compose.onNodeWithContentDescription("Redimensionner la note").performTouchInput {swipe(center,center+Offset(90f,110f),300)}
  val after=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot;assertTrue(after.height>before.height||after.width>before.width)
  restore.emulateSavedInstanceStateRestore();compose.onNodeWithText("Mon brouillon conservé").assertExists();compose.runOnIdle {assertNull(saved)}
  compose.onNodeWithText("Enregistrer",substring=false).performClick();compose.runOnIdle {assertEquals("Mon brouillon conservé",saved!!.text);assertEquals(initial.outlines,saved!!.outlines);assertEquals(initial.categories,saved!!.categories)}
 }
 @Test fun failedPersistenceKeepsTheDraftAndWindowOpen() {
  var closed=false;val initial=sample()
  compose.setContent {MaterialTheme {ReaderInkEditor(initial,{},{},{closed=true},onSave={throw java.io.IOException("Stockage indisponible")})}}
  compose.onNodeWithText("Enregistrer",substring=false).performClick();compose.onNodeWithText("Stockage indisponible").assertIsDisplayed()
  compose.onNodeWithTag("floating-note-window").assertIsDisplayed();compose.runOnIdle {assertFalse(closed)}
 }
 @Test fun handwritingRemainsCircularAcrossDifferentWindowRatiosAndReadOnlyRejectsInput() {
  compose.runOnIdle {
   val stroke=InkStroke((0..80).map {i -> val a=i*Math.PI*2/80;InkPoint(.5f+.1f*kotlin.math.cos(a).toFloat(),.5f+.2f*kotlin.math.sin(a).toFloat())})
   val v=HandwritingView(InstrumentationRegistry.getInstrumentation().targetContext);v.fixedAspect=2f;v.strokes=listOf(stroke);v.readOnly=true
   for((w,h) in listOf(400 to 200,400 to 400,600 to 250)) {
    v.layout(0,0,w,h);val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    try {v.draw(Canvas(b));val ink=mutableListOf<Pair<Int,Int>>();for(y in 0 until h) for(x in 0 until w) if(Color.red(b.getPixel(x,y))<100) ink+=x to y
     assertTrue(ink.isNotEmpty());assertEquals((ink.maxOf {it.first}-ink.minOf {it.first}).toFloat(),(ink.maxOf {it.second}-ink.minOf {it.second}).toFloat(),2f)
    } finally {b.recycle()}
   }
   var commits=0;v.onStroke={commits++};val e=MotionEvent.obtain(1,1,MotionEvent.ACTION_DOWN,100f,100f,0);try {assertFalse(v.dispatchTouchEvent(e))} finally {e.recycle()};assertEquals(0,commits);assertEquals(listOf(stroke),v.strokes)
  }
 }
}
