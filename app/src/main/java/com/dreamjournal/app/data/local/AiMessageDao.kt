package com.dreamjournal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeByEntryId(entryId: Long): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiMessageEntity): Long
}
