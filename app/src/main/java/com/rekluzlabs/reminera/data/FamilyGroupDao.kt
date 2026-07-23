package com.rekluzlabs.reminera.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class GroupEntryCount(val groupId: Long, val cnt: Int)
data class GroupMemberCount(val groupId: Long, val cnt: Int)

@Dao
interface FamilyGroupDao {

    @Query("SELECT * FROM family_groups ORDER BY sortOrder ASC")
    fun getAllOrderedBySortOrder(): Flow<List<FamilyGroupEntity>>

    @Query("SELECT groupId, COUNT(*) AS cnt FROM memory_entries GROUP BY groupId")
    fun getEntryCounts(): Flow<List<GroupEntryCount>>

    @Query("SELECT groupId, COUNT(*) AS cnt FROM family_members GROUP BY groupId")
    fun getMemberCounts(): Flow<List<GroupMemberCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: FamilyGroupEntity): Long

    @Update
    suspend fun update(group: FamilyGroupEntity)

    @Transaction
    suspend fun updateSortOrders(idToOrderMap: Map<Long, Int>) {
        idToOrderMap.forEach { (id, sortOrder) ->
            updateSortOrder(id, sortOrder)
        }
    }

    @Query("UPDATE family_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun deleteByIds(ids: List<Long>) {
        deleteMemoryEntriesForGroups(ids)
        deleteGroups(ids)
    }

    @Query("DELETE FROM memory_entries WHERE groupId IN (:groupIds)")
    suspend fun deleteMemoryEntriesForGroups(groupIds: List<Long>)

    @Query("DELETE FROM family_groups WHERE id IN (:ids)")
    suspend fun deleteGroups(ids: List<Long>)
}
