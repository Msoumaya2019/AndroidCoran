package com.msoumaya.androidcoran.ui
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.msoumaya.androidcoran.domain.*

internal fun drawHandwriting(canvas: Canvas,strokes: List<InkStroke>,width: Float,height: Float,color: Int=Color.rgb(173,42,48)) {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;style=Paint.Style.STROKE }
    strokes.forEach { stroke ->
        paint.color=stroke.color?.let { runCatching { Color.parseColor(it) }.getOrNull() }?:color
        stroke.points.zipWithNext().forEach { (a,b) -> paint.strokeWidth=(width*.0027f*(.5f+b.pressure*.5f)).coerceAtLeast(1f);canvas.drawLine(a.x*width,a.y*height,b.x*width,b.y*height,paint) }
        if(stroke.points.size==1) { val p=stroke.points.first();paint.style=Paint.Style.FILL;canvas.drawCircle(p.x*width,p.y*height,width*.0017f,paint);paint.style=Paint.Style.STROKE }
    }
}
class HandwritingView(context: Context): View(context) {
    var strokes: List<InkStroke> = emptyList();set(value) { field=value;invalidate() }
    var stylusOnly=false
    var onStroke: (InkStroke)->Unit={}
    private val draft=mutableListOf<InkPoint>()
    private var drawing=false
    init { contentDescription="Zone de note manuscrite";isSaveEnabled=false;setBackgroundColor(Color.WHITE) }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas);drawHandwriting(canvas,strokes+if(draft.isEmpty()) emptyList() else listOf(InkStroke(draft)),width.toFloat(),height.toFloat(),Color.rgb(28,39,43)) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(width==0||height==0) return false
        if(stylusOnly&&event.getToolType(0)!=MotionEvent.TOOL_TYPE_STYLUS) return false
        if(event.pointerCount>1) { drawing=false;draft.clear();invalidate();return true }
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { drawing=true;parent?.requestDisallowInterceptTouchEvent(true);draft.clear();add(event) }
            MotionEvent.ACTION_MOVE -> { if(!drawing) return true;for(i in 0 until event.historySize) append(event.getHistoricalX(i),event.getHistoricalY(i),event.getHistoricalPressure(i));add(event) }
            MotionEvent.ACTION_UP -> { if(!drawing) return true;drawing=false;add(event);val stroke=InkStroke(draft.toList());draft.clear();onStroke(stroke);parent?.requestDisallowInterceptTouchEvent(false);performClick() }
            MotionEvent.ACTION_CANCEL -> { drawing=false;draft.clear();parent?.requestDisallowInterceptTouchEvent(false) }
        }
        invalidate();return true
    }
    private fun add(e: MotionEvent)=append(e.x,e.y,e.pressure)
    private fun append(x: Float,y: Float,pressure: Float) { if(draft.size<12000) draft+=InkPoint((x/width).coerceIn(0f,1f),(y/height).coerceIn(0f,1f),pressure.coerceIn(.1f,1f)) }
    override fun performClick(): Boolean { super.performClick();return true }
}
