package com.example.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey
    val id: String,
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
    val hostedUrl: String?
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
