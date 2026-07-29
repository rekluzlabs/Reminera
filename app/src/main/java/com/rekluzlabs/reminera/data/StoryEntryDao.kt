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

    @Query("SELECT * FROM story_entries WHERE biographyId = :biographyId ORDER BY recordedAt DESC")
    suspend fun getByBiographyIdOnce(biographyId: String): List<StoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StoryEntryEntity)

    @Query("DELETE FROM story_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE story_entries SET mediaUri = :mediaUri WHERE id = :id")
    suspend fun updateMediaUri(id: String, mediaUri: String?)

    @Query("UPDATE story_entries SET thumbnailUri = :thumbnailUri WHERE id = :id")
    suspend fun updateThumbnailUri(id: String, thumbnailUri: String?)

    @Query("UPDATE story_entries SET textContent = :textContent WHERE id = :id")
    suspend fun updateTextContent(id: String, textContent: String?)

    @Query("SELECT * FROM story_entries WHERE id = :id")
    suspend fun getById(id: String): StoryEntryEntity?

    @Query("SELECT * FROM story_entries ORDER BY recordedAt DESC")
    suspend fun getAllStoriesList(): List<StoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirect(entry: StoryEntryEntity)
}
