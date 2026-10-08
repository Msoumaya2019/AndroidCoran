package com.msoumaya.androidcoran

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test

class OutboxPersistenceTest {
    @Test fun queueIsIdempotentPersistentAndAcknowledgementsStayInTheirAccount() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="outbox-test.db"
        context.deleteDatabase(name)
        try {
            LocalStore(context,name).use { store ->
                store.enqueue(QueuedOperation("same","a","quiz",json("answer" to "first"),null,null))
                store.enqueue(QueuedOperation("same","a","quiz",json("answer" to "second"),null,null))
                store.enqueue(QueuedOperation("same","b","quiz",json("answer" to "other"),null,null))
                assertEquals(1,store.pending("a","quiz").size)
            }
            LocalStore(context,name).use { store ->
                assertEquals("first",store.pending("a","quiz").single().payload.str("answer"))
                store.acknowledge("a","same");assertTrue(store.pending("a","quiz").isEmpty());assertEquals(1,store.pending("b","quiz").size)
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun additiveMigrationPreservesExistingStateAndCache() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="migration-test.db"
        context.deleteDatabase(name)
        try {
            val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
                db.execSQL("CREATE TABLE states(account TEXT PRIMARY KEY,data TEXT NOT NULL,base TEXT,remote_version TEXT,pending INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE cache(key TEXT PRIMARY KEY,data TEXT NOT NULL)")
                db.execSQL("INSERT INTO states(account,data,pending) VALUES(?,?,1)",arrayOf("a",defaultState().toString()))
                db.execSQL("INSERT INTO cache(key,data) VALUES(?,?)",arrayOf("keep",json("unknown" to true).toString()));db.version=1
            }
            LocalStore(context,name).use { store ->
                assertEquals(defaultState(),store.load("a")!!.data);assertTrue(store.load("a")!!.pending)
                assertEquals(json("unknown" to true),store.cached("keep"))
                store.enqueue(QueuedOperation("new","a","report",json("description" to "keep"),null,null));assertEquals(1,store.pending("a","report").size)
            }
        } finally { context.deleteDatabase(name) }
    }
}
