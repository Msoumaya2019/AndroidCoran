package com.msoumaya.androidcoran
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ReaderInkAndroidTest {
 @get:Rule val compose=createComposeRule()
 private fun event(action: Int,x: Float,y: Float,time: Long)=MotionEvent.obtain(1,time,action,1,arrayOf(MotionEvent.PointerProperties().apply { id=0;toolType=MotionEvent.TOOL_TYPE_STYLUS }),arrayOf(MotionEvent.PointerCoords().apply { this.x=x;this.y=y;pressure=.7f;size=.1f }),0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_STYLUS,0)
 @Test fun stylusPressureAndCoordinatesPersistAndRenderAfterResize() {
  compose.runOnIdle {
   val view=HandwritingView(InstrumentationRegistry.getInstrumentation().targetContext);view.layout(0,0,400,200)
   var captured: InkStroke?=null;view.onStroke={captured=it}
   listOf(event(MotionEvent.ACTION_DOWN,40f,40f,1),event(MotionEvent.ACTION_MOVE,200f,100f,2),event(MotionEvent.ACTION_UP,360f,160f,3)).forEach { e -> try { view.dispatchTouchEvent(e) } finally { e.recycle() } }
   val stroke=captured!!;assertEquals(.1f,stroke.points.first().x,.001f);assertEquals(.2f,stroke.points.first().y,.001f);assertEquals(.7f,stroke.points.first().pressure,.001f)
   view.strokes=listOf(stroke);view.layout(0,0,800,400)
   val image=Bitmap.createBitmap(800,400,Bitmap.Config.ARGB_8888)
   try { view.draw(Canvas(image));assertTrue(android.graphics.Color.red(image.getPixel(400,200))<100) } finally { image.recycle() }
  }
 }
 @Test fun mushafCapturesFreehandCoordinatesInThePageFrame() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repo=(context.applicationContext as CoranApplication).repository
  kotlinx.coroutines.runBlocking { repo.awaitReady() }
  val data=repo.quran.qcfData(context,1)
  compose.runOnIdle {
   val view=MushafView(context);view.load(repo.quran,1,"coranTest",data,null,null);view.layout(0,0,600,1060)
   val image=Bitmap.createBitmap(600,1060,Bitmap.Config.ARGB_8888)
   try {
    view.draw(Canvas(image));view.annotationMode=true
    var captured: InkStroke?=null;view.onInkStroke={stroke,_ -> captured=stroke}
    // At this size the 1000x2120 page is scaled by 0.5 and centered at x=50.
    listOf(event(MotionEvent.ACTION_DOWN,100f,212f,1),event(MotionEvent.ACTION_MOVE,250f,424f,2),event(MotionEvent.ACTION_UP,100f,212f,3)).forEach { e -> try { view.dispatchTouchEvent(e) } finally { e.recycle() } }
    val stroke=captured!!;assertEquals(.1f,stroke.points.first().x,.001f);assertEquals(.2f,stroke.points.first().y,.001f)
    view.annotations=listOf(newReaderInk(1,stroke,emptyList()));view.annotationMode=false;view.layout(0,0,1200,2120)
    val enlarged=Bitmap.createBitmap(1200,2120,Bitmap.Config.ARGB_8888)
    try { view.draw(Canvas(enlarged));val color=enlarged.getPixel(200,424);assertTrue(android.graphics.Color.red(color)>140);assertTrue(android.graphics.Color.green(color)<90) } finally { enlarged.recycle() }
   } finally { image.recycle() }
  }
 }
 @Test fun cancelledStylusStrokeIsNotCommitted() {
  compose.runOnIdle {
   val view=HandwritingView(InstrumentationRegistry.getInstrumentation().targetContext);view.layout(0,0,400,200);var commits=0;view.onStroke={commits++}
   listOf(event(MotionEvent.ACTION_DOWN,40f,40f,1),event(MotionEvent.ACTION_CANCEL,100f,100f,2)).forEach { e -> try { view.dispatchTouchEvent(e) } finally { e.recycle() } };assertEquals(0,commits)
  }
 }
 @Test fun handwrittenNoteRestoresAndDeletionRequiresConfirmation() {
  val outline=InkStroke(listOf(InkPoint(.1f,.1f),InkPoint(.3f,.2f)))
  val ink=newReaderInk(1,outline,listOf(1));val latest=androidx.compose.runtime.mutableStateOf(ink);var deleted=false
  val restore=StateRestorationTester(compose)
  restore.setContent { MaterialTheme { ReaderInkEditor(latest.value,{latest.value=it},{deleted=true},{}) } }
  compose.onNodeWithContentDescription("Zone de note manuscrite").performTouchInput { swipe(centerLeft,centerRight,300) }
  compose.runOnIdle { assertEquals(1,latest.value.note.size) }
  restore.emulateSavedInstanceStateRestore()
  compose.onNodeWithContentDescription("Zone de note manuscrite").performTouchInput { swipe(topCenter,bottomCenter,300) }
  compose.runOnIdle { assertEquals(2,latest.value.note.size) }
  compose.onNodeWithText("Annuler le trait").performClick();compose.runOnIdle { assertEquals(1,latest.value.note.size) }
  compose.onNodeWithText("Supprimer l’annotation").performClick();compose.runOnIdle { assertFalse(deleted) }
  compose.onNodeWithText("Conserver").performClick();compose.runOnIdle { assertFalse(deleted) }
  compose.onNodeWithText("Supprimer l’annotation").performClick();compose.onNodeWithText("Supprimer",substring=false).performClick();compose.runOnIdle { assertTrue(deleted) }
 }
}
