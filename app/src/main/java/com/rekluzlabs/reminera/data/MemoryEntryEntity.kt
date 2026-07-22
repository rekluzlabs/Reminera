package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey
    val id: String,
    val groupId: Long = 0L,
    val title: String,
    val type: String,
    val localFilePath: String,
    val thumbnailPath: String?,
    val personTag: String?,
    val notes: String?,
    val dateCaptured: Long,
    val dateAdded: Long,
    val durationMillis: Long?,
    val isImported: Boolean,
    val uploadStatus: String,
    val hostedUrl: String?,
    // Optional attachment for PHOTO entries: an audio or video clip that
    // plays alongside the photo in the full-screen viewer. Null for all
    // other entries, and null on PHOTO entries with no attachment.
    val secondaryMediaPath: String? = null,
    val secondaryMediaType: String? = null   // "AUDIO" or "VIDEO"
)

object Converters {
    @TypeConverter
    fun fromMemoryType(value: MemoryType): String = value.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = MemoryType.valueOf(value)

    @TypeConverter
    fun fromUploadStatus(value: UploadStatus): String = value.name

    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus = UploadStatus.valueOf(value)
}
