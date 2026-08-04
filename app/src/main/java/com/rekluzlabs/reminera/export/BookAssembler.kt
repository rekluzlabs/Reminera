package com.rekluzlabs.reminera.export

import android.content.Context
import com.rekluzlabs.reminera.data.BookExportManifestDao
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.repository.BookExportManifestRepository
import com.rekluzlabs.reminera.data.repository.ChapterRenderCacheRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

object BookAssembler {

    sealed class AssembleResult {
        data class Success(val outputFile: File) : AssembleResult()
        data class Failure(val error: String) : AssembleResult()
    }

    private const val MAX_TOC_ITERATIONS = 3
    private const val PAGE_NUMBER_FORMAT = "%d"

    suspend fun assembleBook(
        context: Context,
        manifestId: Long,
        manifestDao: BookExportManifestDao,
        memberDao: FamilyMemberDao,
        renderCacheRepository: ChapterRenderCacheRepository,
        onProgress: ((current: Int, total: Int, stage: ExportStage, elapsed: Long?) -> Unit)? = null
    ): AssembleResult = withContext(Dispatchers.Main) {
        val exportDir = File(context.filesDir, "exports/books").also { it.mkdirs() }
        var mergedDoc: PDDocument? = null
        val chapterDocs = mutableListOf<PDDocument>()

        try {
            val manifest = manifestDao.getById(manifestId)
                ?: return@withContext AssembleResult.Failure("Manifest $manifestId not found")

            val memberIds = parseMemberOrder(manifest.memberOrderJson)
            if (memberIds.isEmpty()) {
                return@withContext AssembleResult.Failure("Manifest has no members")
            }

            val chapterFiles = mutableListOf<File>()
            for ((index, memberId) in memberIds.withIndex()) {
                val start = System.currentTimeMillis()
                onProgress?.invoke(index, memberIds.size, ExportStage.RENDERING, null)
                
                val file = renderCacheRepository.ensureRenderedChapter(memberId)
                chapterFiles.add(file)
                
                val elapsed = System.currentTimeMillis() - start
                onProgress?.invoke(index + 1, memberIds.size, ExportStage.RENDERING, elapsed)
            }

            onProgress?.invoke(0, 1, ExportStage.ASSEMBLING, null)

            val chapterPageCounts = mutableListOf<Int>()
            for (file in chapterFiles) {
                val doc = PDDocument.load(file)
                chapterDocs.add(doc)
                chapterPageCounts.add(doc.numberOfPages)
            }

            val memberNames = mutableListOf<String>()
            val memberRoles = mutableListOf<String>()
            for (memberId in memberIds) {
                val member = memberDao.getMemberById(memberId)
                memberNames.add(member?.name ?: "Unknown")
                memberRoles.add(member?.role ?: "Member")
            }

            var tocPageCount = 1
            var finalOffsets = computePageOffsets(chapterPageCounts, tocPageCount)

            var tocDoc: PDDocument? = null
            try {
                for (iteration in 0 until MAX_TOC_ITERATIONS) {
                    val tocEntries = buildTocEntries(memberNames, memberRoles, finalOffsets)
                    val tocHtml = TocHtmlTemplateBuilder.buildHtml(
                        bookTitle = manifest.title.ifBlank { "Family Book" },
                        entries = tocEntries
                    )

                    val tocResult = ChapterPdfRenderer.renderChapter(
                        context = context,
                        memberId = -1L,
                        html = tocHtml,
                        chapterTitle = "Table of Contents"
                    )

                    if (tocResult is ChapterPdfRenderer.RenderResult.Failure) {
                        return@withContext AssembleResult.Failure("TOC rendering failed: ${tocResult.error}")
                    }

                    val tocFile = (tocResult as ChapterPdfRenderer.RenderResult.Success).outputFile
                    val newTocDoc = PDDocument.load(tocFile)
                    val newTocPageCount = newTocDoc.numberOfPages

                    if (newTocPageCount == tocPageCount) {
                        tocDoc?.close()
                        tocDoc = newTocDoc
                        break
                    }

                    tocDoc?.close()
                    tocDoc = newTocDoc
                    tocPageCount = newTocPageCount
                    finalOffsets = computePageOffsets(chapterPageCounts, tocPageCount)
                }
            } catch (e: Exception) {
                return@withContext AssembleResult.Failure("TOC generation failed: ${e.message}")
            }

            mergedDoc = PDDocument()

            if (tocDoc != null) {
                for (i in 0 until tocDoc.numberOfPages) {
                    mergedDoc.importPage(tocDoc.getPage(i))
                }
            }

            val chapterStartPages = mutableListOf<Int>()
            var currentPage = (tocDoc?.numberOfPages ?: 1)
            for (i in chapterDocs.indices) {
                chapterStartPages.add(currentPage)
                val doc = chapterDocs[i]
                for (j in 0 until doc.numberOfPages) {
                    mergedDoc.importPage(doc.getPage(j))
                }
                currentPage += doc.numberOfPages
            }

            val outline = PDDocumentOutline()
            mergedDoc.documentCatalog.documentOutline = outline
            for (i in memberIds.indices) {
                val item = PDOutlineItem()
                item.title = memberNames[i]
                item.destination = PDPageFitWidthDestination()
                (item.destination as PDPageFitWidthDestination).page = mergedDoc.getPage(chapterStartPages[i])
                outline.addLast(item)
            }

            val totalPages = mergedDoc.numberOfPages
            for (i in 0 until totalPages) {
                val page = mergedDoc.getPage(i)
                val mediaBox = page.mediaBox ?: PDRectangle.A4
                val pageWidth = mediaBox.width
                val pageHeight = mediaBox.height

                val text = PAGE_NUMBER_FORMAT.format(i + 1)
                val font = PDType1Font.HELVETICA
                val fontSize = 10f
                val textWidth = font.getStringWidth(text) / 1000 * fontSize

                val x = (pageWidth - textWidth) / 2
                val y = 24f

                val contentStream = PDPageContentStream(mergedDoc, page, PDPageContentStream.AppendMode.APPEND, true)
                contentStream.beginText()
                contentStream.setFont(font, fontSize)
                contentStream.newLineAtOffset(x, y)
                contentStream.showText(text)
                contentStream.endText()
                contentStream.close()
            }

            val slug = slugify(manifest.title.ifBlank { "book" })
            val outputFile = File(exportDir, "${manifestId}_${slug}.pdf")
            mergedDoc.save(outputFile)

            onProgress?.invoke(1, 1, ExportStage.ASSEMBLING, null)
            AssembleResult.Success(outputFile)
        } catch (e: Exception) {
            AssembleResult.Failure(e.message ?: "Unknown assembly error")
        } finally {
            mergedDoc?.close()
            chapterDocs.forEach { it.close() }
        }
    }

    internal fun computePageOffsets(
        chapterPageCounts: List<Int>,
        tocPageCount: Int
    ): List<Int> {
        val offsets = mutableListOf<Int>()
        var current = tocPageCount
        for (count in chapterPageCounts) {
            offsets.add(current)
            current += count
        }
        return offsets
    }

    private fun buildTocEntries(
        names: List<String>,
        roles: List<String>,
        offsets: List<Int>
    ): List<TocHtmlTemplateBuilder.TocEntry> {
        return names.indices.map { i ->
            TocHtmlTemplateBuilder.TocEntry(
                memberName = names[i],
                role = roles[i],
                startPage = offsets[i] + 1
            )
        }
    }

    private fun parseMemberOrder(json: String): List<Long> {
        if (json.isBlank() || json == "[]") return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getLong(it) }
    }

    private fun slugify(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[/\\\\:*?\"<>|\\s]+"), "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "book" }
    }
}
