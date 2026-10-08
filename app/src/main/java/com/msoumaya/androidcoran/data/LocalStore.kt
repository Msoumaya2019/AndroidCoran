package com.msoumaya.androidcoran.data

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

data class StoredState(val data: JsonObject,val base: JsonObject?,val remoteVersion: String?,val pending: Boolean)
data class QueuedOperation(val id: String,val owner: String,val kind: String,val payload: JsonObject,val attachment: String?,val mime: String?)
class LocalStore(context: Context,name: String="android-coran.db"): SQLiteOpenHelper(context,name,null,2) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE states(account TEXT PRIMARY KEY,data TEXT NOT NULL,base TEXT,remote_version TEXT,pending INTEGER NOT NULL DEFAULT 0)");db.execSQL("CREATE TABLE cache(key TEXT PRIMARY KEY,data TEXT NOT NULL)");createOutbox(db) }
    private fun createOutbox(db: SQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS outbox(owner TEXT NOT NULL,id TEXT NOT NULL,kind TEXT NOT NULL,payload TEXT NOT NULL,attachment TEXT,mime TEXT,created_at TEXT NOT NULL,PRIMARY KEY(owner,id))") }
    override fun onUpgrade(db: SQLiteDatabase,oldVersion: Int,newVersion: Int) { if(oldVersion<2) createOutbox(db) }
    fun load(account: String): StoredState? = readableDatabase.rawQuery("SELECT data,base,remote_version,pending FROM states WHERE account=?",arrayOf(account)).use { c -> if(!c.moveToFirst()) null else StoredState(Json.parseToJsonElement(c.getString(0)).jsonObject,if(c.isNull(1)) null else Json.parseToJsonElement(c.getString(1)).jsonObject,if(c.isNull(2)) null else c.getString(2),c.getInt(3)==1) }
    fun save(account: String,s: StoredState) { writableDatabase.execSQL("INSERT INTO states(account,data,base,remote_version,pending) VALUES(?,?,?,?,?) ON CONFLICT(account) DO UPDATE SET data=excluded.data,base=excluded.base,remote_version=excluded.remote_version,pending=excluded.pending",arrayOf<Any?>(account,s.data.toString(),s.base?.toString(),s.remoteVersion,if(s.pending) 1 else 0)) }
    fun cached(key: String): JsonElement? = readableDatabase.rawQuery("SELECT data FROM cache WHERE key=?",arrayOf(key)).use { if(it.moveToFirst()) Json.parseToJsonElement(it.getString(0)) else null }
    fun cache(key: String,data: JsonElement) { writableDatabase.execSQL("INSERT INTO cache(key,data) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET data=excluded.data",arrayOf(key,data.toString())) }
    fun enqueue(operation: QueuedOperation) { writableDatabase.execSQL("INSERT OR IGNORE INTO outbox(owner,id,kind,payload,attachment,mime,created_at) VALUES(?,?,?,?,?,?,?)",arrayOf(operation.owner,operation.id,operation.kind,operation.payload.toString(),operation.attachment,operation.mime,java.time.Instant.now().toString())) }
    fun pending(owner: String,kind: String): List<QueuedOperation> = readableDatabase.rawQuery("SELECT id,payload,attachment,mime FROM outbox WHERE owner=? AND kind=? ORDER BY rowid",arrayOf(owner,kind)).use { cursor -> buildList { while(cursor.moveToNext()) add(QueuedOperation(cursor.getString(0),owner,kind,Json.parseToJsonElement(cursor.getString(1)).jsonObject,if(cursor.isNull(2)) null else cursor.getString(2),if(cursor.isNull(3)) null else cursor.getString(3))) } }
    fun acknowledge(owner: String,id: String) { writableDatabase.execSQL("DELETE FROM outbox WHERE owner=? AND id=?",arrayOf(owner,id)) }
    fun transaction(block: ()->Unit) { val db=writableDatabase;db.beginTransaction();try { block();db.setTransactionSuccessful() } finally { db.endTransaction() } }
}
