package com.greeklexicon.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LexiconEntry::class, LexiconEntryFts::class, LexiconAlias::class],
    version = 1,
    exportSchema = false,
)
abstract class LexiconDatabase : RoomDatabase() {

    abstract fun lexiconDao(): LexiconDao

    companion object {
        private const val DB_NAME = "lexicon.db"
        private const val EXPECTED_IDENTITY_HASH = "64ac97b5d8bbff71fb5cc6e256d04c07"
        @Volatile
        private var instance: LexiconDatabase? = null

        fun getInstance(context: Context): LexiconDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LexiconDatabase {
            ensureDatabaseIsFresh(context)

            return Room.databaseBuilder(
                context.applicationContext,
                LexiconDatabase::class.java,
                DB_NAME
            )
                .createFromAsset(DB_NAME)
                .build()
        }

        private fun ensureDatabaseIsFresh(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return
            }

            val shouldDelete = dbFile.length() < 1_000_000 || !hasExpectedSchema(dbFile.path)
            if (shouldDelete) {
                dbFile.delete()
                context.getDatabasePath("$DB_NAME-journal").delete()
                context.getDatabasePath("$DB_NAME-shm").delete()
                context.getDatabasePath("$DB_NAME-wal").delete()
            }
        }

        private fun hasExpectedSchema(dbPath: String): Boolean {
            return runCatching {
                SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    hasExpectedIdentityHash(db) &&
                        hasExpectedEntriesTable(db) &&
                        hasExpectedFtsTable(db) &&
                        hasExpectedEntryAliasesTable(db) &&
                        hasExpectedIndexes(db)
                }
            }.getOrDefault(false)
        }

        private fun hasExpectedIdentityHash(db: SQLiteDatabase): Boolean {
            return db.rawQuery(
                "SELECT identity_hash FROM room_master_table WHERE id = 42",
                emptyArray(),
            ).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == EXPECTED_IDENTITY_HASH
            }
        }

        private fun hasExpectedEntriesTable(db: SQLiteDatabase): Boolean {
            return db.rawQuery("PRAGMA table_info(entries)", emptyArray()).use { cursor ->
                var hasValidId = false
                var hasValidEntryType = false

                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
                val defaultValueIndex = cursor.getColumnIndexOrThrow("dflt_value")
                val pkIndex = cursor.getColumnIndexOrThrow("pk")

                while (cursor.moveToNext()) {
                    when (cursor.getString(nameIndex)) {
                        "_id" -> {
                            hasValidId =
                                cursor.getInt(notNullIndex) == 1 && cursor.getInt(pkIndex) == 1
                        }

                        "entry_type" -> {
                            hasValidEntryType = cursor.isNull(defaultValueIndex)
                        }
                    }
                }

                hasValidId && hasValidEntryType
            }
        }

        private fun hasExpectedIndexes(db: SQLiteDatabase): Boolean {
            val names = buildSet {
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name IN ('entries', 'entry_aliases')",
                    emptyArray(),
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }
            return "idx_entries_key" in names &&
                "idx_entries_latin_key" in names &&
                "idx_entry_aliases_entry_id" in names &&
                "idx_entry_aliases_alias_key" in names &&
                "idx_entry_aliases_latin_key" in names
        }

        private fun hasExpectedFtsTable(db: SQLiteDatabase): Boolean {
            return db.rawQuery(
                "SELECT sql FROM sqlite_master WHERE name = 'entries_fts'",
                emptyArray(),
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use false
                }

                val sql = cursor.getString(0)
                sql.contains("CREATE VIRTUAL TABLE") &&
                    sql.contains("entries_fts") &&
                    sql.contains("headword TEXT NOT NULL") &&
                    sql.contains("key TEXT NOT NULL") &&
                    sql.contains("short_def TEXT") &&
                    sql.contains("tokenize=unicode61") &&
                    sql.contains("content=`entries`")
            }
        }

        private fun hasExpectedEntryAliasesTable(db: SQLiteDatabase): Boolean {
            return db.rawQuery("PRAGMA table_info(entry_aliases)", emptyArray()).use { cursor ->
                val names = buildSet {
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        add(cursor.getString(nameIndex))
                    }
                }
                "_id" in names &&
                    "entry_id" in names &&
                    "alias_key" in names &&
                    "latin_key" in names
            }
        }
    }
}
