package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChapterExportDao {

    @Query("SELECT * FROM chapter_exports WHERE memberId = :memberId LIMIT 1")
    suspend fun getByMemberId(memberId: Long): ChapterExportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chapter: ChapterExportEntity)

    @Query("DELETE FROM chapter_exports WHERE memberId = :memberId")
    suspend fun deleteByMemberId(memberId: Long)

    @Query("UPDATE chapter_exports SET renderedPdfPath = :path, renderedPdfHash = :hash WHERE memberId = :memberId")
    suspend fun updateRenderedPdf(memberId: Long, path: String?, hash: String?)
}
