package com.msoumaya.androidcoran
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NoteReadingFlowTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun view()=findReaderView<MushafView>(compose.activity.window.decorView)!!
 private fun number(v: MushafView,key: String)=MushafView::class.java.getDeclaredField(key).apply {isAccessible=true}.getFloat(v)
 @Suppress("UNCHECKED_CAST") private fun icons(v: MushafView)=MushafView::class.java.getDeclaredField("noteIcons").apply {isAccessible=true}.get(v) as Map<String,RectF>
 private fun pose(): List<Float> {var values=emptyList<Float>();compose.runOnUiThread {val v=view();values=listOf(v.page.toFloat(),v.width.toFloat(),v.height.toFloat())+listOf("zoom","offsetX","offsetY","ratio","left","top").map {number(v,it)}};return values}
 private fun tap(x: Float,y: Float,long: Boolean=false) {
  val down=SystemClock.uptimeMillis()
  compose.runOnUiThread {val e=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,y,0);try {view().dispatchTouchEvent(e)} finally {e.recycle()}}
  if(long) SystemClock.sleep(android.view.ViewConfiguration.getLongPressTimeout()+120L)
  compose.runOnUiThread {val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);try {view().dispatchTouchEvent(e)} finally {e.recycle()}}
  if(!long) SystemClock.sleep(android.view.ViewConfiguration.getDoubleTapTimeout()+100L)
  compose.waitForIdle()
 }
 private fun iconCenter(id: String): Pair<Float,Float> {var point=0f to 0f;compose.runOnUiThread {val v=view();val r=icons(v).getValue(id);val scale=number(v,"ratio")*number(v,"zoom");point=number(v,"left")+number(v,"offsetX")+r.centerX()*scale to number(v,"top")+number(v,"offsetY")+r.centerY()*scale};return point}
 private fun fixture(seed: ReaderInk?=null,block: (com.msoumaya.androidcoran.data.Repository)->Unit) {
  val repo=(compose.activity.application as CoranApplication).repository;runBlocking {repo.awaitReady()};assertNull(repo.user.value);val saved=repo.state.value
  runBlocking {repo.mutate {val base=defaultState().with("reader" to defaultState().obj("reader").with("tablet" to TabletPreferences(mode="on",stylusOnly=false,pageCurl=false).toJson()),"lastRead" to json("verseId" to 1,"page" to 1));if(seed==null) base else saveReaderNote(base,seed)}}
  try {
   compose.waitForIdle();compose.onNodeWithText("Lire le Coran").performScrollTo().performClick()
   compose.waitUntil(10000) {var ready=false;compose.runOnUiThread {val v=findReaderView<MushafView>(compose.activity.window.decorView);ready=v?.tag is Triple<*,*,*>&&(MushafView::class.java.getDeclaredField("regions").apply {isAccessible=true}.get(v) as List<*>).isNotEmpty()};ready}
   if(seed!=null) {if(compose.onAllNodesWithTag("tablet-compact-header").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("tablet-tab-annoter").performClick() else compose.onNodeWithText("Annoter",substring=false).performScrollTo().performClick()}
   if(compose.onAllNodesWithTag("tablet-compact-header").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithContentDescription("Lecture plein écran").performClick() else compose.onNodeWithText("Lecture plein écran",substring=false).performScrollTo().performClick();compose.waitForIdle();block(repo)
  } catch(e: Throwable) {runCatching {captureReaderState("note-flow",compose.onAllNodes(isRoot(),useUnmergedTree=true).onFirst().printToString())};throw e}
  finally {compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()};runBlocking {repo.mutate {latest -> allReaderInk(latest).filter {n -> allReaderInk(saved).none {it.id==n.id}}.fold(latest) {s,n -> deleteReaderInk(s,n.id,annotationTime(n.updatedAt))}};repo.mutate {saved}}}
 }
 @Test fun iconOpensReadOnlyThenSaveReturnsToReadingWithTheExactSameZoomAndPan() {
  val note=ReaderInk("saved-note-flow",1,emptyList(),emptyList(),listOf(1),annotationTime(),text="Note conservée",anchor=json("verseId" to 1),categories=listOf("Note personnelle"))
  fixture(note) {repo ->
   compose.waitUntil(10000) {var ready=false;compose.runOnUiThread {ready=icons(view()).containsKey(note.id)};ready}
   val first=iconCenter(note.id)
   compose.runOnUiThread {MushafView::class.java.getDeclaredMethod("zoomAt",Float::class.javaPrimitiveType,Float::class.javaPrimitiveType,Float::class.javaPrimitiveType).apply {isAccessible=true}.invoke(view(),1.6f,first.first,first.second)}
   compose.waitForIdle();val before=pose();assertTrue(before[3]>1f)
   val p=iconCenter(note.id);tap(p.first,p.second)
   compose.onNodeWithText(note.text).assertIsDisplayed();compose.onNodeWithText("Clavier",substring=false).assertDoesNotExist();compose.onNodeWithContentDescription("Zone de note manuscrite").assertDoesNotExist()
   compose.onNodeWithText("Modifier",substring=false).performClick();compose.onNodeWithContentDescription("Réduire la note").performClick();compose.onNodeWithText("Clavier",substring=false).performClick()
   compose.onNodeWithText(note.text).performTextReplacement("Note mise à jour")
   compose.onNodeWithText("Enregistrer",substring=false).performClick()
   compose.waitUntil(10000) {allReaderInk(repo.state.value).any {it.id==note.id&&it.text=="Note mise à jour"}}
   compose.onNodeWithTag("floating-note-window").assertDoesNotExist();compose.onNodeWithTag("verse-floating-bar").assertDoesNotExist();compose.onNodeWithText("Terminer les annotations",substring=false).assertDoesNotExist();compose.onNodeWithTag("tablet-tools-floating").assertDoesNotExist()
   assertEquals(before,pose());compose.runOnUiThread {assertFalse(view().annotationMode);assertEquals("select",view().inkTool)}
   val q=iconCenter(note.id);tap(q.first,q.second);compose.onNodeWithText("Note mise à jour").assertIsDisplayed();compose.onNodeWithText("Modifier",substring=false).assertIsDisplayed()
   captureReaderState("note-readonly-final",compose.onRoot(useUnmergedTree=true).printToString())
   compose.onNodeWithText("Fermer",substring=false).performClick()
  }
 }
 @Test fun newHandwrittenNoteSurvivesResizeAndAppearsAfterSavingWithoutMovingThePage()=fixture {repo ->
  val before=pose();val oldIds=allReaderInk(repo.state.value).map {it.id}.toSet();var point=0f to 0f
  compose.runOnUiThread {val v=view();outer@for(y in v.height/4 until v.height*3/4 step 12) for(x in v.width/3 until v.width*2/3 step 12) if(v.selectionAt(x.toFloat(),y.toFloat())!=null) {point=x.toFloat() to y.toFloat();break@outer}}
  tap(point.first,point.second,true);compose.onNodeWithText("Note",substring=false).performClick()
  compose.onNodeWithContentDescription("Zone de note manuscrite").performTouchInput {swipe(center-Offset(width*.2f,0f),center+Offset(width*.2f,0f),300)}
  compose.onNodeWithText("Note personnelle").performScrollTo().performClick()
  var strokes=emptyList<InkStroke>();var aspect: Float?=null
  compose.runOnUiThread {val pad=android.view.inspector.WindowInspector.getGlobalWindowViews().firstNotNullOf {findReaderView<HandwritingView>(it)};strokes=pad.strokes;aspect=pad.fixedAspect}
  assertTrue(strokes.isNotEmpty());compose.onNodeWithContentDescription("Réduire la note").performClick()
  compose.runOnUiThread {val pad=android.view.inspector.WindowInspector.getGlobalWindowViews().firstNotNullOf {findReaderView<HandwritingView>(it)};assertEquals(strokes,pad.strokes);assertEquals(aspect,pad.fixedAspect)}
  compose.onNodeWithText("Enregistrer",substring=false).performClick();compose.waitUntil(10000) {allReaderInk(repo.state.value).any {it.id !in oldIds}}
  val saved=allReaderInk(repo.state.value).single {it.id !in oldIds};assertEquals(strokes,saved.note);assertEquals(aspect,saved.noteAspect);assertEquals(before,pose())
  compose.onNodeWithTag("floating-note-window").assertDoesNotExist();compose.runOnUiThread {assertTrue(icons(view()).containsKey(saved.id));assertFalse(view().annotationMode)}
 }
}
