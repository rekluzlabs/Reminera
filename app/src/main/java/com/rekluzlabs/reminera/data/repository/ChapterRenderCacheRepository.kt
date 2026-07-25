package com.rekluzlabs.reminera.data.repository

import android.content.Context
import com.rekluzlabs.reminera.data.ChapterExportDao
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.export.ChapterHtmlTemplateBuilder
import com.rekluzlabs.reminera.export.ChapterPdfRenderer
import com.rekluzlabs.reminera.export.ChapterPdfRenderer.RenderResult
import com.rekluzlabs.reminera.data.BiographyDao
import com.rekluzlabs.reminera.data.BiographySectionDao
import com.rekluzlabs.reminera.data.MemoryEntryDao
import com.rekluzlabs.reminera.data.StoryEntryDao
import org.json.JSONArray
import java.io.File

class ChapterRenderCacheRepository(
    private val context: Context,
    private val chapterDao: ChapterExportDao,
    private val chapterExportRepository: ChapterExportRepository,
    private val memberDao: FamilyMemberDao,
    private val memoryDao: MemoryEntryDao
) {

    private val exportDir: File
        get() = File(context.filesDir, "exports/chapters").also { it.mkdirs() }

    suspend fun ensureRenderedChapter(memberId: Long): File {
        val chapter = chapterExportRepository.getOrGenerateChapter(memberId)

        if (chapter.renderedPdfPath != null && chapter.renderedPdfHash != null) {
            val existingFile = File(chapter.renderedPdfPath)
            if (existingFile.exists() && existingFile.length() > 0 &&
                chapter.renderedPdfHash == chapter.sourceDataHash
            ) {
                return existingFile
            }
        }

        val member = memberDao.getMemberById(memberId)
            ?: throw IllegalArgumentException("Member $memberId not found")

        val mediaEntries = memoryDao.getEntriesByGroupIdAndPersonTagList(member.groupId, member.name)

        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = chapter,
                member = member,
                mediaEntries = mediaEntries
            )
        )

        val outputFile = File(exportDir, "chapter_${memberId}.pdf")

        val result = ChapterPdfRenderer.renderChapter(
            context = context,
            memberId = memberId,
            html = html,
            chapterTitle = member.name
        )

        when (result) {
            is RenderResult.Success -> {
                if (result.outputFile != outputFile) {
                    result.outputFile.copyTo(outputFile, overwrite = true)
                }
            }
            is RenderResult.Failure -> {
                throw RuntimeException("PDF rendering failed: ${result.error}")
            }
        }

        chapterDao.updateRenderedPdf(
            memberId = memberId,
            path = outputFile.absolutePath,
            hash = chapter.sourceDataHash
        )

        return outputFile
    }
}
