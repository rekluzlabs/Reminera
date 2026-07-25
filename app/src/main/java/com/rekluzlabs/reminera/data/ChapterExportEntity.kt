package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapter_exports")
data class ChapterExportEntity(
    @PrimaryKey
    val memberId: Long,
    val groupId: Long,
    val sourceDataHash: String,
    val generatedBioText: String,
    val mediaManifestJson: String,
    val lastGenerated: Long,
    val renderedPdfPath: String? = null,
    val renderedPdfHash: String? = null,
    val biographySource: String = "RAW",
    val aiPolishedAt: Long? = null
)
