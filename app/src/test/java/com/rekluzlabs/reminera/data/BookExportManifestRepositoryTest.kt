package com.rekluzlabs.reminera.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekluzlabs.reminera.data.repository.BookExportManifestRepository
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
class BookExportManifestRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: RemineraDatabase
    private lateinit var manifestRepo: BookExportManifestRepository
    private lateinit var chapterRepo: ChapterExportRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemineraDatabase::class.java
        ).allowMainThreadQueries().build()

        manifestRepo = BookExportManifestRepository(db.bookExportManifestDao())
        chapterRepo = ChapterExportRepository(
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
        id: Long,
        groupId: Long = 10L,
        name: String,
        role: String = "Member"
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

    @Test
    fun addingThirdMember_doesNotTouchExistingChapters() = runTest {
        insertGroup()
        val memberDao = db.familyMemberDao()
        memberDao.insert(member(id = 1L, name = "Alice"))
        memberDao.insert(member(id = 2L, name = "Bob"))

        val manifest = manifestRepo.getOrCreateManifestForGroup(10L)
        manifestRepo.addMemberToManifest(manifest.id, 1L)
        manifestRepo.addMemberToManifest(manifest.id, 2L)

        val chapterA = chapterRepo.getOrGenerateChapter(1L)
        val chapterB = chapterRepo.getOrGenerateChapter(2L)

        val beforeAddA = chapterA.lastGenerated
        val beforeAddB = chapterB.lastGenerated

        memberDao.insert(member(id = 3L, name = "Charlie"))
        manifestRepo.addMemberToManifest(manifest.id, 3L)

        val chapterAAfter = db.chapterExportDao().getByMemberId(1L)
        val chapterBAfter = db.chapterExportDao().getByMemberId(2L)

        assertNotNull(chapterAAfter)
        assertNotNull(chapterBAfter)
        assertEquals(beforeAddA, chapterAAfter!!.lastGenerated)
        assertEquals(beforeAddB, chapterBAfter!!.lastGenerated)
    }

    @Test
    fun getOrCreateManifestForGroup_createsNewWhenNoneExists() = runTest {
        val manifest = manifestRepo.getOrCreateManifestForGroup(10L)

        assertNotNull(manifest)
        assertEquals(10L, manifest.groupId)
        assertEquals("[]", manifest.memberOrderJson)
    }

    @Test
    fun getOrCreateManifestForGroup_returnsExisting() = runTest {
        val manifest1 = manifestRepo.getOrCreateManifestForGroup(10L)
        val manifest2 = manifestRepo.getOrCreateManifestForGroup(10L)

        assertEquals(manifest1.id, manifest2.id)
    }

    @Test
    fun addAndRemoveMember_updatesManifest() = runTest {
        val manifest = manifestRepo.getOrCreateManifestForGroup(10L)
        manifestRepo.addMemberToManifest(manifest.id, 1L)
        manifestRepo.addMemberToManifest(manifest.id, 2L)

        val updated = manifestRepo.getManifestById(manifest.id)!!
        val memberIds = BookExportManifestRepository.parseMemberOrder(updated.memberOrderJson)
        assertEquals(listOf(1L, 2L), memberIds)

        manifestRepo.removeMemberFromManifest(manifest.id, 1L)
        val afterRemove = manifestRepo.getManifestById(manifest.id)!!
        val memberIdsAfter = BookExportManifestRepository.parseMemberOrder(afterRemove.memberOrderJson)
        assertEquals(listOf(2L), memberIdsAfter)
    }

    @Test
    fun reorderManifest_updatesOrder() = runTest {
        val manifest = manifestRepo.getOrCreateManifestForGroup(10L)
        manifestRepo.addMemberToManifest(manifest.id, 1L)
        manifestRepo.addMemberToManifest(manifest.id, 2L)

        manifestRepo.reorderManifest(manifest.id, listOf(2L, 1L))

        val updated = manifestRepo.getManifestById(manifest.id)!!
        val memberIds = BookExportManifestRepository.parseMemberOrder(updated.memberOrderJson)
        assertEquals(listOf(2L, 1L), memberIds)
    }

    @Test
    fun addDuplicateMember_doesNotDuplicate() = runTest {
        val manifest = manifestRepo.getOrCreateManifestForGroup(10L)
        manifestRepo.addMemberToManifest(manifest.id, 1L)
        manifestRepo.addMemberToManifest(manifest.id, 1L)

        val updated = manifestRepo.getManifestById(manifest.id)!!
        val memberIds = BookExportManifestRepository.parseMemberOrder(updated.memberOrderJson)
        assertEquals(listOf(1L), memberIds)
    }
}
