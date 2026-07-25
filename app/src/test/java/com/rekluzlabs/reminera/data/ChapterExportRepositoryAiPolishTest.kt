package com.rekluzlabs.reminera.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rekluzlabs.reminera.data.repository.ChapterExportRepository
import com.rekluzlabs.reminera.export.BiographyGenerationInput
import com.rekluzlabs.reminera.export.BiographyGenerationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterExportRepositoryAiPolishTest {

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

        runBlocking {
            db.familyGroupDao().insert(
                FamilyGroupEntity(id = 10L, name = "Test Group", groupType = "CUSTOM", sortOrder = 0)
            )
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun member(
        biography: String = "I grew up in Ohio."
    ) = FamilyMemberEntity(
        id = 1L,
        groupId = 10L,
        name = "Alice",
        role = "Mother",
        biography = biography,
        birthDate = null,
        sortOrder = 0,
        createdAt = System.currentTimeMillis()
    )

    private fun successProvider(text: String = "Polished text."): BiographyGenerationProvider {
        return BiographyGenerationProvider { _ -> Result.success(text) }
    }

    private fun failingProvider(message: String = "API error"): BiographyGenerationProvider {
        return BiographyGenerationProvider { _ -> Result.failure(Exception(message)) }
    }

    @Test
    fun requestAiPolish_success_updatesAllFields() = runTest {
        db.familyMemberDao().insert(member())
        repo.getOrGenerateChapter(1L)

        val result = repo.requestAiPolish(1L, successProvider("Beautiful polished text."))
        assertTrue(result.isSuccess)

        val chapter = db.chapterExportDao().getByMemberId(1L)!!
        assertEquals("Beautiful polished text.", chapter.generatedBioText)
        assertEquals("AI_POLISHED", chapter.biographySource)
        assertTrue(chapter.aiPolishedAt != null)
        assertTrue(chapter.aiPolishedAt!! > 0)
    }

    @Test
    fun requestAiPolish_failure_leavesEntityUntouched() = runTest {
        db.familyMemberDao().insert(member(biography = "Original bio."))
        val chapter1 = repo.getOrGenerateChapter(1L)
        val originalText = chapter1.generatedBioText
        val originalSource = chapter1.biographySource
        val originalHash = chapter1.sourceDataHash

        val result = repo.requestAiPolish(1L, failingProvider("Network error"))
        assertTrue(result.isFailure)

        val chapter2 = db.chapterExportDao().getByMemberId(1L)!!
        assertEquals(originalText, chapter2.generatedBioText)
        assertEquals(originalSource, chapter2.biographySource)
        assertEquals(originalHash, chapter2.sourceDataHash)
        assertTrue(chapter2.aiPolishedAt == null)
    }

    @Test
    fun hashChangeAfterPolish_revertsToRaw() = runTest {
        db.familyMemberDao().insert(member(biography = "Version 1"))
        repo.getOrGenerateChapter(1L)

        repo.requestAiPolish(1L, successProvider("Polished version 1."))
        val polished = db.chapterExportDao().getByMemberId(1L)!!
        assertEquals("AI_POLISHED", polished.biographySource)

        db.familyMemberDao().update(member(biography = "Version 2"))

        val fresh = repo.getOrGenerateChapter(1L)
        assertEquals("RAW", fresh.biographySource)
        assertTrue(fresh.generatedBioText.contains("Version 2"))
        assertNotEquals(polished.sourceDataHash, fresh.sourceDataHash)
    }

    @Test
    fun requestAiPolish_noChapterExists_returnsFailure() = runTest {
        val result = repo.requestAiPolish(999L, successProvider())
        assertTrue(result.isFailure)
    }

    @Test
    fun buildBiographyInput_includesAllData() = runTest {
        db.familyMemberDao().insert(member(biography = "My life story."))
        repo.getOrGenerateChapter(1L)

        val input = repo.buildBiographyInput(1L)
        assertEquals("Alice", input.name)
        assertEquals("Mother", input.relationship)
        assertEquals("My life story.", input.biographyText)
    }

    @Test
    fun buildBiographyInput_noBiography_emptyText() = runTest {
        db.familyMemberDao().insert(member(biography = ""))
        repo.getOrGenerateChapter(1L)

        val input = repo.buildBiographyInput(1L)
        assertEquals("", input.biographyText)
        assertTrue(input.sections.isEmpty())
        assertTrue(input.stories.isEmpty())
    }
}
