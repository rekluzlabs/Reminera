package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biography_sections")
data class BiographySectionEntity(
    @PrimaryKey val id: String,
    val biographyId: String,
    val sectionType: String,
    val fieldsJson: String,
    val updatedAt: Long
)
