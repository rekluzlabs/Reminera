package com.rekluzlabs.reminera.data.repository

import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

class FamilyMemberRepository(private val dao: FamilyMemberDao) {

    fun getMembersByGroupId(groupId: Long): Flow<List<FamilyMemberEntity>> =
        dao.getMembersByGroupId(groupId)

    suspend fun getMembersByGroupIdList(groupId: Long): List<FamilyMemberEntity> =
        dao.getMembersByGroupIdList(groupId)

    suspend fun getMemberById(id: Long): FamilyMemberEntity? =
        dao.getMemberById(id)

    suspend fun insert(member: FamilyMemberEntity): Long = dao.insert(member)

    suspend fun update(member: FamilyMemberEntity) = dao.update(member)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteByGroupId(groupId: Long) = dao.deleteByGroupId(groupId)
}
