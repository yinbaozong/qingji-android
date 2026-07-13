package com.dreamjournal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DreamEntryEntity::class, AiMessageEntity::class],
    version = 7,
    exportSchema = false
)
abstract class DreamDatabase : RoomDatabase() {
    abstract fun dreamEntryDao(): DreamEntryDao
    abstract fun aiMessageDao(): AiMessageDao
}
