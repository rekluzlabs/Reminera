package com.rekluzlabs.reminera.export

import org.junit.Assert.assertEquals
import org.junit.Test

class BookAssemblerTest {

    @Test
    fun computePageOffsets_singleChapterOnePageToc() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(5),
            tocPageCount = 1
        )
        assertEquals(listOf(1), offsets)
    }

    @Test
    fun computePageOffsets_multipleChaptersOnePageToc() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(3, 2, 4),
            tocPageCount = 1
        )
        assertEquals(listOf(1, 4, 6), offsets)
    }

    @Test
    fun computePageOffsets_singleChapterTwoPageToc() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(5),
            tocPageCount = 2
        )
        assertEquals(listOf(2), offsets)
    }

    @Test
    fun computePageOffsets_multipleChaptersTwoPageToc() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(3, 2, 4),
            tocPageCount = 2
        )
        assertEquals(listOf(2, 5, 7), offsets)
    }

    @Test
    fun computePageOffsets_emptyChapters() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = emptyList(),
            tocPageCount = 1
        )
        assertEquals(emptyList<Int>(), offsets)
    }

    @Test
    fun computePageOffsets_singlePageChapters() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(1, 1, 1, 1),
            tocPageCount = 1
        )
        assertEquals(listOf(1, 2, 3, 4), offsets)
    }

    @Test
    fun computePageOffsets_largeBook() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(10, 15, 8, 12, 6),
            tocPageCount = 1
        )
        assertEquals(listOf(1, 11, 26, 34, 46), offsets)
    }

    @Test
    fun computePageOffsets_tocPageCountZero() {
        val offsets = BookAssembler.computePageOffsets(
            chapterPageCounts = listOf(3, 2),
            tocPageCount = 0
        )
        assertEquals(listOf(0, 3), offsets)
    }

    @Test
    fun computePageOffsets_convergenceScenario_tocFitsOnePage() {
        val chapterPageCounts = listOf(3, 2, 4)
        val offsetsPass1 = BookAssembler.computePageOffsets(chapterPageCounts, 1)
        assertEquals(listOf(1, 4, 6), offsetsPass1)

        val offsetsPass2 = BookAssembler.computePageOffsets(chapterPageCounts, 1)
        assertEquals(offsetsPass1, offsetsPass2)
    }

    @Test
    fun computePageOffsets_convergenceScenario_tocNeedsTwoPages() {
        val chapterPageCounts = listOf(3, 2, 4)
        val offsetsPass1 = BookAssembler.computePageOffsets(chapterPageCounts, 1)
        assertEquals(listOf(1, 4, 6), offsetsPass1)

        val offsetsPass2 = BookAssembler.computePageOffsets(chapterPageCounts, 2)
        assertEquals(listOf(2, 5, 7), offsetsPass2)

        val offsetsPass3 = BookAssembler.computePageOffsets(chapterPageCounts, 2)
        assertEquals(offsetsPass2, offsetsPass3)
    }

    @Test
    fun computePageOffsets_manyChaptersManyPages() {
        val chapterPageCounts = listOf(20, 20, 20, 20, 20, 20, 20, 20, 20, 20)
        val offsets = BookAssembler.computePageOffsets(chapterPageCounts, 1)
        assertEquals(listOf(1, 21, 41, 61, 81, 101, 121, 141, 161, 181), offsets)
    }
}
