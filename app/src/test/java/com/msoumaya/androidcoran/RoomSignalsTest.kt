package com.msoumaya.androidcoran

import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class RoomSignalsTest {
    private class Transport: RoomSignalTransport {
        override val events=MutableSharedFlow<RoomSignal>()
        val sent=mutableListOf<RoomSignal>()
        var disconnected=false
        var fail=false
        override suspend fun connect() {}
        override suspend fun send(signal: RoomSignal) { if(fail) error("Offline");sent+=signal }
        override suspend fun disconnect() { disconnected=true }
    }
    @Test fun privateRoomIgnoresOwnAndUnexpectedSenders()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this)
        try { room.start();transport.events.emit(RoomSignal("typing","me",true));transport.events.emit(RoomSignal("typing","stranger",true));yield();assertFalse(room.state.value.typing)
            transport.events.emit(RoomSignal("typing","friend",true));yield();assertTrue(room.state.value.typing)
        } finally { room.close() }
    }
    @Test fun typingExpiresAndExplicitStopClearsIt()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this,expiryMs=20)
        try { room.start();transport.events.emit(RoomSignal("typing","friend",true));yield();assertTrue(room.state.value.typing);delay(60);assertFalse(room.state.value.typing)
            transport.events.emit(RoomSignal("typing","friend",true));yield();transport.events.emit(RoomSignal("typing","friend",false));yield();assertFalse(room.state.value.typing)
        } finally { room.close() }
    }
    @Test fun debounceSendsOnlyLatestActivityAndNoDraftText()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this,debounceMs=20)
        try { room.start();room.changed("private text");room.changed("another private text");delay(60)
            assertEquals(listOf(RoomSignal("typing","me",true)),transport.sent)
            room.changed("   ");delay(60);assertFalse(transport.sent.last().active)
        } finally { room.close() }
    }
    @Test fun stopAfterSendCancelsPendingActiveBroadcast()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this,debounceMs=20)
        try { room.start();room.changed("draft");room.stopTyping();delay(60);assertEquals(listOf(RoomSignal("typing","me",false)),transport.sent) }
        finally { room.close() }
    }
    @Test fun readReceiptsAreValidatedAndNeverMoveBackwards()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this)
        try { room.start();for(date in listOf("2026-10-11T10:00:00Z","invalid","2026-10-10T10:00:00Z")) transport.events.emit(RoomSignal("read","friend",at=date));yield()
            assertEquals("2026-10-11T10:00:00Z",room.state.value.readAt)
            room.markRead("2026-10-11T11:00:00Z");assertEquals(RoomSignal("read","me",at="2026-10-11T11:00:00Z"),transport.sent.last())
        } finally { room.close() }
    }
    @Test fun groupAcceptsMemberTypingWithoutInventingReadReceipts()=runBlocking {
        val transport=Transport();val room=RoomSignals("me",null,transport,this)
        try { room.start();transport.events.emit(RoomSignal("typing","member",true));transport.events.emit(RoomSignal("read","member",at="2026-10-11T10:00:00Z"));yield();assertTrue(room.state.value.typing);assertNull(room.state.value.readAt)
            room.markRead("2026-10-11T10:00:00Z");assertTrue(transport.sent.isEmpty())
        } finally { room.close() }
    }
    @Test fun closeCancelsTimersResetsIndicatorsAndDisconnects()=runBlocking {
        val transport=Transport();val room=RoomSignals("me","friend",transport,this,debounceMs=20)
        room.start();transport.events.emit(RoomSignal("typing","friend",true));yield();room.changed("draft");room.close();room.changed("late draft");delay(60)
        assertTrue(transport.disconnected);assertEquals(RoomSignalState(),room.state.value);assertEquals(listOf(RoomSignal("typing","me",false)),transport.sent)
    }
    @Test fun offlineSignalsDoNotCrashMessageComposer()=runBlocking {
        val transport=Transport();transport.fail=true;val room=RoomSignals("me","friend",transport,this,debounceMs=20)
        try { room.start();room.changed("draft");delay(60);room.stopTyping();room.markRead("2026-10-11T10:00:00Z") } finally { room.close() }
        assertTrue(transport.disconnected)
    }
}
