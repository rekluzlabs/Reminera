package com.rekluzlabs.reminera.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekluzlabs.reminera.data.repository.ChapterExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterExportRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: RemineraDatabase
    private lateinit var repo: ChapterExportRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemineraDatabase::class.java
        ).allowMainThreadQueries().build()

        repo = ChapterExportRepository(
            chapterDao = db.chapterExportDao(),
            memberDao = db.familyMemberDao(),
            biographyDao = db.biographyDao(),
            sectionDao = db.biographySectionDao(),
            storyDao = db.storyEntryDao(),
            memoryDao = db.memoryEntryDao()
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun insertGroup(id: Long = 10L, name: String = "Test Group") {
        db.familyGroupDao().insert(
            FamilyGroupEntity(id = id, name = name, groupType = "CUSTOM", sortOrder = 0)
        )
    }

    private fun member(
        id: Long = 1L,
        groupId: Long = 10L,
        name: String = "Alice",
        role: String = "Mother"
    ) = FamilyMemberEntity(
        id = id,
        groupId = groupId,
        name = name,
        role = role,
        biography = "",
        birthDate = null,
        sortOrder = 0,
        createdAt = System.currentTimeMillis()
    )

    private fun biography(
        id: String = "bio-1",
        personId: Long = 1L
    ) = BiographyEntity(
        id = id,
        personId = personId,
        fullName = "Alice Smith",
        relationship = "Mother",
        birthDate = null,
        familyGroupId = 10L,
        photoUri = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    private fun section(
        id: String = "sec-1",
        biographyId: String = "bio-1",
        sectionType: String = "early-life",
        fieldsJson: String = """{"text":"Grew up in Ohio"}"""
    ) = BiographySectionEntity(
        id = id,
        biographyId = biographyId,
        sectionType = sectionType,
        fieldsJson = fieldsJson,
        updatedAt = System.currentTimeMillis()
    )

    @Test
    fun cacheHit_returnsSameLastGenerated() = runTest {
        insertGroup()
        db.familyMemberDao().insert(member())

        val chapter1 = repo.getOrGenerateChapter(1L)
        val chapter2 = repo.getOrGenerateChapter(1L)

        assertEquals(chapter1.lastGenerated, chapter2.lastGenerated)
        assertEquals(chapter1.sourceDataHash, chapter2.sourceDataHash)
    }

    @Test
    fun bioEdit_invalidatesCache() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        val bioDao = db.biographyDao()
        val secDao = db.biographySectionDao()

        memberDao.insert(member())
        bioDao.insert(biography())
        secDao.insert(section())

        val chapter1 = repo.getOrGenerateChapter(1L)

        val updatedSection = section().copy(fieldsJson = """{"text":"Grew up in Texas"}""")
        secDao.insert(updatedSection)

        val chapter2 = repo.getOrGenerateChapter(1L)

        assertNotEquals(chapter1.sourceDataHash, chapter2.sourceDataHash)
        assert(chapter2.lastGenerated >= chapter1.lastGenerated)
    }

    @Test
    fun mediaReorder_invalidatesCache() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        val memDao = db.memoryEntryDao()

        memberDao.insert(member())

        memDao.insert(memoryEntry(id = "m1", groupId = 10L, personTag = "Alice", sortOrder = 0))
        memDao.insert(memoryEntry(id = "m2", groupId = 10L, personTag = "Alice", sortOrder = 1))

        val chapter1 = repo.getOrGenerateChapter(1L)

        memDao.updateSortOrder("m1", 1)
        memDao.updateSortOrder("m2", 0)

        val chapter2 = repo.getOrGenerateChapter(1L)

        assertNotEquals(chapter1.sourceDataHash, chapter2.sourceDataHash)
    }

    @Test
    fun chapterContainsMediaManifestJson() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        val memDao = db.memoryEntryDao()

        memberDao.insert(member())
        memDao.insert(memoryEntry(id = "m1", groupId = 10L, personTag = "Alice", sortOrder = 0))
        memDao.insert(memoryEntry(id = "m2", groupId = 10L, personTag = "Alice", sortOrder = 1))

        val chapter = repo.getOrGenerateChapter(1L)

        assertNotNull(chapter.mediaManifestJson)
        assert(chapter.mediaManifestJson.contains("m1"))
        assert(chapter.mediaManifestJson.contains("m2"))
    }

    @Test
    fun chapterContainsPlaceholderText() = runTest {
        insertGroup()
        db.familyMemberDao().insert(member())

        val chapter = repo.getOrGenerateChapter(1L)

        assert(chapter.generatedBioText.contains("[Placeholder biography"))
        assert(chapter.generatedBioText.contains("Alice"))
        assert(chapter.generatedBioText.contains("Mother"))
    }

    private fun memoryEntry(
        id: String,
        groupId: Long,
        personTag: String,
        sortOrder: Int
    ) = MemoryEntryEntity(
        id = id,
        groupId = groupId,
        title = "Media $id",
        type = "PHOTO",
        localFilePath = "/tmp/$id.jpg",
        thumbnailPath = null,
        personTag = personTag,
        notes = null,
        dateCaptured = System.currentTimeMillis(),
        dateAdded = System.currentTimeMillis(),
        durationMillis = null,
        isImported = true,
        uploadStatus = "NOT_UPLOADED",
        hostedUrl = null,
        sortOrder = sortOrder
    )
}
