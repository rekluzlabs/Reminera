package com.rekluzlabs.reminera.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "family_groups")
data class FamilyGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val groupType: String,
    val sortOrder: Int,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
