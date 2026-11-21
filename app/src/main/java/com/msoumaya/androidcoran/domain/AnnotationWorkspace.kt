package com.msoumaya.androidcoran.domain
const val NOTE_MARGIN_X=.35f
const val NOTE_MARGIN_Y=.08f
fun workspacePoint(x: Float,y: Float,margins: Boolean)=InkPoint(if(margins) x.coerceIn(-64f,65f) else x.coerceIn(0f,1f),if(margins) y.coerceIn(-64f,65f) else y.coerceIn(0f,1f))
fun workspaceDimensions(width: Float,height: Float,enabled: Boolean)=if(enabled) width*(1+2*NOTE_MARGIN_X) to height*(1+2*NOTE_MARGIN_Y) else width to height
fun hasMarginInk(notes: List<ReaderInk>)=notes.any { it.anchor==null&&it.outlines.any { stroke -> stroke.points.any { p -> p.x<0||p.x>1||p.y<0||p.y>1 } } }
data class InkWorkspace(val left: Float,val top: Float,val right: Float,val bottom: Float)
fun inkWorkspace(notes: List<ReaderInk>,margins: Boolean): InkWorkspace {
 val points=notes.filter { it.anchor==null }.flatMap { it.outlines }.flatMap { it.points }
 val mx=if(margins) NOTE_MARGIN_X else 0f;val my=if(margins) NOTE_MARGIN_Y else 0f
 return InkWorkspace(minOf(-mx,points.minOfOrNull { it.x-.01f }?:0f),minOf(-my,points.minOfOrNull { it.y-.01f }?:0f),maxOf(1+mx,points.maxOfOrNull { it.x+.01f }?:1f),maxOf(1+my,points.maxOfOrNull { it.y+.01f }?:1f))
}
data class TabletReaderControls(val canAnnotate: Boolean,val showTools: Boolean,val verticalTools: Boolean,val floatingTools: Boolean)
fun tabletReaderControls(tablet: Boolean,doublePage: Boolean,fullscreen: Boolean,annotating: Boolean,toolsVisible: Boolean,sidebars: Boolean): TabletReaderControls { val can=!doublePage;val show=can&&toolsVisible&&(!fullscreen||annotating);return TabletReaderControls(can,show,show&&sidebars&&!fullscreen,show&&(!sidebars||fullscreen)) }
