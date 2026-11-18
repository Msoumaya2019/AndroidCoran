package com.msoumaya.androidcoran
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class HandwrittenStateTest {
 @Test fun handwritingPersistsAcrossDatabaseReopeningAndStaysAccountScoped() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="ink-test-"+UUID.randomUUID()+".db"
  val stroke=InkStroke(listOf(InkPoint(.2f,.3f,.5f),InkPoint(.3f,.4f,.9f)))
  val ink=newReaderInk(1,stroke,listOf(1)).copy(note=listOf(stroke))
  try {
   LocalStore(context,name).use { store -> store.save("account-a",StoredState(saveReaderInk(defaultState(),ink),null,null,true));store.save("account-b",StoredState(defaultState(),null,null,false)) }
   LocalStore(context,name).use { store ->
    assertEquals(ink,readerInk(store.load("account-a")!!.data,1).single());assertTrue(readerInk(store.load("account-b")!!.data,1).isEmpty())
    val deleted=deleteReaderInk(store.load("account-a")!!.data,ink.id);store.save("account-a",StoredState(deleted,null,null,true))
   }
   LocalStore(context,name).use { assertTrue(readerInk(it.load("account-a")!!.data,1).isEmpty()) }
  } finally { context.deleteDatabase(name) }
 }
 @Test fun usageQueueRetainsNewerMeasurementsAndNeverAcknowledgesAnotherAccount() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="usage-test-"+UUID.randomUUID()+".db"
  try {
   UsageStore(context,name).use { store ->
    store.save("session","a","2026-11-18T10:00:00Z",15,false);val snapshot=store.pending("a").single()
    store.save("session","a","2026-11-18T10:00:00Z",30,true)
    store.acknowledge("a",snapshot);assertEquals(30,store.pending("a").single().num("p_seconds"))
    store.acknowledge("b",store.pending("a").single());assertEquals(1,store.pending("a").size);assertTrue(store.pending("b").isEmpty())
   }
   UsageStore(context,name).use { store -> assertEquals(30,store.pending("a").single().num("p_seconds"));store.acknowledge("a",store.pending("a").single());assertTrue(store.pending("a").isEmpty()) }
  } finally { context.deleteDatabase(name) }
 }
}
