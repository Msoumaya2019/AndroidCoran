package com.msoumaya.androidcoran.domain

const val LEGACY_NOTE_ASPECT=1.4f
data class NoteFrame(val left: Float,val top: Float,val width: Float,val height: Float) {
 val aspect get()=width/height
}
fun noteContentFrame(strokes: List<InkStroke>,aspect: Float): NoteFrame {
 val points=strokes.flatMap {it.points};if(points.isEmpty()) return NoteFrame(0f,0f,aspect,1f)
 val left=(points.minOf {it.x}-.02f).coerceAtLeast(0f)*aspect
 val top=(points.minOf {it.y}-.02f).coerceAtLeast(0f)
 val right=(points.maxOf {it.x}+.02f).coerceAtMost(1f)*aspect
 val bottom=(points.maxOf {it.y}+.02f).coerceAtMost(1f)
 return NoteFrame(left,top,(right-left).coerceAtLeast(.02f),(bottom-top).coerceAtLeast(.02f))
}
data class NoteTransform(val scale: Float,val x: Float,val y: Float) {
 fun point(px: Float,py: Float,aspect: Float)=InkPoint(((px-x)/scale/aspect).coerceIn(0f,1f),((py-y)/scale).coerceIn(0f,1f))
}
fun noteTransform(width: Float,height: Float,frame: NoteFrame): NoteTransform {
 val scale=minOf(width/frame.width,height/frame.height).coerceAtLeast(.0001f)
 return NoteTransform(scale,(width-frame.width*scale)/2-frame.left*scale,(height-frame.height*scale)/2-frame.top*scale)
}
