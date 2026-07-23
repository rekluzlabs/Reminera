package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryEntryDao {

    @Query("SELECT * FROM memory_entries ORDER BY dateCaptured DESC")
    fun getAllEntries(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE personTag = :personTag ORDER BY dateCaptured DESC")
    fun getEntriesByPersonTag(personTag: String): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE type = :type ORDER BY dateCaptured DESC")
    fun getEntriesByType(type: String): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE uploadStatus = 'UPLOADED' ORDER BY dateCaptured DESC")
    fun getUploadedEntries(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE groupId = :groupId ORDER BY dateCaptured DESC")
    fun getEntriesByGroupId(groupId: Long): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE groupId = :groupId AND personTag = :personTag ORDER BY dateCaptured DESC")
    fun getEntriesByGroupIdAndPersonTag(groupId: Long, personTag: String): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE id = :id")
    fun getEntryById(id: String): Flow<MemoryEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntryEntity)

    @Update
    suspend fun update(entry: MemoryEntryEntity)

    @Delete
    suspend fun delete(entry: MemoryEntryEntity)
}
