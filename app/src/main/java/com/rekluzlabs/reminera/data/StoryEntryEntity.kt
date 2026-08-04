package com.rekluzlabs.reminera.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "story_entries")
data class StoryEntryEntity(
    @PrimaryKey val id: String,
    val biographyId: String,
    val contributedBy: String,
    val type: String,
    val mediaUri: String?,
    val thumbnailUri: String? = null,
    val textContent: String?,
    val recordedAt: Long,
    val createdAt: Long,
    val sortOrder: Int = 0
) : Parcelable
