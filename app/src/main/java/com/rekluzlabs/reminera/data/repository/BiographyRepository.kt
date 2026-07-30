package com.rekluzlabs.reminera.data.repository

import com.rekluzlabs.reminera.data.BiographyDao
import com.rekluzlabs.reminera.data.BiographyEntity
import com.rekluzlabs.reminera.data.BiographySectionDao
import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.StoryEntryDao
import com.rekluzlabs.reminera.data.StoryEntryEntity
import kotlinx.coroutines.flow.Flow

class BiographyRepository(
    private val biographyDao: BiographyDao,
    private val sectionDao: BiographySectionDao,
    private val storyEntryDao: StoryEntryDao
) {
    fun getBiographyByPersonId(personId: Long): Flow<BiographyEntity?> =
        biographyDao.getByPersonId(personId)

    suspend fun getBiographyById(id: String): BiographyEntity? =
        biographyDao.getById(id)

    suspend fun upsertBiography(biography: BiographyEntity) =
        biographyDao.insert(biography)

    fun getSectionsByBiographyId(biographyId: String): Flow<List<BiographySectionEntity>> =
        sectionDao.getByBiographyId(biographyId)

    suspend fun getSectionByBiographyIdAndType(biographyId: String, sectionType: String): BiographySectionEntity? =
        sectionDao.getByBiographyIdAndType(biographyId, sectionType)

    suspend fun upsertSection(section: BiographySectionEntity) =
        sectionDao.insert(section)

    fun getStoryEntriesByBiographyId(biographyId: String): Flow<List<StoryEntryEntity>> =
        storyEntryDao.getByBiographyId(biographyId)

    suspend fun insertStoryEntry(entry: StoryEntryEntity) =
        storyEntryDao.insert(entry)

    suspend fun deleteStoryEntry(id: String) =
        storyEntryDao.deleteById(id)

    suspend fun updateStoryEntryMedia(id: String, mediaUri: String?) =
        storyEntryDao.updateMediaUri(id, mediaUri)

    suspend fun updateStoryEntryText(id: String, textContent: String?) =
        storyEntryDao.updateTextContent(id, textContent)

    suspend fun updateStoryEntryContributedBy(id: String, contributedBy: String) =
        storyEntryDao.updateContributedBy(id, contributedBy)

    suspend fun updateStoryEntryRecordedAt(id: String, recordedAt: Long) =
        storyEntryDao.updateRecordedAt(id, recordedAt)

    suspend fun updateStoryEntrySortOrder(id: String, sortOrder: Int) =
        storyEntryDao.updateSortOrder(id, sortOrder)

    suspend fun getStoryEntryById(id: String): StoryEntryEntity? =
        storyEntryDao.getById(id)
}
