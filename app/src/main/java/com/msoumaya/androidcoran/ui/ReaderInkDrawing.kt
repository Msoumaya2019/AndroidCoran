package com.msoumaya.androidcoran.ui
import android.graphics.*
import com.msoumaya.androidcoran.domain.*
import kotlin.math.*
internal fun drawReaderMark(canvas: Canvas,ink: ReaderInk,zones: List<RectF>,w: Float,h: Float) {
 val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=runCatching { Color.parseColor(ink.color) }.getOrDefault(Color.RED);strokeWidth=ink.width*w;style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;alpha=if(ink.penStyle=="pencil") 155 else 255 }
 val points=ink.outlines.firstOrNull()?.points.orEmpty()
 val boxes=if(zones.isNotEmpty()) zones else if(points.isNotEmpty()) listOf(RectF(points.first().x*w,points.first().y*h,points.last().x*w,points.last().y*h).apply { sort() }) else emptyList()
 when(ink.tool) {
  "highlight" -> { paint.style=Paint.Style.FILL;paint.alpha=70;paint.xfermode=PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);if(zones.isNotEmpty()) zones.forEach { canvas.drawRect(it,paint) } else { paint.style=Paint.Style.STROKE;paint.strokeWidth=ink.width*w;val path=Path();points.forEachIndexed { i,p -> if(i==0) path.moveTo(p.x*w,p.y*h) else path.lineTo(p.x*w,p.y*h) };canvas.drawPath(path,paint) } }
  "circle" -> boxes.forEach { val r=RectF(it);r.inset(-w*.004f,-h*.002f);canvas.drawOval(r,paint) }
  "underline" -> boxes.forEach { canvas.drawLine(it.left,it.bottom,it.right,it.bottom,paint) }
  "arrow" -> if(points.size>=2) { val a=points.first();val b=points.last();val x=a.x*w;val y=a.y*h;val ex=b.x*w;val ey=b.y*h;canvas.drawLine(x,y,ex,ey,paint);val angle=atan2(ey-y,ex-x);val size=minOf(w*.035f,hypot(ex-x,ey-y)*.3f);canvas.drawLine(ex,ey,ex-size*cos(angle-.5f),ey-size*sin(angle-.5f),paint);canvas.drawLine(ex,ey,ex-size*cos(angle+.5f),ey-size*sin(angle+.5f),paint) }
  "note" -> { val r=boxes.firstOrNull();if(r!=null) { paint.style=Paint.Style.FILL;canvas.drawCircle(r.centerX(),r.top,w*.012f,paint) } }
  else -> ink.outlines.forEach { stroke -> stroke.points.zipWithNext().forEach { (a,b) -> paint.strokeWidth=ink.width*w*(.4f+.8f*b.pressure.coerceIn(.25f,1f));canvas.drawLine(a.x*w,a.y*h,b.x*w,b.y*h,paint) };if(stroke.points.size==1) { val p=stroke.points.first();paint.style=Paint.Style.FILL;canvas.drawCircle(p.x*w,p.y*h,ink.width*w/2,paint) } }
 }
}
