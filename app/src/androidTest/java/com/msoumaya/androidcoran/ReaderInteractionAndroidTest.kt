package com.msoumaya.androidcoran
import android.graphics.*
import android.os.SystemClock
import android.view.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderInteractionAndroidTest {
 @get:Rule val compose=createComposeRule()
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private fun field(view: MushafView,key: String)=MushafView::class.java.getDeclaredField(key).apply { isAccessible=true }.getFloat(view)
 private fun touch(v: MushafView,action: Int,points: List<Pair<Float,Float>>,down: Long,tool: Int=MotionEvent.TOOL_TYPE_FINGER) {
  val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,points.size,points.indices.map { MotionEvent.PointerProperties().apply { id=it;toolType=tool } }.toTypedArray(),points.map { (x,y) -> MotionEvent.PointerCoords().apply { this.x=x;this.y=y;pressure=.7f;size=.1f } }.toTypedArray(),0,0,1f,1f,0,0,if(tool==MotionEvent.TOOL_TYPE_STYLUS) InputDevice.SOURCE_STYLUS else InputDevice.SOURCE_TOUCHSCREEN,0)
  try { v.dispatchTouchEvent(e) } finally { e.recycle() }
 }
 private fun fixture(block: (MushafView,Bitmap)->Unit) {
  val repo=(context.applicationContext as CoranApplication).repository;runBlocking { repo.awaitReady() };val data=repo.quran.qcfData(context,1)
  lateinit var view: MushafView;lateinit var bitmap: Bitmap
  compose.runOnIdle { view=MushafView(context);view.load(repo.quran,1,"coranTest",data,null,null);view.marginEnabled=true;view.layout(0,0,1400,1000);bitmap=Bitmap.createBitmap(1400,1000,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap)) }
  try { block(view,bitmap) } finally { compose.runOnIdle { bitmap.recycle() } }
 }
 @Test fun realPinchKeepsThePaperUnderTheFocalPointAndDoesNotOpenMenu()=fixture { v,b ->
  var menus=0;val down=SystemClock.uptimeMillis();val x=810f;val y=430f;var beforeX=0f;var beforeY=0f
  compose.runOnIdle {
   v.onSelection={menus++};beforeX=(x-field(v,"left")-field(v,"offsetX"))/field(v,"zoom");beforeY=(y-field(v,"top")-field(v,"offsetY"))/field(v,"zoom")
   touch(v,MotionEvent.ACTION_DOWN,listOf(x-60 to y),down)
   touch(v,MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),listOf(x-60 to y,x+60 to y),down)
   for(span in listOf(80f,100f,130f,160f)) touch(v,MotionEvent.ACTION_MOVE,listOf(x-span to y,x+span to y),down)
   v.draw(Canvas(b));assertTrue(field(v,"zoom")>1f)
   assertEquals(beforeX,(x-field(v,"left")-field(v,"offsetX"))/field(v,"zoom"),.01f);assertEquals(beforeY,(y-field(v,"top")-field(v,"offsetY"))/field(v,"zoom"),.01f)
   touch(v,MotionEvent.ACTION_MOVE,listOf(x-110 to y+30,x+210 to y+30),down);v.draw(Canvas(b));assertEquals(beforeX,(x+50-field(v,"left")-field(v,"offsetX"))/field(v,"zoom"),.01f);assertEquals(beforeY,(y+30-field(v,"top")-field(v,"offsetY"))/field(v,"zoom"),.01f)
   touch(v,MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),listOf(x-110 to y+30,x+210 to y+30),down);touch(v,MotionEvent.ACTION_UP,listOf(x-160 to y),down)
  }
  SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+100L);compose.runOnIdle { assertEquals(0,menus) }
 }
 @Test fun tapDoesNothingAndLongPressRetainsVerseAndWordActions()=fixture { v,_ ->
  var menus=0;var selected=0;var point=0f to 0f
  compose.runOnIdle { outer@for(y in 200..800 step 10) for(x in 350..1000 step 10) if(v.selectionAt(x.toFloat(),y.toFloat())!=null) { point=x.toFloat() to y.toFloat();break@outer };v.onVerse={selected++};v.onSelection={menus++};val t=SystemClock.uptimeMillis();touch(v,MotionEvent.ACTION_DOWN,listOf(point),t);touch(v,MotionEvent.ACTION_UP,listOf(point),t) }
  SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout()+100L);compose.runOnIdle { assertEquals(0,menus);assertEquals(0,selected) }
  val t=SystemClock.uptimeMillis();compose.runOnIdle { touch(v,MotionEvent.ACTION_DOWN,listOf(point),t) };SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+150L)
  compose.runOnIdle { touch(v,MotionEvent.ACTION_UP,listOf(point),t);assertEquals(1,menus);assertEquals(1,selected) }
 }
 @Test fun stylusCanDrawAtBothOuterScreenEdgesWithoutCoordinateClamping()=fixture { v,_ ->
  var stroke: InkStroke?=null
  compose.runOnIdle { v.annotationMode=true;v.stylusOnly=true;v.onInkStroke={s,_ -> stroke=s};val t=SystemClock.uptimeMillis();touch(v,MotionEvent.ACTION_DOWN,listOf(4f to 300f),t,MotionEvent.TOOL_TYPE_STYLUS);touch(v,MotionEvent.ACTION_MOVE,listOf(1394f to 350f),t,MotionEvent.TOOL_TYPE_STYLUS);touch(v,MotionEvent.ACTION_UP,listOf(1395f to 360f),t,MotionEvent.TOOL_TYPE_STYLUS);assertTrue(stroke!!.points.first().x< -.35f);assertTrue(stroke!!.points.last().x>1.35f);assertEquals(stroke,inkFromJson(newReaderInk(1,stroke!!,emptyList()).toSourceJson())!!.outlines.single());val scale=field(v,"ratio");v.annotations=listOf(newReaderInk(1,stroke!!,emptyList()));val frame=Bitmap.createBitmap(1400,1000,Bitmap.Config.ARGB_8888);try {v.draw(Canvas(frame));assertEquals(scale,field(v,"ratio"),0f)} finally {frame.recycle()} }
 }
 @Test fun marginNoteIconsDoNotOverlapAndTapOpensOnlyThatNote()=fixture { v,b ->
  var opened="";var menus=0;var point=0f to 0f
  compose.runOnIdle {
   val base=newReaderInk(1,InkStroke(listOf(InkPoint(.5f,.4f))),listOf(1)).copy(tool="note",text="Note",categories=listOf("Note personnelle"),iconColor="#DB2777")
   v.annotations=(1..3).map { base.copy(id="note-$it") };v.onInkClick={opened=it.id};v.onSelection={menus++};v.draw(Canvas(b))
   @Suppress("UNCHECKED_CAST") val icons=MushafView::class.java.getDeclaredField("noteIcons").apply { isAccessible=true }.get(v) as Map<String,RectF>
   assertEquals(3,icons.size);icons.values.toList().zipWithNext().forEach { (a,b) -> assertFalse(RectF.intersects(a,b)) }
   val r=icons.getValue("note-2");point=(field(v,"left")+r.centerX()*field(v,"ratio")) to (field(v,"top")+r.centerY()*field(v,"ratio"));val t=SystemClock.uptimeMillis();touch(v,MotionEvent.ACTION_DOWN,listOf(point),t);touch(v,MotionEvent.ACTION_UP,listOf(point),t)
  }
  SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout()+100L);compose.runOnIdle { assertEquals("note-2",opened);assertEquals(0,menus) }
 }
 @Test fun noteRequiresCategoryAndSavesTitleKeyboardAndIconColorExplicitly() {
  val initial=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1));var saved: ReaderInk?=null
  compose.setContent { MaterialTheme { ReaderInkEditor(initial,{saved=it},{},{}) } }
  compose.onNodeWithText("Enregistrer",substring=false).assertIsNotEnabled()
  compose.onNodeWithText("Titre de la note").performTextReplacement("Mon titre")
  compose.onNodeWithText("Clavier",substring=false).performClick();compose.onNodeWithText("Écrivez votre note…").performTextInput("Ma note au clavier")
  compose.onNodeWithText("Catégories et couleur · 0 sélectionnée(s)").performClick()
  compose.onNodeWithText("+ Créer une catégorie").performScrollTo().performClick();compose.onNodeWithText("Nom de la catégorie").performTextInput("Mon cours");compose.onNodeWithText("Ajouter",substring=false).performClick()
  compose.onNodeWithText("Violet",substring=false).performScrollTo().performClick();compose.runOnIdle { assertNull(saved) }
  compose.onNodeWithText("Enregistrer",substring=false).performClick();compose.runOnIdle { assertEquals("Mon titre",saved!!.title);assertEquals("Ma note au clavier",saved!!.text);assertEquals(listOf("Mon cours"),saved!!.categories);assertEquals("#7C3AED",saved!!.iconColor) }
 }
 @Test fun floatingNoteCanBeDraggedWithoutSavingOrOpeningAnotherNote() {
  val ink=newReaderInk(1,InkStroke(listOf(InkPoint(.2f,.3f))),listOf(1));var saves=0
  compose.setContent { MaterialTheme { ReaderInkEditor(ink,{saves++},{},{}) } }
  val before=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot
  compose.onNodeWithContentDescription("Déplacer la note").performTouchInput { swipe(center,center+androidx.compose.ui.geometry.Offset(100f,20f),300) }
  val after=compose.onNodeWithTag("floating-note-window").fetchSemanticsNode().boundsInRoot
  assertTrue(after.left>before.left);compose.onNodeWithText("Enregistrer",substring=false).assertIsNotEnabled();compose.runOnIdle { assertEquals(0,saves) }
 }
 @Test fun curlRevealsTheNextSheetAndCanRunInBothDirections() {
  compose.runOnIdle {
   val old=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);old.eraseColor(Color.RED)
   val frame=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);val c=Canvas(frame)
   try { for(forward in listOf(true,false)) { frame.eraseColor(Color.BLUE);drawPageCurl(c,old,.5f,forward);assertEquals(Color.BLUE,frame.getPixel(if(forward) 10 else 390,300));assertTrue(Color.red(frame.getPixel(if(forward) 390 else 10,300))>150) };frame.eraseColor(Color.BLUE);drawPageCurl(c,old,1f,true);assertEquals(Color.BLUE,frame.getPixel(200,300)) } finally { old.recycle();frame.recycle() }
  }
 }
}
