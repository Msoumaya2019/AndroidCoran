package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class NoteCanvasTest {
 @Test fun resizingUsesTheSameScaleForBothAxesAndInvertsInput() {
  val frame=NoteFrame(0f,0f,2f,1f)
  for((w,h) in listOf(400f to 200f,400f to 500f,900f to 300f,180f to 500f)) {
   val t=noteTransform(w,h,frame);val p=InkPoint(.35f,.6f,.8f)
   val screenX=t.x+p.x*2*t.scale;val screenY=t.y+p.y*t.scale
   assertEquals(p.x,t.point(screenX,screenY,2f).x,.0001f);assertEquals(p.y,t.point(screenX,screenY,2f).y,.0001f)
   assertEquals(.2f*t.scale,(t.x+.6f*t.scale)-(t.x+.4f*t.scale),.0001f)
  }
 }
 @Test fun compactPreviewCropsWhitespaceWithoutChangingPoints() {
  val strokes=listOf(InkStroke(listOf(InkPoint(.4f,.45f),InkPoint(.6f,.55f))))
  val copy=strokes.toList();val f=noteContentFrame(strokes,2f)
  assertTrue(f.width<.5f);assertTrue(f.height<.2f);assertEquals(copy,strokes)
  val t=noteTransform(400f,200f,f)
  assertEquals(4f,((.6f-.4f)*2*t.scale)/((.55f-.45f)*t.scale),.001f)
  assertEquals(f.width/f.height,(f.width*t.scale)/(f.height*t.scale),.001f)
 }
 @Test fun aspectIsAdditiveAndLegacyNotesAreUntouched() {
  val legacy=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1)).copy(note=listOf(InkStroke(listOf(InkPoint(.3f,.5f)))))
  assertNull(inkFromJson(legacy.toJson())!!.noteAspect)
  val modern=legacy.copy(noteAspect=2.3f,categories=listOf("Note personnelle"))
  assertEquals(modern,inkFromJson(modern.toSourceJson()));assertEquals(modern,allReaderInk(saveReaderNote(defaultState(),modern)).single())
  val invalid=modern.toJson().with("noteAspect" to JsonPrimitive(-3f));assertNull(inkFromJson(invalid)!!.noteAspect)
  assertEquals(legacy.note,inkFromJson(modern.toSourceJson())!!.note)
 }
 @Test fun invalidSaveDoesNotReplaceAnExistingNote() {
  val ink=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1)).copy(categories=listOf("Mon cours"),text="Texte original")
  val original=saveReaderNote(defaultState(),ink)
  assertTrue(original.obj("reader").arr("noteCategories").any {it.jsonPrimitive.content=="Mon cours"})
  assertThrows(IllegalArgumentException::class.java) {saveReaderNote(original,ink.copy(categories=emptyList(),text="Autre"))}
  assertEquals("Texte original",allReaderInk(original).single().text)
 }
}
