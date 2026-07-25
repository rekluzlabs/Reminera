package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_export_manifests")
data class BookExportManifestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val title: String,
    val memberOrderJson: String,
    val dateCreated: Long,
    val lastModified: Long
)
