package com.greeklexicon.app.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface LexiconDao {

    @Query("""
        SELECT entries.* FROM entries
        JOIN entries_fts ON entries._id = entries_fts.rowid
        WHERE entries_fts MATCH :query
          AND entries.entry_type != 'smithhall'
        ORDER BY entries.headword
        LIMIT :limit
    """)
    suspend fun searchGreek(query: String, limit: Int = 50): List<LexiconEntry>

    @Query("""
        SELECT entries.* FROM entries
        WHERE entries._id IN (
            SELECT _id FROM entries
            WHERE entry_type != 'smithhall'
              AND key LIKE :prefix || '%'
            UNION
            SELECT entry_aliases.entry_id FROM entry_aliases
            JOIN entries ON entries._id = entry_aliases.entry_id
            WHERE entries.entry_type != 'smithhall'
              AND entry_aliases.alias_key LIKE :prefix || '%'
        )
        ORDER BY entries.key
        LIMIT :limit
    """)
    suspend fun searchGreekByPrefix(prefix: String, limit: Int = 50): List<LexiconEntry>

    @Query("""
        SELECT entries.* FROM entries
        WHERE entries._id IN (
            SELECT _id FROM entries
            WHERE entry_type != 'smithhall'
              AND latin_key LIKE :prefix || '%'
            UNION
            SELECT entry_aliases.entry_id FROM entry_aliases
            JOIN entries ON entries._id = entry_aliases.entry_id
            WHERE entries.entry_type != 'smithhall'
              AND entry_aliases.latin_key LIKE :prefix || '%'
        )
        ORDER BY entries.latin_key
        LIMIT :limit
    """)
    suspend fun searchGreekByLatinPrefix(prefix: String, limit: Int = 50): List<LexiconEntry>

    @Query("SELECT * FROM entries WHERE entry_type = 'smithhall' AND key LIKE :prefix || '%' ORDER BY key, headword LIMIT :limit")
    suspend fun searchLatinByPrefix(prefix: String, limit: Int = 50): List<LexiconEntry>

    @Query("SELECT * FROM entries WHERE _id = :id")
    suspend fun getById(id: Long): LexiconEntry?

    @Query("""
        SELECT entries.* FROM entries
        WHERE entries.key = :key
        UNION
        SELECT entries.* FROM entries
        JOIN entry_aliases ON entry_aliases.entry_id = entries._id
        WHERE entry_aliases.alias_key = :key
        LIMIT 1
    """)
    suspend fun getByKey(key: String): LexiconEntry?
}
