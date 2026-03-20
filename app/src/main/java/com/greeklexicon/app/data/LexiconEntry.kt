package com.greeklexicon.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["key"], name = "idx_entries_key"),
        Index(value = ["latin_key"], name = "idx_entries_latin_key"),
    ],
)
data class LexiconEntry(
    @PrimaryKey
    @ColumnInfo(name = "_id") val id: Long,
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "headword") val headword: String,
    @ColumnInfo(name = "latin_key") val latinKey: String,
    @ColumnInfo(name = "entry_type") val entryType: String,
    @ColumnInfo(name = "short_def") val shortDef: String?,
    @ColumnInfo(name = "xml_content") val xmlContent: String,
)
