package com.msoumaya.androidcoran
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class MushafLayoutTest {
 @Test fun fractionalSourceFontSizesKeepReferenceLinesWithinTheirMargins() {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    val q=(context.applicationContext as CoranApplication).repository.quran
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    assertEquals(56.928294f,q.qcfData(context,582).qcfFontSize(),0.0001f)
    for(page in listOf(1,2,100,582,604)) {
      val document=q.qcfData(context,page)
      paint.typeface=Typeface.createFromAsset(context.assets,"fonts/$page.ttf");paint.textSize=document.qcfFontSize()
      for(raw in document.arr("lines")) {
        val line=raw.jsonObject;if(line.str("type")!="ayah") continue
        val words=line.arr("words");val limit=if(line.flag("centered")) 1000.0 else 940.0
        val width=words.sumOf { paint.measureText(it.jsonArray[4].jsonPrimitive.content).toDouble() }+if(line.flag("centered")) 8.0*(words.size-1).coerceAtLeast(0) else 0.0
        assertTrue("Page $page ligne "+line.num("line")+" dépasse la largeur de référence $limit : $width",width<=limit+1.0)
      }
    }
 }
}
