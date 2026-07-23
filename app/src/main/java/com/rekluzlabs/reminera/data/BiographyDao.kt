package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BiographyDao {

    @Query("SELECT * FROM biographies WHERE personId = :personId LIMIT 1")
    fun getByPersonId(personId: Long): Flow<BiographyEntity?>

    @Query("SELECT * FROM biographies WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BiographyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(biography: BiographyEntity)

    @Update
    suspend fun update(biography: BiographyEntity)

    @Query("DELETE FROM biographies WHERE id = :id")
    suspend fun deleteById(id: String)
}
