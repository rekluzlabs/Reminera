package com.rekluzlabs.reminera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biographies")
data class BiographyEntity(
    @PrimaryKey val id: String,
    val personId: Long,
    val fullName: String,
    val relationship: String,
    val birthDate: Long?,
    val familyGroupId: Long,
    val photoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
