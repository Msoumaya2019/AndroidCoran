package com.msoumaya.androidcoran.data

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

data class StoredState(val data: JsonObject,val base: JsonObject?,val remoteVersion: String?,val pending: Boolean)
class LocalStore(context: Context): SQLiteOpenHelper(context,"android-coran.db",null,1) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE states(account TEXT PRIMARY KEY,data TEXT NOT NULL,base TEXT,remote_version TEXT,pending INTEGER NOT NULL DEFAULT 0)");db.execSQL("CREATE TABLE cache(key TEXT PRIMARY KEY,data TEXT NOT NULL)") }
    override fun onUpgrade(db: SQLiteDatabase,oldVersion: Int,newVersion: Int) = error("A reviewed additive migration is required")
    fun load(account: String): StoredState? = readableDatabase.rawQuery("SELECT data,base,remote_version,pending FROM states WHERE account=?",arrayOf(account)).use { c -> if(!c.moveToFirst()) null else StoredState(Json.parseToJsonElement(c.getString(0)).jsonObject,if(c.isNull(1)) null else Json.parseToJsonElement(c.getString(1)).jsonObject,if(c.isNull(2)) null else c.getString(2),c.getInt(3)==1) }
    fun save(account: String,s: StoredState) { writableDatabase.execSQL("INSERT INTO states(account,data,base,remote_version,pending) VALUES(?,?,?,?,?) ON CONFLICT(account) DO UPDATE SET data=excluded.data,base=excluded.base,remote_version=excluded.remote_version,pending=excluded.pending",arrayOf<Any?>(account,s.data.toString(),s.base?.toString(),s.remoteVersion,if(s.pending) 1 else 0)) }
    fun cached(key: String): JsonElement? = readableDatabase.rawQuery("SELECT data FROM cache WHERE key=?",arrayOf(key)).use { if(it.moveToFirst()) Json.parseToJsonElement(it.getString(0)) else null }
    fun cache(key: String,data: JsonElement) { writableDatabase.execSQL("INSERT INTO cache(key,data) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET data=excluded.data",arrayOf(key,data.toString())) }
}
