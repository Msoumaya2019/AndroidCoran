package com.msoumaya.androidcoran.data

import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate

data class ChatRoom(val id: String,val group: Boolean=false,val otherId: String?=null,val title: String="Conversation") {
    init { require(id.isNotBlank()) }
    val column get()=if(group) "group_id" else "link_id"
}
fun messagePayload(room: ChatRoom,user: String,body: String,kind: String="text",recitationId: String?=null): JsonObject {
    require(user.isNotBlank());require(body.trim().length in 1..2000);require(kind in listOf("text","encouragement","progress","recitation"))
    if(kind=="recitation") require(!recitationId.isNullOrBlank())
    return json("link_id" to if(room.group) null else room.id,"group_id" to if(room.group) room.id else null,"sender_id" to user,"body" to body.trim(),"kind" to kind,"recitation_id" to recitationId)
}
data class SocialSnapshot(val profile: JsonObject,val links: List<JsonObject>,val profiles: List<JsonObject>,val groups: List<JsonObject>,val members: List<JsonObject>,val inbox: List<JsonObject>,val suspension: JsonObject?)
data class MessagePage(val messages: List<JsonObject>,val cursor: String?,val hasMore: Boolean)
fun rpcObject(value: JsonElement): JsonObject = when(value) { is JsonObject -> value;is JsonArray -> value.firstOrNull()?.jsonObject?:json();else -> json() }
class SocialService(val repo: Repository) {
    suspend fun snapshot(): SocialSnapshot = coroutineScope {
        val owner=repo.user.value?:error("Connexion nécessaire")
        val profile=async { rpcObject(repo.rpc("ensure_social_profile")) }
        val links=async { repo.query("friend_links") }
        val groups=async { repo.query("friend_groups") }
        val members=async { repo.query("friend_group_members",eq=mapOf("user_id" to owner),orderBy="user_id",size=300) }
        val suspension=async { repo.query("social_suspensions",eq=mapOf("user_id" to owner)).firstOrNull() }
        val inbox=async { runCatching { repo.rpc("friend_inbox").jsonArray.map { it.jsonObject } }.getOrElse { if(it is CancellationException) throw it;emptyList() } }
        val relationships=links.await();val ids=relationships.flatMap { listOf(it.str("requester_id"),it.str("recipient_id")) }.distinct()
        val profiles=repo.query("friend_profiles",ids="id" to ids,orderBy="display_name",ascending=true,size=300)
        SocialSnapshot(profile.await(),relationships,profiles,groups.await(),members.await(),inbox.await(),suspension.await())
    }
    suspend fun messages(room: ChatRoom,before: String?=null): MessagePage {
        val owner=repo.user.value?:error("Connexion nécessaire")
        val rows=repo.query("friend_messages",eq=mapOf(room.column to room.id),before=before?.let { "created_at" to it },size=50,cacheResult=true)
        if(rows.isEmpty()) return MessagePage(emptyList(),null,false)
        val hidden=repo.query("friend_message_hidden",eq=mapOf("user_id" to owner),ids="message_id" to rows.map { it.str("id") },orderBy="message_id",size=50,cacheResult=true).map { it.str("message_id") }.toSet()
        val visible=rows.filter { it.str("id") !in hidden }
        val ids=visible.filter { it.str("kind")=="recitation" }.map { it.str("recitation_id") }.filter { it.isNotBlank() }.distinct()
        val recitations=repo.query("recitations",ids="id" to ids,size=50,cacheResult=true).associateBy { it.str("id") }
        return MessagePage(visible.map { row -> recitations[row.str("recitation_id")]?.let { row.with("recitation" to it) } ?: row }.reversed(),rows.last().str("created_at"),rows.size==50)
    }
    suspend fun send(room: ChatRoom,body: String,kind: String="text",recitation: String?=null) = repo.insert("friend_messages",messagePayload(room,repo.user.value?:error("Connexion nécessaire"),body,kind,recitation))
    suspend fun markRead(room: ChatRoom) { if(!room.group) repo.upsertRows("friend_message_reads",json("link_id" to room.id,"user_id" to repo.user.value,"last_read_at" to Instant.now().toString())) }
    suspend fun hide(id: String)=repo.upsertRows("friend_message_hidden",json("message_id" to id,"user_id" to repo.user.value))
    suspend fun goals(room: ChatRoom)=repo.query("friend_shared_goals",eq=mapOf("link_id" to room.id),orderBy="week_start",size=8)
    suspend fun appointments(room: ChatRoom)=repo.query("friend_review_appointments",eq=mapOf("link_id" to room.id),after="starts_at" to Instant.now().toString(),orderBy="starts_at",ascending=true,size=20)
    suspend fun proposeGoal(room: ChatRoom,date: String,target: Int) {
        require(!room.group&&target>0);LocalDate.parse(date)
        repo.insert("friend_shared_goals",json("link_id" to room.id,"week_start" to date,"target_sessions" to target,"proposed_by" to repo.user.value))
    }
    suspend fun proposeAppointment(room: ChatRoom,date: String) {
        require(!room.group);val instant=Instant.parse(date);require(instant>Instant.now()) { "Choisis une date future" }
        repo.insert("friend_review_appointments",json("link_id" to room.id,"starts_at" to instant.toString(),"proposed_by" to repo.user.value))
    }
    suspend fun members(room: ChatRoom): List<JsonObject> {
        val members=repo.query("friend_group_members",eq=mapOf("group_id" to room.id),orderBy="user_id",size=300)
        val profiles=repo.query("friend_profiles",ids="id" to members.map { it.str("user_id") },orderBy="display_name",size=300).associateBy { it.str("id") }
        return members.map { it.with("profile" to (profiles[it.str("user_id")]?:json())) }
    }
}
