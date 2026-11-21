package com.msoumaya.androidcoran.domain
const val NOTE_MARGIN_X=.35f
const val NOTE_MARGIN_Y=.08f
fun workspacePoint(x: Float,y: Float,margins: Boolean)=InkPoint(x.coerceIn(if(margins) -NOTE_MARGIN_X else 0f,if(margins) 1+NOTE_MARGIN_X else 1f),y.coerceIn(if(margins) -NOTE_MARGIN_Y else 0f,if(margins) 1+NOTE_MARGIN_Y else 1f))
fun workspaceDimensions(width: Float,height: Float,enabled: Boolean)=if(enabled) width*(1+2*NOTE_MARGIN_X) to height*(1+2*NOTE_MARGIN_Y) else width to height
fun hasMarginInk(notes: List<ReaderInk>)=notes.any { it.anchor==null&&it.outlines.any { stroke -> stroke.points.any { p -> p.x<0||p.x>1||p.y<0||p.y>1 } } }
data class TabletReaderControls(val canAnnotate: Boolean,val showTools: Boolean,val verticalTools: Boolean,val floatingTools: Boolean)
fun tabletReaderControls(tablet: Boolean,doublePage: Boolean,fullscreen: Boolean,annotating: Boolean,toolsVisible: Boolean,sidebars: Boolean): TabletReaderControls { val can=tablet&&!doublePage;val show=can&&toolsVisible&&(!fullscreen||annotating);return TabletReaderControls(can,show,show&&sidebars&&!fullscreen,show&&(!sidebars||fullscreen)) }
