package com.msoumaya.androidcoran
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ReaderAnnotationsAndroidTest {
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private val context get()=instrumentation.targetContext
 private val repo get()=(context.applicationContext as CoranApplication).repository
 @Test fun stylusOnlyRejectsFingerInBothPageAndHandwrittenComment() {
  runBlocking { repo.awaitReady() };val data=repo.quran.qcfData(context,1)
  instrumentation.runOnMainSync {
   val note=HandwritingView(context);note.layout(0,0,400,200);note.stylusOnly=true;var notes=0;note.onStroke={notes++}
   val page=MushafView(context);page.load(repo.quran,1,"coranTest",data,null,null);page.layout(0,0,1000,2120);page.annotationMode=true;page.stylusOnly=true;var marks=0;var turns=0;val active=mutableListOf<Boolean>();page.onInkStroke={_,_->marks++};page.onPage={turns++};page.onInkActive={active+=it}
   val bitmap=Bitmap.createBitmap(1000,2120,Bitmap.Config.ARGB_8888);try { page.draw(Canvas(bitmap)) } finally { bitmap.recycle() }
   listOf(note,page).forEach { target -> listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP).forEachIndexed { i,action -> val e=MotionEvent.obtain(1,1L+i,action,40f+i*40,40f,0);try { target.dispatchTouchEvent(e) } finally { e.recycle() } } }
   assertEquals(0,notes);assertEquals(0,marks)
   sendReaderStylus(note,listOf(.1f to .2f,.5f to .5f,.9f to .8f));sendReaderStylus(page,listOf(.1f to .2f,.5f to .2f,.9f to .2f))
   assertEquals(1,notes);assertEquals(1,marks);assertEquals(0,turns);assertEquals(listOf(true,false),active)
  }
 }
 @Test fun qcfLongPressSelectionUsesRealWordIdentityAndVerseRegions() {
  runBlocking { repo.awaitReady() };val data=repo.quran.qcfData(context,1)
  instrumentation.runOnMainSync {
   val view=MushafView(context);view.load(repo.quran,1,"coranTest",data,null,null);view.layout(0,0,1000,2120)
   val image=Bitmap.createBitmap(1000,2120,Bitmap.Config.ARGB_8888)
   try {
    view.draw(Canvas(image));var hit:kotlinx.serialization.json.JsonObject?=null
    for(y in 200..1900 step 30) { for(x in 40..960 step 20) { val s=view.selectionAt(x.toFloat(),y.toFloat());if(s!=null&&s.num("wordId")>0) { hit=s;break } };if(hit!=null) break }
    assertNotNull(hit);assertTrue(hit!!.str("wordText").isNotBlank());assertTrue(hit!!.num("wordIndex")>=1);assertEquals(1,hit!!.arr("regions").size);assertTrue(hit!!.arr("verseRegions").isNotEmpty())
   } finally { image.recycle() }
  }
 }
 @Test fun pngAndMultipagePdfKeepOriginalPageGeometryAndVisibleInk() = runBlocking {
  repo.awaitReady()
  val stroke=InkStroke(listOf(InkPoint(.1f,.2f),InkPoint(.3f,.3f)))
  val state=saveReaderInk(defaultState(),newReaderInk(1,stroke,listOf(1)).copy(color="#16A34A",width=.01f))
  val png=exportAnnotationPages(context,repo.quran,state,"coranTest",listOf(1),false)
  try {
   val bitmap=BitmapFactory.decodeFile(png.absolutePath);try { assertEquals(1000,bitmap.width);assertEquals(2120,bitmap.height);val p=bitmap.getPixel(200,530);assertTrue(Color.green(p)>Color.red(p)+40) } finally { bitmap.recycle() }
   val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".annotation-exports",png);assertEquals("content",uri.scheme)
  } finally { png.delete() }
  val pdf=exportAnnotationPages(context,repo.quran,state,"coranTest",listOf(1,2),true)
  try { ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { renderer -> assertEquals(2,renderer.pageCount);renderer.openPage(0).use { page -> assertEquals(595,page.width);assertEquals(842,page.height) } } } } finally { pdf.delete() }
 }
 @Test fun eraserRecordsOneDeletionPerMarkEvenWhileStateUpdateIsPending() {
  runBlocking { repo.awaitReady() };val data=repo.quran.qcfData(context,1)
  instrumentation.runOnMainSync {
   val view=MushafView(context);view.load(repo.quran,1,"coranTest",data,null,null);view.layout(0,0,1000,2120);view.annotationMode=true;view.stylusOnly=true;view.inkTool="eraser"
   view.annotations=listOf(newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f),InkPoint(.3f,.2f))),listOf(1)))
   val bitmap=Bitmap.createBitmap(1000,2120,Bitmap.Config.ARGB_8888);try { view.draw(Canvas(bitmap)) } finally { bitmap.recycle() }
   var erased=0;var created=0;view.onInkErase={erased++};view.onInkStroke={_,_->created++}
   sendReaderStylus(view,listOf(.1f to .2f,.15f to .2f,.2f to .2f,.25f to .2f,.3f to .2f))
   assertEquals(1,erased);assertEquals(0,created)
  }
 }

}
