package com.rekluzlabs.reminera.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : Parcelable
