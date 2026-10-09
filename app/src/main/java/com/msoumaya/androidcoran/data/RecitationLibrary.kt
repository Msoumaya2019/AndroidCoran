package com.msoumaya.androidcoran.data
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

data class RecitationFriend(val link: JsonObject,val profile: JsonObject) { val id get()=link.str("id");val name get()=profile.str("display_name","Ami") }
fun acceptedRecitationFriends(owner: String,links: List<JsonObject>,profiles: List<JsonObject>): List<RecitationFriend> {
 require(owner.isNotBlank());val people=profiles.associateBy { it.str("id") }
 return links.filter { it.str("id").isNotBlank()&&it.str("status")=="accepted"&&owner in listOf(it.str("requester_id"),it.str("recipient_id")) }.map { link -> val other=if(link.str("requester_id")==owner) link.str("recipient_id") else link.str("requester_id");RecitationFriend(link,people[other]?:json("id" to other,"display_name" to "Ami")) }
}
fun recitationLibrary(owner: String?,local: List<JsonObject>,remote: List<JsonObject>): List<JsonObject> {
 if(owner==null) return emptyList()
 val ownLocal=local.filter { it.str("user_id")==owner&&it.str("id").isNotBlank() }.associateBy { it.str("id") }
 val combined=ownLocal.toMutableMap()
 remote.filter { it.str("user_id")==owner&&it.str("id").isNotBlank() }.forEach { row -> combined[row.str("id")]=row.with("local_path" to (ownLocal[row.str("id")]?.get("local_path")?:JsonNull),"synced" to JsonPrimitive(true)) }
 return combined.values.sortedByDescending { it.str("created_at") }
}
fun recitationSharePayload(owner: String,link: JsonObject,recording: JsonObject,description: String): JsonObject {
 require(owner.isNotBlank()&&recording.str("user_id")==owner) { "Cette récitation appartient à un autre compte." }
 require(link.str("status")=="accepted"&&owner in listOf(link.str("requester_id"),link.str("recipient_id"))) { "Choisis un ami accepté." }
 require(recording.str("recording_type","quran")=="quran"&&recording.str("storage_path").isNotBlank()) { "Synchronise d’abord cette récitation du Coran." }
 return messagePayload(ChatRoom(link.str("id")),owner,description.take(2000),"recitation",recording.str("id"))
}
class RecitationLibraryService(private val repo: Repository) {
 suspend fun friends(): List<RecitationFriend> {
  val owner=repo.user.value?:error("Connexion nécessaire")
  val links=mutableListOf<JsonObject>();var offset=0
  while(true) {
   val page=repo.query("friend_links",eq=mapOf("status" to "accepted"),offset=offset,size=300)
   check(repo.user.value==owner);links+=page;offset+=page.size;if(page.size<300) break
  }
  val ownLinks=links.distinctBy { it.str("id") }.filter { owner in listOf(it.str("requester_id"),it.str("recipient_id")) }
  val ids=ownLinks.flatMap { listOf(it.str("requester_id"),it.str("recipient_id")) }.filter { it.isNotBlank()&&it!=owner }.distinct()
  val profiles=mutableListOf<JsonObject>()
  for(batch in ids.chunked(300)) { profiles+=repo.query("friend_profiles",ids="id" to batch,orderBy="display_name",ascending=true,size=300);check(repo.user.value==owner) }
  check(repo.user.value==owner);return acceptedRecitationFriends(owner,ownLinks,profiles)
 }
 suspend fun share(owner: String,linkId: String,recordingId: String,description: String) {
  check(repo.user.value==owner) { "Le compte a changé." }
  val recording=repo.query("recitations",eq=mapOf("id" to recordingId,"user_id" to owner),size=1).singleOrNull()?:error("Récitation synchronisée introuvable.")
  val link=repo.query("friend_links",eq=mapOf("id" to linkId,"status" to "accepted"),size=1).singleOrNull()?:error("Cet ami n’est plus accepté.")
  val payload=recitationSharePayload(owner,link,recording,description)
  check(repo.user.value==owner) { "Le compte a changé." };repo.insert("friend_messages",payload)
 }
}
