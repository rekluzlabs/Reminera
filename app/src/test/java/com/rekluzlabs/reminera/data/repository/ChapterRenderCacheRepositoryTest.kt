package com.rekluzlabs.reminera.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rekluzlabs.reminera.data.ChapterExportDao
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryDao
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.export.ChapterPdfRenderer.RenderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChapterRenderCacheRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: RemineraDatabase
    private lateinit var chapterDao: ChapterExportDao
    private lateinit var memberDao: FamilyMemberDao
    private lateinit var memoryDao: MemoryEntryDao
    private lateinit var context: Context
    private lateinit var renderCountFile: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RemineraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        chapterDao = db.chapterExportDao()
        memberDao = db.familyMemberDao()
        memoryDao = db.memoryEntryDao()
        renderCountFile = File(context.cacheDir, "render_count.txt")
        renderCountFile.writeText("0")

        runBlocking {
            db.familyGroupDao().insert(
                FamilyGroupEntity(id = 10L, name = "Test Group", groupType = "CUSTOM", sortOrder = 0)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
        renderCountFile.delete()
    }

    private fun fakeMember(id: Long = 1L) = FamilyMemberEntity(
        id = id,
        groupId = 10L,
        name = "Alice",
        role = "Mother",
        birthDate = 867734400000L,
        createdAt = System.currentTimeMillis()
    )

    private fun fakeChapter(
        memberId: Long = 1L,
        hash: String = "abc123",
        renderedPdfPath: String? = null,
        renderedPdfHash: String? = null
    ) = ChapterExportEntity(
        memberId = memberId,
        groupId = 10L,
        sourceDataHash = hash,
        generatedBioText = "Test bio",
        mediaManifestJson = "[]",
        lastGenerated = System.currentTimeMillis(),
        renderedPdfPath = renderedPdfPath,
        renderedPdfHash = renderedPdfHash
    )

    private fun createDummyPdf(path: String): File {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
        return file
    }

    private fun countingRenderer(): ChapterRenderer {
        return ChapterRenderer { _, _, _, _ ->
            val count = renderCountFile.readText().trim().toInt() + 1
            renderCountFile.writeText(count.toString())
            val dummyFile = File(context.cacheDir, "dummy_rendered.pdf")
            dummyFile.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            RenderResult.Success(dummyFile)
        }
    }

    @Test
    fun ensureRenderedChapter_rendersWhenNoPreviousRender() = runTest {
        memberDao.insert(fakeMember())
        val repo = createRepoWithCountingRenderer()

        val file = repo.ensureRenderedChapter(1L)
        assertTrue(file.exists())
        assertEquals(1, renderCountFile.readText().trim().toInt())

        val updated = chapterDao.getByMemberId(1L)
        assertTrue(updated!!.renderedPdfPath != null)
        assertEquals(updated.sourceDataHash, updated.renderedPdfHash)
    }

    @Test
    fun ensureRenderedChapter_skipsWhenHashMatchesAndFileExists() = runTest {
        memberDao.insert(fakeMember())
        val tempRepo = createRepoWithCountingRenderer()
        tempRepo.ensureRenderedChapter(1L)

        val chapter = chapterDao.getByMemberId(1L)!!
        val pdfPath = chapter.renderedPdfPath!!
        assertTrue(File(pdfPath).exists())

        assertEquals(1, renderCountFile.readText().trim().toInt())

        val repo2 = createRepoWithCountingRenderer()
        val file = repo2.ensureRenderedChapter(1L)
        assertEquals(pdfPath, file.absolutePath)
        assertEquals(1, renderCountFile.readText().trim().toInt())
    }

    @Test
    fun ensureRenderedChapter_rendersWhenHashMismatches() = runTest {
        memberDao.insert(fakeMember())
        val tempRepo = createRepoWithCountingRenderer()
        tempRepo.ensureRenderedChapter(1L)

        val chapter = chapterDao.getByMemberId(1L)!!
        val pdfPath = chapter.renderedPdfPath!!

        chapterDao.updateRenderedPdf(1L, pdfPath, "old_hash")

        renderCountFile.writeText("0")

        val repo = createRepoWithCountingRenderer()
        repo.ensureRenderedChapter(1L)
        assertEquals(1, renderCountFile.readText().trim().toInt())
    }

    @Test
    fun ensureRenderedChapter_rendersWhenFileMissing() = runTest {
        memberDao.insert(fakeMember())
        val tempRepo = createRepoWithCountingRenderer()
        tempRepo.ensureRenderedChapter(1L)

        val chapter = chapterDao.getByMemberId(1L)!!
        File(chapter.renderedPdfPath!!).delete()

        renderCountFile.writeText("0")

        val repo = createRepoWithCountingRenderer()
        repo.ensureRenderedChapter(1L)
        assertEquals(1, renderCountFile.readText().trim().toInt())
    }

    @Test
    fun ensureRenderedChapter_rendersWhenPathNull() = runTest {
        memberDao.insert(fakeMember())
        chapterDao.upsert(fakeChapter(
            renderedPdfPath = null,
            renderedPdfHash = null
        ))

        val repo = createRepoWithCountingRenderer()
        repo.ensureRenderedChapter(1L)
        assertEquals(1, renderCountFile.readText().trim().toInt())
    }

    private fun createRepository(): ChapterExportRepository {
        val biographyDao = db.biographyDao()
        val sectionDao = db.biographySectionDao()
        val storyDao = db.storyEntryDao()
        return ChapterExportRepository(
            chapterDao = chapterDao,
            memberDao = memberDao,
            biographyDao = biographyDao,
            sectionDao = sectionDao,
            storyDao = storyDao,
            memoryDao = memoryDao
        )
    }

    private fun createRepoWithCountingRenderer(): ChapterRenderCacheRepository {
        return ChapterRenderCacheRepository(
            context = context,
            chapterDao = chapterDao,
            chapterExportRepository = createRepository(),
            memberDao = memberDao,
            memoryDao = memoryDao,
            renderer = countingRenderer()
        )
    }
}
