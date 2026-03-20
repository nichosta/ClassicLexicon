package com.greeklexicon.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(contentEntity = LexiconEntry::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "entries_fts")
data class LexiconEntryFts(
    @ColumnInfo(name = "headword") val headword: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "short_def") val shortDef: String?,
)
