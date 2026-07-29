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

    @Query("UPDATE chapter_exports SET biographySource = :source, aiPolishedAt = :aiPolishedAt WHERE memberId = :memberId")
    suspend fun updateBiographySource(memberId: Long, source: String, aiPolishedAt: Long?)

    @Query("UPDATE chapter_exports SET generatedBioText = :text, biographySource = :source, sourceDataHash = :hash, aiPolishedAt = :aiPolishedAt, lastGenerated = :lastGenerated WHERE memberId = :memberId")
    suspend fun updateAiPolished(memberId: Long, text: String, source: String, hash: String, aiPolishedAt: Long, lastGenerated: Long)

    @Query("SELECT * FROM chapter_exports ORDER BY memberId ASC")
    suspend fun getAllChaptersList(): List<ChapterExportEntity>
}
