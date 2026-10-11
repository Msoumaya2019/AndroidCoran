package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Test

class ContentBrowserTest {
 private val reminders=ContentSelection("owner","reminder",false,false,null)
 private fun rows(start: Int,count: Int)=List(count) { json("id" to (start+it).toString()) }
 @Test fun delayedCancelledRequestCannotOverwriteNewFilter()=runBlocking {
  val browser=ContentBrowser();val old=CompletableDeferred<List<JsonObject>>()
  browser.load(this,reminders) { _,_ -> withContext(NonCancellable) { old.await() } };yield()
  browser.load(this,reminders.copy(type="invocation")) { _,_ -> rows(100,1) };yield()
  old.complete(rows(1,1));yield()
  assertEquals("100",browser.state.value.rows.single().str("id"));assertFalse(browser.state.value.loading)
 }
 @Test fun cancelledRequestCannotClearLoadingOfItsReplacement()=runBlocking {
  val browser=ContentBrowser();val old=CompletableDeferred<List<JsonObject>>();val current=CompletableDeferred<List<JsonObject>>()
  browser.load(this,reminders) { _,_ -> withContext(NonCancellable) { old.await() } };yield()
  browser.load(this,reminders.copy(owner="other")) { _,_ -> current.await() };yield()
  old.complete(rows(1,1));yield();assertTrue(browser.state.value.loading);assertTrue(browser.state.value.rows.isEmpty())
  current.complete(rows(2,1));yield();assertEquals("2",browser.state.value.rows.single().str("id"))
 }
 @Test fun paginationUsesRawOffsetEvenIfRowsOverlapAndBlocksDoubleLoad()=runBlocking {
  val browser=ContentBrowser();browser.load(this,reminders) { _,offset -> assertEquals(0,offset);rows(0,30) };yield()
  val next=CompletableDeferred<List<JsonObject>>();var requests=0
  browser.load(this,reminders,true) { _,offset -> requests++;assertEquals(30,offset);next.await() };yield()
  browser.load(this,reminders,true) { _,_ -> requests++;emptyList() }
  next.complete(rows(29,2));yield()
  assertEquals(1,requests);assertEquals(31,browser.state.value.rows.size);assertEquals(32,browser.state.value.offset);assertFalse(browser.state.value.more)
 }
 @Test fun failedAppendKeepsRowsAndCanRetryAtSameOffset()=runBlocking {
  val browser=ContentBrowser();browser.load(this,reminders) { _,_ -> rows(0,30) };yield()
  browser.load(this,reminders,true) { _,_ -> error("Hors connexion") };yield()
  assertEquals(30,browser.state.value.rows.size);assertEquals("Hors connexion",browser.state.value.error);assertFalse(browser.state.value.loading)
  browser.load(this,reminders,true) { _,offset -> assertEquals(30,offset);rows(30,1) };yield();assertEquals(31,browser.state.value.rows.size)
 }
 @Test fun closedBrowserCannotReceiveLateAccountData()=runBlocking {
  val browser=ContentBrowser();val result=CompletableDeferred<List<JsonObject>>()
  browser.load(this,reminders) { _,_ -> withContext(NonCancellable) { result.await() } };yield();browser.close();result.complete(rows(1,1));yield()
  assertTrue(browser.state.value.rows.isEmpty())
 }
 @Test fun dailyPreviewOrdersReminderBeforeInvocationAndDeduplicatesTypes() {
  val input=listOf(json("id" to "i","type" to "invocation"),json("id" to "r","type" to "reminder"),json("id" to "other","type" to "reminder"))
  assertEquals(listOf("r","i"),dailyContentEntries(input).map { it.str("id") })
 }
 @Test fun dailyPreviewSkipsMissingIdsAndUnrelatedTypes() {
  assertTrue(dailyContentEntries(listOf(json("type" to "invocation"),json("id" to "x","type" to "unknown"))).isEmpty())
 }
}
