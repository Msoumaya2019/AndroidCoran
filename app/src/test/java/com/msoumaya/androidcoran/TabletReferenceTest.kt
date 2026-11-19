package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class TabletReferenceTest {
 @Test fun automaticModeUsesDeviceAndRealWindow() {
  val p=TabletPreferences();assertFalse(tabletLayout(p,"phone",1280f,800f).enabled);assertTrue(tabletLayout(p,"tablet",600f,800f).enabled);assertFalse(tabletLayout(p,"tablet",599f,800f).enabled);assertTrue(tabletLayout(p,"unknown",768f,900f).enabled);assertFalse(tabletLayout(p,"unknown",767f,900f).enabled)
  assertTrue(tabletLayout(p.copy(mode="on"),"phone",390f,850f).enabled);assertFalse(tabletLayout(p.copy(mode="off"),"tablet",1400f,900f).enabled)
 }
 @Test fun spreadRequiresLandscapeAndSufficientCenterWidth() {
  val p=TabletPreferences(spread="double");assertTrue(tabletLayout(p,"tablet",1100f,700f).doublePage);assertTrue(tabletLayout(p,"tablet",1000f,600f,true).doublePage);assertFalse(tabletLayout(p,"tablet",639f,500f).doublePage);assertFalse(tabletLayout(p,"tablet",768f,1024f).doublePage)
 }
 @Test fun pairsAreRtlAndStayInsideQuran() { assertEquals(listOf(2,1),spreadPages(1,true));assertEquals(listOf(2,1),spreadPages(2,true));assertEquals(listOf(604,603),spreadPages(604,true));assertEquals(3,nextSpread(1,1,true));assertEquals(1,nextSpread(2,-1,true));assertEquals(603,nextSpread(604,1,true)) }
 @Test fun invalidPreferencesCannotInjectInvalidInk() { val p=TabletPreferences.from(json("mode" to "invalid","penWidth" to -4,"color" to "url(fake)","spread" to "invalid"));assertEquals("auto",p.mode);assertEquals(.001f,p.penWidth,0f);assertEquals("#16A34A",p.color);assertEquals("single",p.spread);assertTrue(p.stylusOnly) }
 @Test fun exportRangesAreUniqueLimitedAndValidated() { assertEquals(listOf(1,3,4,5),exportPageNumbers("5, 1,3-5"));listOf("0","605","5-3","1-21","abc","").forEach { assertTrue(runCatching { exportPageNumbers(it) }.isFailure) } }
 @Test fun sourceAnnotationsAndLegacyNotesRemainReadableWithoutDuplication() {
  val old=newReaderInk(2,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(8),"2026-11-19T10:00:00.000Z")
  val state=saveReaderInk(defaultState(),old);assertEquals(1,readerInk(state,2).size)
  val canonical=old.copy(text="commentaire",source="coran_1441",updatedAt="2026-11-19T10:01:00.000Z").toSourceJson()
  val remote=json("readerAnnotations" to json(old.id to canonical));assertTrue(readerInk(remote,2).isEmpty());assertEquals("commentaire",readerInk(remote,2,"coran_1441").single().text)
 }
 @Test fun explicitUndoCanRestoreNewerThanTombstoneButStaleSaveCannot() {
  val old=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1),"2026-11-19T10:00:00.000Z")
  val deleted=deleteReaderInk(saveReaderInk(defaultState(),old),old.id,"2026-11-19T10:01:00.000Z")
  assertEquals(deleted,saveReaderInk(deleted,old));val restored=saveReaderInk(deleted,old.copy(updatedAt="2026-11-19T10:02:00.000Z"),true)
  assertEquals(1,readerInk(restored,1).size);assertEquals(1,readerInk(json("readerAnnotations" to mergeSourceAnnotations(deleted.obj("readerAnnotations"),restored.obj("readerAnnotations"))),1).size)
 }
 @Test fun sourceTimestampsAreMonotonicWithMillisecondFormat() { val t=annotationTime("2099-01-01T00:00:00.000Z");assertEquals("2099-01-01T00:00:00.001Z",t) }
 @Test fun notebookFiltersDoNotValidateMemorization() {
  val a=newReaderInk(1,InkStroke(listOf(InkPoint(.1f,.2f))),listOf(1)).copy(favorite=true,text="Texte",source="coranTest")
  val b=a.copy(id="other",page=2,verses=listOf(8),favorite=false,masteredAt="2026-11-19T00:00:00Z")
  assertEquals(listOf(a),notebookEntries(listOf(a,b),NotebookFilter(favorite=true),setOf(1,8)){if(it<=7) 1 else 2})
  assertEquals(listOf(a),notebookEntries(listOf(a,b),NotebookFilter(difficult=true),setOf(1,8)){if(it<=7) 1 else 2})
  assertTrue(noteTextExport(listOf(a)){"Al Fatiha"}.contains("Texte"));assertTrue(knownIds(saveReaderInk(defaultState(),a.copy(masteredAt="now"))).isEmpty())
 }
}
