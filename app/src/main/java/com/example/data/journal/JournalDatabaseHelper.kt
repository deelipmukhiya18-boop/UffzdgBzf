package com.example.data.journal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class JournalEntry(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val content: String,
    val mood: String = ""
)

class JournalDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "mamta_journal.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "journal_entries"
        private const val COL_ID = "id"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_MOOD = "mood"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSql = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_TITLE TEXT,
                $COL_CONTENT TEXT NOT NULL,
                $COL_MOOD TEXT
            )
        """.trimIndent()
        db.execSQL(createSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addEntry(title: String, content: String, mood: String): JournalEntry {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, now)
            put(COL_TITLE, title.trim())
            put(COL_CONTENT, content.trim())
            put(COL_MOOD, mood.trim())
        }
        val id = db.insert(TABLE_NAME, null, values)
        return JournalEntry(id = id, timestamp = now, title = title.trim(), content = content.trim(), mood = mood.trim())
    }

    fun getAllEntries(): List<JournalEntry> {
        val list = mutableListOf<JournalEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_ID)
            val timeIdx = c.getColumnIndexOrThrow(COL_TIMESTAMP)
            val titleIdx = c.getColumnIndexOrThrow(COL_TITLE)
            val contentIdx = c.getColumnIndexOrThrow(COL_CONTENT)
            val moodIdx = c.getColumnIndexOrThrow(COL_MOOD)

            while (c.moveToNext()) {
                list.add(
                    JournalEntry(
                        id = c.getLong(idIdx),
                        timestamp = c.getLong(timeIdx),
                        title = c.getString(titleIdx) ?: "",
                        content = c.getString(contentIdx) ?: "",
                        mood = c.getString(moodIdx) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun getWeekEntries(): List<JournalEntry> {
        val oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val list = mutableListOf<JournalEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COL_TIMESTAMP >= ?",
            arrayOf(oneWeekAgo.toString()),
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_ID)
            val timeIdx = c.getColumnIndexOrThrow(COL_TIMESTAMP)
            val titleIdx = c.getColumnIndexOrThrow(COL_TITLE)
            val contentIdx = c.getColumnIndexOrThrow(COL_CONTENT)
            val moodIdx = c.getColumnIndexOrThrow(COL_MOOD)

            while (c.moveToNext()) {
                list.add(
                    JournalEntry(
                        id = c.getLong(idIdx),
                        timestamp = c.getLong(timeIdx),
                        title = c.getString(titleIdx) ?: "",
                        content = c.getString(contentIdx) ?: "",
                        mood = c.getString(moodIdx) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun deleteEntry(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }
}
