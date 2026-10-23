package com.example.attentionmonitor

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Store(context: Context) : SQLiteOpenHelper(context, "attention.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, kind TEXT NOT NULL, app TEXT NOT NULL, detail TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE INDEX idx_events_ts ON events(ts)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    @Synchronized fun add(kind: String, app: String, detail: String = "", ts: Long = System.currentTimeMillis()) {
        writableDatabase.execSQL("INSERT INTO events(ts,kind,app,detail) VALUES(?,?,?,?)", arrayOf(ts,kind,app,detail))
    }
    data class Item(val ts: Long, val kind: String, val app: String, val detail: String)
    fun read(from: Long = 0): List<Item> {
        val result = mutableListOf<Item>()
        readableDatabase.rawQuery("SELECT ts,kind,app,detail FROM events WHERE ts>=? ORDER BY ts,id", arrayOf(from.toString())).use { c ->
            while(c.moveToNext()) result.add(Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)))
        }
        return result
    }
    fun clear() { writableDatabase.delete("events", null, null) }
    companion object {
        fun todayStart(): Long { val c=java.util.Calendar.getInstance(); c.set(java.util.Calendar.HOUR_OF_DAY,0);c.set(java.util.Calendar.MINUTE,0);c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0);return c.timeInMillis }
        fun format(t:Long):String=SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(t))
    }
}
