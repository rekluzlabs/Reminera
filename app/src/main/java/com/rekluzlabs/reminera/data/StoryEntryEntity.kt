package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_entries")
data class StoryEntryEntity(
    @PrimaryKey val id: String,
    val biographyId: String,
    val contributedBy: String,
    val type: String,
    val mediaUri: String?,
    val textContent: String?,
    val recordedAt: Long,
    val createdAt: Long
)
