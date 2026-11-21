package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class AnnotationWorkspaceTest {
 @Test fun marginsAllowTheEntireVisibleWorkspace() { assertEquals(InkPoint(-.2f,.2f),workspacePoint(-.2f,.2f,true));assertEquals(0f,workspacePoint(-.2f,.2f,false).x,0f);assertEquals(-2f,workspacePoint(-2f,0f,true).x,0f);assertEquals(3f,workspacePoint(0f,3f,true).y,.00001f) }
 @Test fun workspaceMatchesReferenceProportions() { val size=workspaceDimensions(1000f,2120f,true);assertEquals(1700f,size.first,.001f);assertEquals(2459.2f,size.second,.001f) }
 @Test fun marginInkRoundTripsWithoutClippingButHandwrittenCommentStaysInItsPad() { val ink=newReaderInk(1,InkStroke(listOf(InkPoint(-.2f,.2f),InkPoint(1.25f,.5f))),listOf(1)).copy(note=listOf(InkStroke(listOf(InkPoint(.1f,.2f)))));assertEquals(ink,inkFromJson(ink.toSourceJson()));assertTrue(hasMarginInk(listOf(ink)));assertFalse(hasMarginInk(listOf(ink.copy(anchor=json("verseId" to 1))))) }
 @Test fun narrowLandscapeSpreadDoesNotReserveAnnotationPanels() { val p=TabletPreferences(spread="double");val layout=tabletLayout(p,"tablet",1024f,768f,true);assertTrue(layout.doublePage);assertTrue(layout.notesOverlay);assertEquals(1024f,layout.centerWidth,0f) }
 @Test fun fullscreenAnnotationKeepsFloatingToolsAndSpreadIsAlwaysReadOnly() { val c=tabletReaderControls(true,false,true,true,true,true);assertTrue(c.canAnnotate);assertTrue(c.floatingTools);assertFalse(c.verticalTools);for(full in listOf(false,true)) { val d=tabletReaderControls(true,true,full,true,true,true);assertFalse(d.canAnnotate);assertFalse(d.showTools) };assertFalse(tabletReaderControls(true,false,true,false,true,true).showTools) }
}
