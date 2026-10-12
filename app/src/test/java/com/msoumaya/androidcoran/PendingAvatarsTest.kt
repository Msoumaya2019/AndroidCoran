package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
class PendingAvatarsTest {
 @get:Rule val folder=TemporaryFolder()
 private val photo=byteArrayOf(0xff.toByte(),0xd8.toByte(),1,2)
 private val account=AvatarAccount("one","person@example.org")
 @Test fun normalizedAddressPersistsAndIsolatesOtherAddresses() { val dir=folder.newFolder();val store=PendingAvatars(dir);store.save(" Person@Example.ORG ",photo);assertArrayEquals(photo,PendingAvatars(dir).load(account.email));assertNull(store.load("other@example.org")) }
 @Test fun successRemovesOnlyMatchingPhoto() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);store.save("other@example.org",photo);var calls=0;assertTrue(store.sync(account,{account}) { bytes,id->assertEquals("one",id);assertArrayEquals(photo,bytes);calls++ });assertEquals(1,calls);assertNull(store.load(account.email));assertNotNull(store.load("other@example.org")) }
 @Test fun failurePreservesPhotoForRetry() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);try { store.sync(account,{account}) { _,_->error("offline") };fail() } catch(_: IllegalStateException) { };assertArrayEquals(photo,store.load(account.email));assertTrue(store.sync(account,{account}) { _,_-> }) }
 @Test fun mismatchedSessionNeverUploads() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);assertFalse(store.sync(account,{AvatarAccount("two",account.email)}) { _,_->fail() });assertArrayEquals(photo,store.load(account.email)) }
 @Test fun sessionChangeDuringUploadKeepsPendingPhoto() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);var current: AvatarAccount?=account;assertFalse(store.sync(account,{current}) { _,_->current=null });assertArrayEquals(photo,store.load(account.email)) }
 @Test fun replacingPhotoDuringUploadPreservesNewSelection() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);val newer=photo+3.toByte();assertTrue(store.sync(account,{account}) { _,_->store.save(account.email,newer) });assertArrayEquals(newer,store.load(account.email)) }
 @Test fun concurrentRequestsUploadOnce() = runBlocking { val store=PendingAvatars(folder.newFolder());store.save(account.email,photo);var calls=0;val first=async { store.sync(account,{account}) { _,_->calls++;yield() } };val second=async { store.sync(account,{account}) { _,_->calls++ } };first.await();second.await();assertEquals(1,calls) }
 @Test fun emailNeverBecomesAFilePathAndDeletionIsIsolated() { val dir=folder.newFolder();val store=PendingAvatars(dir);val address="../user@example.org";store.save(address,photo);assertTrue(dir.listFiles()!!.single().name.matches(Regex("[a-f0-9]{64}\\.jpg")));store.remove(address);assertNull(store.load(address)) }
}
