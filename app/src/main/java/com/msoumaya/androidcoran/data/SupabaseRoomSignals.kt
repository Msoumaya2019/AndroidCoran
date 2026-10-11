package com.msoumaya.androidcoran.data

import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/** Uses the existing private Expo room and its existing realtime RLS. */
class SupabaseRoomSignals(private val repo: Repository,room: ChatRoom,private val owner: String): RoomSignalTransport {
    private val sdk=repo.db()
    private val channel=sdk.channel("friend-room-${room.id}") { isPrivate=true }
    override val events: Flow<RoomSignal> = merge(
        channel.broadcastFlow<JsonObject>("typing").map { RoomSignal("typing",it.str("userId"),it.flag("active")) },
        channel.broadcastFlow<JsonObject>("read").map { RoomSignal("read",it.str("userId"),at=it.str("at")) }
    ).filter { repo.user.value==owner }
    override suspend fun connect() { check(repo.user.value==owner);channel.subscribe(blockUntilSubscribed=true) }
    override suspend fun send(signal: RoomSignal) {
        if(repo.user.value!=owner||repo.db()!==sdk) return
        val payload=if(signal.event=="typing") json("userId" to owner,"active" to signal.active) else json("userId" to owner,"at" to signal.at)
        channel.broadcast(signal.event,payload)
    }
    override suspend fun disconnect() { channel.unsubscribe() }
}
