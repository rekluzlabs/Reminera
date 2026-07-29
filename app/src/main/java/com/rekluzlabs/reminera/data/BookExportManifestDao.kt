package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookExportManifestDao {

    @Query("SELECT * FROM book_export_manifests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BookExportManifestEntity?

    @Query("SELECT * FROM book_export_manifests WHERE groupId = :groupId LIMIT 1")
    suspend fun getByGroupId(groupId: Long): BookExportManifestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manifest: BookExportManifestEntity): Long

    @Update
    suspend fun update(manifest: BookExportManifestEntity)

    @Query("DELETE FROM book_export_manifests WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM book_export_manifests ORDER BY id ASC")
    suspend fun getAllManifestsList(): List<BookExportManifestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirect(manifest: BookExportManifestEntity)
}
