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
import org.junit.Assert.assertTrue
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
        role: String = "Mother",
        biography: String = ""
    ) = FamilyMemberEntity(
        id = id,
        groupId = groupId,
        name = name,
        role = role,
        biography = biography,
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
        sectionType: String = "origins",
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
    fun chapterContainsRawAssembledText() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        memberDao.insert(member(biography = "I grew up in a small town."))

        val chapter = repo.getOrGenerateChapter(1L)

        assertEquals("RAW", chapter.biographySource)
        assertTrue(chapter.generatedBioText.contains("I grew up in a small town."))
    }

    @Test
    fun hashMatch_returnsCachedRegardlessOfSource() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        memberDao.insert(member(biography = "Hello"))

        val chapter1 = repo.getOrGenerateChapter(1L)
        assertEquals("RAW", chapter1.biographySource)

        db.chapterExportDao().updateBiographySource(1L, "AI_POLISHED", System.currentTimeMillis())

        val chapter2 = repo.getOrGenerateChapter(1L)
        assertEquals("AI_POLISHED", chapter2.biographySource)
        assertEquals(chapter1.sourceDataHash, chapter2.sourceDataHash)
    }

    @Test
    fun hashMismatch_producesRawText() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        memberDao.insert(member(biography = "Version 1"))

        val chapter1 = repo.getOrGenerateChapter(1L)
        assertEquals("RAW", chapter1.biographySource)

        memberDao.update(member(biography = "Version 2"))

        val chapter2 = repo.getOrGenerateChapter(1L)
        assertEquals("RAW", chapter2.biographySource)
        assertTrue(chapter2.generatedBioText.contains("Version 2"))
        assertNotEquals(chapter1.sourceDataHash, chapter2.sourceDataHash)
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
