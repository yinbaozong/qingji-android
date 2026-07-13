package com.dreamjournal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamEntryDao {
    @Query("SELECT * FROM dream_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DreamEntryEntity>>

    @Query("SELECT * FROM dream_entries WHERE id = :id")
    fun observeById(id: Long): Flow<DreamEntryEntity?>

    @Query("SELECT * FROM dream_entries WHERE dreamDate = :date ORDER BY createdAt DESC")
    fun observeByDate(date: String): Flow<List<DreamEntryEntity>>

    @Query("SELECT * FROM dream_entries WHERE id = :id")
    suspend fun getById(id: Long): DreamEntryEntity?

    @Query("SELECT * FROM dream_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<DreamEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DreamEntryEntity): Long

    @Update
    suspend fun update(entry: DreamEntryEntity)

    @Query("DELETE FROM dream_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
