package com.dreamjournal.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dream_entries")
data class DreamEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val dreamDate: String,
    val recordType: String,
    val title: String,
    val dreamTag: String,
    val content: String,
    val photoPaths: String,
    val todoItems: String,
    val audioPath: String?,
    val transcript: String?,
    val aiSummary: String?,
    val aiKeywords: String?,
    val aiEmotion: String?,
    val aiSuggestions: String?,
    // Keeps the original audioPath compatible while allowing later voice additions.
    val extraAudioPaths: String = "[]",
    val contentBlocks: String = "[]",
    val weatherText: String? = null,
    val locationText: String? = null
)
