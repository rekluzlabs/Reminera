package com.example.reminera.data.repository

import com.example.reminera.data.MemoryEntryDao
import com.example.reminera.data.MemoryEntryEntity
import kotlinx.coroutines.flow.Flow

class MemoryEntryRepository(private val dao: MemoryEntryDao) {

    val allEntries: Flow<List<MemoryEntryEntity>> = dao.getAllEntries()

    fun getEntriesByPersonTag(personTag: String): Flow<List<MemoryEntryEntity>> =
        dao.getEntriesByPersonTag(personTag)

    fun getEntriesByType(type: String): Flow<List<MemoryEntryEntity>> =
        dao.getEntriesByType(type)

    fun getUploadedEntries(): Flow<List<MemoryEntryEntity>> =
        dao.getUploadedEntries()

    fun getEntryById(id: String): Flow<MemoryEntryEntity?> =
        dao.getEntryById(id)

    suspend fun insert(entry: MemoryEntryEntity) = dao.insert(entry)

    suspend fun update(entry: MemoryEntryEntity) = dao.update(entry)

    suspend fun delete(entry: MemoryEntryEntity) = dao.delete(entry)
}
