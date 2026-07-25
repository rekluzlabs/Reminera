package com.rekluzlabs.reminera.data.repository

import com.rekluzlabs.reminera.data.BookExportManifestDao
import com.rekluzlabs.reminera.data.BookExportManifestEntity
import org.json.JSONArray

class BookExportManifestRepository(
    private val manifestDao: BookExportManifestDao
) {

    suspend fun getOrCreateManifestForGroup(groupId: Long): BookExportManifestEntity {
        val existing = manifestDao.getByGroupId(groupId)
        if (existing != null) return existing

        val manifest = BookExportManifestEntity(
            groupId = groupId,
            title = "",
            memberOrderJson = "[]",
            dateCreated = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        val id = manifestDao.insert(manifest)
        return manifest.copy(id = id)
    }

    suspend fun addMemberToManifest(manifestId: Long, memberId: Long) {
        val manifest = manifestDao.getById(manifestId) ?: return
        val memberIds = parseMemberOrder(manifest.memberOrderJson).toMutableList()
        if (memberId !in memberIds) {
            memberIds.add(memberId)
            manifestDao.update(
                manifest.copy(
                    memberOrderJson = serializeMemberOrder(memberIds),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeMemberFromManifest(manifestId: Long, memberId: Long) {
        val manifest = manifestDao.getById(manifestId) ?: return
        val memberIds = parseMemberOrder(manifest.memberOrderJson).toMutableList()
        if (memberIds.remove(memberId)) {
            manifestDao.update(
                manifest.copy(
                    memberOrderJson = serializeMemberOrder(memberIds),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun reorderManifest(manifestId: Long, newOrder: List<Long>) {
        val manifest = manifestDao.getById(manifestId) ?: return
        manifestDao.update(
            manifest.copy(
                memberOrderJson = serializeMemberOrder(newOrder),
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun getManifestById(id: Long): BookExportManifestEntity? =
        manifestDao.getById(id)

    companion object {
        fun parseMemberOrder(json: String): List<Long> {
            if (json.isBlank() || json == "[]") return emptyList()
            val arr = JSONArray(json)
            return (0 until arr.length()).map { arr.getLong(it) }
        }

        fun serializeMemberOrder(memberIds: List<Long>): String {
            val arr = JSONArray()
            memberIds.forEach { arr.put(it) }
            return arr.toString()
        }
    }
}
