package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Query("SELECT * FROM family_members WHERE groupId = :groupId ORDER BY sortOrder ASC")
    fun getMembersByGroupId(groupId: Long): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE groupId = :groupId ORDER BY sortOrder ASC")
    suspend fun getMembersByGroupIdList(groupId: Long): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMemberEntity): Long

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM family_members WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: Long)
}
