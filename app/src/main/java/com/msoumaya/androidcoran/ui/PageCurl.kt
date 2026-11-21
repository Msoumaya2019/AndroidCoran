package com.msoumaya.androidcoran.ui
import android.graphics.*
import kotlin.math.*

/** A cylindrical paper fold, rendered as a shaded bitmap mesh. No text reflow or per-frame allocation. */
internal class CurlMesh {
 val columns=40;val rows=16
 val vertices=FloatArray((columns+1)*(rows+1)*2)
 val colors=IntArray((columns+1)*(rows+1))
 val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
}
private val curlMesh=ThreadLocal.withInitial { CurlMesh() }
internal fun drawPageCurl(canvas: Canvas,page: Bitmap,progress: Float,forward: Boolean) {
 if(progress>=1f) return
 if(progress<=0f) { canvas.drawBitmap(page,0f,0f,null);return }
 val m=curlMesh.get()!!;val w=page.width.toFloat();val h=page.height.toFloat()
 val p=progress.coerceIn(0f,1f);val radius=w*.1f;val fold=-radius+p*(w+radius*2)
 // Mirroring both coordinates and texture preserves the writing for a reverse turn.
 canvas.save()
 val shadow=Paint().apply { shader=LinearGradient(fold-radius,0f,fold+radius,0f,intArrayOf(Color.TRANSPARENT,0x55000000,Color.TRANSPARENT),null,Shader.TileMode.CLAMP) }
 canvas.drawRect(maxOf(0f,fold-radius),0f,minOf(w,fold+radius),h,shadow)
 for(row in 0..m.rows) for(col in 0..m.columns) {
  val original=col*w/m.columns;val u=if(forward) original else w-original
  val distance=u-fold;val angle=(distance/radius).coerceIn(0f,PI.toFloat())
  val projected=when { distance<=0 -> fold-distance;distance<PI.toFloat()*radius -> fold+radius*sin(angle);else -> u }
  val lifted=if(distance>0&&distance<PI.toFloat()*radius) sin(angle)*radius else 0f
  val i=row*(m.columns+1)+col
  // The turned portion moves off-screen; the visible edge bends around the cylinder.
  val x=if(distance<=0) fold-distance else projected
  m.vertices[2*i]=if(forward) x else w-x
  val v=row*h/m.rows;m.vertices[2*i+1]=v+(v/h-.5f)*lifted*.12f
  val light=(255-(sin(angle)*65).toInt()).coerceIn(160,255)
  m.colors[i]=Color.rgb(light,light,light)
 }
 // Clip the turned-away half. The narrow fold remains curved and shaded.
 if(forward) canvas.clipRect(fold.coerceIn(0f,w),0f,w,h) else canvas.clipRect(0f,0f,(w-fold).coerceIn(0f,w),h)
 canvas.drawBitmapMesh(page,m.columns,m.rows,m.vertices,0,m.colors,0,m.paint)
 canvas.restore()
}
