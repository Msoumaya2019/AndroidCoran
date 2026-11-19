package com.msoumaya.androidcoran
import android.view.*
internal inline fun <reified T: View> findReaderView(root: View): T? {
 val pending=java.util.ArrayDeque<View>();pending.add(root)
 while(pending.isNotEmpty()) { val v=pending.removeFirst();if(v is T) return v;if(v is ViewGroup) for(i in 0 until v.childCount) pending.add(v.getChildAt(i)) };return null
}
internal fun sendReaderStylus(target: View,points: List<Pair<Float,Float>>) {
 val down=android.os.SystemClock.uptimeMillis()
 points.forEachIndexed { i,p ->
  val action=when(i) { 0->MotionEvent.ACTION_DOWN;points.lastIndex->MotionEvent.ACTION_UP;else->MotionEvent.ACTION_MOVE }
  val event=MotionEvent.obtain(down,down+i*20,action,1,arrayOf(MotionEvent.PointerProperties().apply { id=0;toolType=MotionEvent.TOOL_TYPE_STYLUS }),arrayOf(MotionEvent.PointerCoords().apply { x=target.width*p.first;y=target.height*p.second;pressure=.8f;size=.1f }),0,0,1f,1f,0,0,InputDevice.SOURCE_STYLUS,0)
  try { target.dispatchTouchEvent(event) } finally { event.recycle() }
 }
}

internal fun captureReaderState(label: String,semantics: String) {
 val i=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();val dir=i.targetContext.getExternalFilesDir(null)?:return
 java.io.File(dir,label+"-semantics.txt").writeText(semantics)
 val bitmap=i.uiAutomation.takeScreenshot()?:return
 try { java.io.File(dir,label+"-screen.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() }
}
