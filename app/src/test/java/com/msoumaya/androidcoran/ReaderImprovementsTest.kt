package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test

class ReaderImprovementsTest {
 @Test fun focalPaperCoordinateIsStableThroughRepeatedZooms() {
  var t=ReaderTransform(1f,34f,-17f);val originX=260f;val originY=54f
  val x=720f;val y=350f;val px=(x-originX-t.x)/t.zoom;val py=(y-originY-t.y)/t.zoom
  for(factor in listOf(1.2f,1.5f,.8f,2f,.4f)) {
   t=t.scale(factor,x,y,originX,originY)
   assertEquals(px,(x-originX-t.x)/t.zoom,.001f);assertEquals(py,(y-originY-t.y)/t.zoom,.001f)
  }
 }
 @Test fun fullSurfaceInkSurvivesSerializationAndExportBounds() {
  val stroke=InkStroke(listOf(workspacePoint(-1.2f,-.4f,true),workspacePoint(2.5f,1.8f,true)))
  val ink=newReaderInk(1,stroke,listOf(1))
  assertEquals(ink,inkFromJson(ink.toSourceJson()))
  val box=inkWorkspace(listOf(ink),true)
  assertTrue(box.left< -1.2f);assertTrue(box.top< -.4f);assertTrue(box.right>2.5f);assertTrue(box.bottom>1.8f)
 }
 @Test fun titleCategoriesAndIndependentIconColorSurviveBothCopiesAndMerge() {
  val legacy=newReaderInk(1,InkStroke(listOf(InkPoint(.2f,.3f))),listOf(1))
  val ink=legacy.copy(title="Ma révision",categories=listOf(noteCategories[0],"Ma catégorie"),iconColor="#7C3AED",text="Texte",note=listOf(InkStroke(listOf(InkPoint(.3f,.4f)))))
  assertEquals(ink,inkFromJson(ink.toJson()));assertEquals(ink,inkFromJson(ink.toSourceJson()))
  assertTrue(ink.canSaveNote());assertFalse(legacy.canSaveNote())
  val saved=saveReaderInk(defaultState(),ink)
  assertEquals(ink,allReaderInk(saved).single());assertEquals(legacy.outlines,allReaderInk(saved).single().outlines)
  assertEquals("#AD2A30",ink.color)
 }
 @Test fun legacyNotesAreReadableWithoutInventedCategories() {
  val old=json("id" to "legacy","page" to 604,"text" to "Historique","points" to listOf(json("x" to .2,"y" to .3)))
  val note=inkFromJson(old)!!
  assertEquals("Historique",note.text);assertEquals(emptyList<String>(),note.categories);assertEquals("#16A34A",note.iconColor)
 }
 @Test fun categoriesFilterIndependentlyOfIconAndMarkColors() {
  val a=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1)).copy(categories=listOf("A","B"),iconColor="#DB2777")
  val b=a.copy(id="other",categories=listOf("C"))
  assertEquals(listOf(a),notebookEntries(listOf(a,b),NotebookFilter(category="B"),emptySet()) { 1 })
  assertEquals(emptyList<ReaderInk>(),notebookEntries(listOf(a,b),NotebookFilter(category="missing"),emptySet()) { 1 })
 }
 @Test fun curlPreferenceDefaultsOnAndCanBePersistentlyDisabled() {
  assertTrue(TabletPreferences.from(json()).pageCurl)
  assertFalse(TabletPreferences.from(TabletPreferences(pageCurl=false).toJson()).pageCurl)
 }
}
