package com.msoumaya.androidcoran.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant

data class RoomSignal(val event: String,val userId: String,val active: Boolean=false,val at: String?=null)
data class RoomSignalState(val typing: Boolean=false,val readAt: String?=null)
interface RoomSignalTransport {
    val events: Flow<RoomSignal>
    suspend fun connect()
    suspend fun send(signal: RoomSignal)
    suspend fun disconnect()
}
/** Ephemeral signals: never persist draft contents and never echo our own broadcasts. */
class RoomSignals(private val owner: String,private val other: String?,private val transport: RoomSignalTransport,
    private val scope: CoroutineScope,private val debounceMs: Long=400,private val expiryMs: Long=3000) {
    private val mutable=MutableStateFlow(RoomSignalState())
    val state: StateFlow<RoomSignalState> = mutable.asStateFlow()
    private var incoming: Job?=null
    private var outgoing: Job?=null
    private var expiry: Job?=null
    private var ready=false
    private var closed=false
    private var draft=""
    suspend fun start() {
        check(!closed)
        incoming=scope.launch(start=CoroutineStart.UNDISPATCHED) { transport.events.collect { receive(it) } }
        transport.connect();ready=true
        if(draft.isNotBlank()) changed(draft)
    }
    private fun receive(signal: RoomSignal) {
        if(closed||signal.userId.isBlank()||signal.userId==owner||(other!=null&&signal.userId!=other)) return
        when(signal.event) {
            "typing" -> {
                expiry?.cancel();mutable.value=mutable.value.copy(typing=signal.active)
                if(signal.active) expiry=scope.launch { delay(expiryMs);mutable.value=mutable.value.copy(typing=false) }
            }
            "read" -> {
                if(other==null) return
                val date=signal.at?.let { runCatching { Instant.parse(it) }.getOrNull() }?:return
                val previous=mutable.value.readAt?.let { Instant.parse(it) }
                if(previous==null||date>previous) mutable.value=mutable.value.copy(readAt=signal.at)
            }
        }
    }
    fun changed(value: String) {
        if(closed) return
        draft=value;outgoing?.cancel()
        if(ready) outgoing=scope.launch { delay(debounceMs);sendSafely(RoomSignal("typing",owner,draft.isNotBlank())) }
    }
    suspend fun stopTyping() { outgoing?.cancel();draft="";if(ready&&!closed) sendSafely(RoomSignal("typing",owner,false)) }
    suspend fun markRead(at: String) { if(ready&&!closed&&other!=null) sendSafely(RoomSignal("read",owner,at=at)) }
    private suspend fun sendSafely(signal: RoomSignal) {
        try { transport.send(signal) } catch(e: Exception) { if(e is CancellationException) throw e }
    }
    suspend fun close() {
        if(closed) return
        closed=true;incoming?.cancel();outgoing?.cancel();expiry?.cancel();mutable.value=RoomSignalState()
        try { if(ready) withTimeoutOrNull(500) { sendSafely(RoomSignal("typing",owner,false)) } }
        finally { ready=false;transport.disconnect() }
    }
}
