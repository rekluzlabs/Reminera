package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BiographySectionDao {

    @Query("SELECT * FROM biography_sections WHERE biographyId = :biographyId ORDER BY sectionType ASC")
    fun getByBiographyId(biographyId: String): Flow<List<BiographySectionEntity>>

    @Query("SELECT * FROM biography_sections WHERE biographyId = :biographyId AND sectionType = :sectionType LIMIT 1")
    suspend fun getByBiographyIdAndType(biographyId: String, sectionType: String): BiographySectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: BiographySectionEntity)

    @Update
    suspend fun update(section: BiographySectionEntity)
}
