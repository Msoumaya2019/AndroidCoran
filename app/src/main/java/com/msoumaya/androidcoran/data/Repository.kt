package com.msoumaya.androidcoran.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.*
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

private val Context.config by preferencesDataStore("configuration")
const val SUPABASE_URL="https://npbwnvrqmajwqtnncuyv.supabase.co"
class Repository(private val context: Context) {
    val quran=Quran(context);val program=Program(quran);val review=Review(quran,program)
    private val local=LocalStore(context);private val lock=Mutex();private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var client: SupabaseClient?=null
    private val _state=MutableStateFlow(defaultState());val state=_state.asStateFlow()
    private val _notice=MutableStateFlow("");val notice=_notice.asStateFlow()
    private val _user=MutableStateFlow<String?>(null);val user=_user.asStateFlow()
    private var account="guest"
    init { scope.launch { _state.value=local.load(account)?.data?:defaultState();val key=context.config.data.first()[stringPreferencesKey("publicKey")];if(!key.isNullOrBlank()) configure(key) } }
    suspend fun configure(key: String) {
        require(key.isNotBlank()&&!key.contains("service_role")) { "Une clé publique est nécessaire" }
        if(key.startsWith("eyJ")) { val payload=String(android.util.Base64.decode(key.split('.')[1],android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP));require(Json.parseToJsonElement(payload).jsonObject.str("role")=="anon") { "Clé anon uniquement" } }
        require(key.startsWith("sb_publishable_")||key.startsWith("eyJ")) { "Format de clé publique non reconnu" }
        client?.close();client=createSupabaseClient(SUPABASE_URL,key) { install(Auth);install(Postgrest);install(Storage);install(Realtime) }
        context.config.edit { it[stringPreferencesKey("publicKey")]=key }
        client!!.auth.awaitInitialization();client!!.auth.currentUserOrNull()?.let { activate(it.id) }
    }
    internal fun db() = client ?: error("Renseigne la clé publique du projet Supabase dans les réglages")
    suspend fun login(email: String,password: String,register: Boolean=false) {
        if(register) db().auth.signUpWith(Email) { this.email=email.trim();this.password=password } else db().auth.signInWith(Email) { this.email=email.trim();this.password=password }
        val user=db().auth.currentUserOrNull();if(user!=null) activate(user.id) else _notice.value="Consulte ton e-mail pour confirmer l’inscription"
    }
    private suspend fun activate(id: String) = lock.withLock {
        account=id;_user.value=id;_state.value=local.load(id)?.data?:defaultState().with("userId" to JsonPrimitive(id))
        try { syncLocked() } catch(e: Exception) { _notice.value="Compte connecté ; synchronisation en attente : ${e.message}" }
    }
    suspend fun logout() = lock.withLock { client?.auth?.signOut();LocalReminders.schedule(context,false,account);account="guest";_user.value=null;_state.value=local.load("guest")?.data?:defaultState();_notice.value="Déconnecté" }
    suspend fun resetPassword(email: String) { db().auth.resetPasswordForEmail(email.trim());_notice.value="Lien de réinitialisation envoyé" }
    suspend fun changePassword(password: String) { db().auth.updateUser { this.password=password };_notice.value="Mot de passe mis à jour" }
    suspend fun mutate(transform: (JsonObject)->JsonObject) = lock.withLock {
        val next=transform(_state.value);if(next==_state.value) return@withLock
        val old=local.load(account);val stored=StoredState(next,old?.base,old?.remoteVersion,account!="guest")
        local.save(account,stored);_state.value=next
        if(account!="guest") scope.launch { runCatching { sync() }.onFailure { _notice.value="Sauvegardé sur cet appareil. ${it.message}" } }
    }
    suspend fun sync() = lock.withLock { syncLocked() }
    private suspend fun syncLocked() {
        val id=db().auth.currentUserOrNull()?.id ?: return
        check(id==account) { "Le compte a changé" }
        val row=db().from("user_state").select { filter { eq("user_id",id) } }.decodeList<JsonObject>().singleOrNull()
        val remote=row?.obj("data");val version=row?.str("updated_at");val stored=local.load(id)
        if(stored?.pending==true) {
            check(stored.data.str("userId")==id) { "Ces données appartiennent à un autre compte" }
            val next=if(remote==stored.base) stored.data else {
                check(remote!=null&&stored.base!=null) { "La base distante a changé : sauvegarde locale conservée" }
                require(remote.num("schema")==1&&remote["sessions"] is JsonArray&&remote["revisions"] is JsonArray) { "Schéma utilisateur non reconnu" }
                check(listOf(remote,stored.base).all { it.str("userId").isBlank()||it.str("userId")==id }) { "Identité distante incompatible" }
                mergeOfflineState(stored.base.with("userId" to JsonPrimitive(id)),stored.data,remote.with("userId" to JsonPrimitive(id)))
            }
            val payload=json("user_id" to id,"data" to next,"updated_at" to next.str("updatedAt"))
            if(row==null) db().from("user_state").insert(payload) else {
                val changed=db().from("user_state").update(payload) { filter { eq("user_id",id);eq("updated_at",version!!) };select(Columns.list("user_id")) }.decodeList<JsonObject>()
                check(changed.size==1) { "Conflit concurrent : sauvegarde locale conservée" }
            }
            local.save(id,StoredState(next,next,next.str("updatedAt"),false));_state.value=next
        } else if(remote!=null) {
            require(remote.num("schema")==1&&remote["sessions"] is JsonArray&&remote["revisions"] is JsonArray) { "Schéma utilisateur non reconnu" }
            val restored=remote.with("userId" to JsonPrimitive(id));local.save(id,StoredState(restored,remote,version,false));_state.value=restored
        } else { val fresh=stored?.data?:defaultState().with("userId" to JsonPrimitive(id));local.save(id,StoredState(fresh,null,null,false));_state.value=fresh }
        _notice.value="Synchronisation terminée"
    }
    suspend fun rpc(name: String,args: JsonObject=json(),authenticated: Boolean=true): JsonElement { if(authenticated) check(user.value!=null);val owner=user.value;val result=Json.parseToJsonElement(db().postgrest.rpc(name,args).data);check(owner==user.value) { "Le compte a changé" };return result }
    suspend fun rows(table: String,filters: Map<String,String> = emptyMap()): List<JsonObject> { check(user.value!=null);return db().from(table).select { filter { filters.forEach { (k,v) -> eq(k,v) } };limit(100) }.decodeList<JsonObject>() }
    suspend fun insert(table: String,data: JsonObject) { check(user.value!=null);db().from(table).insert(data) }
    suspend fun signedRecitation(path: String) = db().storage.from("recitations").createSignedUrl(path,kotlin.time.Duration.parse("10m"))
    fun recordings(): List<JsonObject> = (local.cached("$account:recordings") as? JsonArray)?.map { it.jsonObject }?:emptyList()
    suspend fun saveRecording(file: java.io.File,range: VerseRange,duration: Long,owner: String,invocation: JsonObject?=null) = lock.withLock {
        require(duration>0&&file.isFile);val existing=(local.cached("$owner:recordings") as? JsonArray)?.map { it.jsonObject }?:emptyList()
        val row=json("id" to file.nameWithoutExtension,"user_id" to owner,"start_verse_id" to range.start,"end_verse_id" to range.end,"duration_ms" to duration,"local_path" to file.path,"created_at" to java.time.Instant.now().toString(),"synced" to false,"recording_type" to if(invocation==null) "quran" else "invocation","invocation_id" to invocation?.str("id"),"invocation_snapshot" to invocation)
        local.cache("$owner:recordings",element(existing+row))
    }
    suspend fun uploadRecordings() = lock.withLock {
        val id=db().auth.currentUserOrNull()?.id?:error("Connexion nécessaire");check(id==account)
        val recordings=recordings().toMutableList()
        for(i in recordings.indices) { val row=recordings[i];if(row.flag("synced")) continue;check(row.str("user_id")==id);val path="$id/${row.str("id")}.m4a";val file=java.io.File(row.str("local_path"));check(file.isFile);check(file.length()<=100_000_000) { "Récitation trop volumineuse" }
            val remote=db().from("recitations").select { filter { eq("id",row.str("id"));eq("user_id",id) } }.decodeList<JsonObject>().singleOrNull()
            if(remote==null) {
                val alreadyUploaded=runCatching { signedRecitation(path) }.isSuccess
                if(!alreadyUploaded) db().storage.from("recitations").upload(path,file.readBytes())
                db().from("recitations").insert(recordingPayload(row,id,path))
            }
            recordings[i]=row.with("synced" to JsonPrimitive(true));local.cache("$id:recordings",element(recordings))
        }
        _notice.value="Récitations synchronisées"
    }
    internal fun cached(key: String)=local.cached(key)
    internal fun cache(key: String,value: JsonElement)=local.cache(key,value)
    suspend fun cachedRpc(name: String,args: JsonObject=json(),authenticated: Boolean=true): JsonElement { val owner=user.value;val key="${owner?:"guest"}:$name:$args";return try { rpc(name,args,authenticated).also { check(owner==user.value);local.cache(key,it) } } catch(e: Exception) { if(e is CancellationException||owner!=user.value) throw e;local.cached(key) ?: throw e } }
    fun feedback(message: String) { _notice.value=message }
}
