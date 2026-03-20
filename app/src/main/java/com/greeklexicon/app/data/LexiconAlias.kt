package com.greeklexicon.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_aliases",
    foreignKeys = [
        ForeignKey(
            entity = LexiconEntry::class,
            parentColumns = ["_id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["entry_id"], name = "idx_entry_aliases_entry_id"),
        Index(value = ["alias_key"], name = "idx_entry_aliases_alias_key"),
        Index(value = ["latin_key"], name = "idx_entry_aliases_latin_key"),
    ],
)
data class LexiconAlias(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id") val id: Long = 0,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "alias_key") val aliasKey: String,
    @ColumnInfo(name = "latin_key") val latinKey: String,
)
