package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryEntryDao {

    @Query("SELECT * FROM story_entries WHERE biographyId = :biographyId ORDER BY recordedAt DESC")
    fun getByBiographyId(biographyId: String): Flow<List<StoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StoryEntryEntity)

    @Query("DELETE FROM story_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE story_entries SET mediaUri = :mediaUri WHERE id = :id")
    suspend fun updateMediaUri(id: String, mediaUri: String?)
}
