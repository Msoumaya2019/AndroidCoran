package com.msoumaya.androidcoran.data
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import androidx.lifecycle.*
import com.msoumaya.androidcoran.domain.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

internal class UsageStore(context: Context,name: String="android-coran-usage.db"): SQLiteOpenHelper(context,name,null,1) {
 override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE usage_pending(id TEXT PRIMARY KEY,owner TEXT NOT NULL,started TEXT NOT NULL,seconds INTEGER NOT NULL,ended INTEGER NOT NULL)") }
 override fun onUpgrade(db: SQLiteDatabase,old: Int,new: Int) {}
 fun save(id: String,owner: String,started: String,seconds: Long,ended: Boolean) { writableDatabase.execSQL("INSERT INTO usage_pending VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET seconds=excluded.seconds,ended=excluded.ended",arrayOf<Any>(id,owner,started,seconds,if(ended) 1 else 0)) }
 fun pending(owner: String)=readableDatabase.rawQuery("SELECT id,started,seconds,ended FROM usage_pending WHERE owner=? ORDER BY started LIMIT 50",arrayOf(owner)).use { c -> buildList { while(c.moveToNext()) add(json("p_id" to c.getString(0),"p_started_at" to c.getString(1),"p_seconds" to c.getLong(2),"p_ended" to (c.getInt(3)==1))) } }
 fun acknowledge(owner: String,row: kotlinx.serialization.json.JsonObject) { writableDatabase.execSQL("DELETE FROM usage_pending WHERE owner=? AND id=? AND seconds=? AND ended=?",arrayOf<Any>(owner,row.str("p_id"),row.num("p_seconds"),if(row.flag("p_ended")) 1 else 0)) }
}
internal class ForegroundUsage(context: Context,private val repo: Repository,private val scope: CoroutineScope) {
 private val store=UsageStore(context);private val foreground=MutableStateFlow(false);private val lock=Mutex()
 private suspend fun flush()=lock.withLock {
  val owner=repo.user.value?:return@withLock
  try {
   if(repo.db().auth.currentUserOrNull()?.id!=owner) return@withLock
   for(row in store.pending(owner)) {
    if(repo.user.value!=owner) return@withLock
    repo.rpc("record_app_usage",row)
    if(repo.user.value!=owner) return@withLock
    store.acknowledge(owner,row)
   }
  } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Durable queue retains the same account's measurements. */ }
 }
 fun start() {
  scope.launch(Dispatchers.Main.immediate) {
   val lifecycle=ProcessLifecycleOwner.get().lifecycle
   foreground.value=lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
   lifecycle.addObserver(object: DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) { foreground.value=true }
    override fun onStop(owner: LifecycleOwner) { foreground.value=false }
   })
  }
  scope.launch {
   combine(repo.user,foreground) { owner,active -> owner to active }.collectLatest { (owner,active) ->
    if(owner==null||!active) { flush();return@collectLatest }
    val id=UUID.randomUUID().toString();val started=Instant.now().toString();val clock=SystemClock.elapsedRealtime()
    fun record(ended: Boolean) { store.save(id,owner,started,elapsedUsageSeconds(clock,SystemClock.elapsedRealtime()),ended) }
    try { while(currentCoroutineContext().isActive) { record(false);flush();delay(15000) } }
    finally { withContext(NonCancellable) { record(true) };scope.launch { flush() } }
   }
  }
 }
}
