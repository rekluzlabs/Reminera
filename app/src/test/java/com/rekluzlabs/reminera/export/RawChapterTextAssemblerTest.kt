package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RawChapterTextAssemblerTest {

    private fun member(
        name: String = "Alice",
        role: String = "Mother",
        biography: String = ""
    ) = FamilyMemberEntity(
        id = 1L,
        groupId = 10L,
        name = name,
        role = role,
        biography = biography,
        birthDate = null,
        sortOrder = 0,
        createdAt = System.currentTimeMillis()
    )

    private fun section(
        id: String = "sec-1",
        biographyId: String = "bio-1",
        sectionType: String = "origins",
        fieldsJson: String = """{"birthplace":"Ohio","heritage":"Irish"}"""
    ) = BiographySectionEntity(
        id = id,
        biographyId = biographyId,
        sectionType = sectionType,
        fieldsJson = fieldsJson,
        updatedAt = System.currentTimeMillis()
    )

    private fun textStory(
        id: String = "story-1",
        biographyId: String = "bio-1",
        textContent: String = "A story"
    ) = StoryEntryEntity(
        id = id,
        biographyId = biographyId,
        contributedBy = "You",
        type = "text",
        mediaUri = null,
        textContent = textContent,
        recordedAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )

    private fun mediaStory(
        id: String = "story-m",
        biographyId: String = "bio-1",
        type: String = "photo"
    ) = StoryEntryEntity(
        id = id,
        biographyId = biographyId,
        contributedBy = "You",
        type = type,
        mediaUri = "/tmp/photo.jpg",
        textContent = null,
        recordedAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun memberBiographyAppearsFirst() {
        val result = RawChapterTextAssembler.assemble(
            member = member(biography = "I was born in 1950."),
            sections = listOf(section(fieldsJson = """{"birthplace":"Ohio"}""")),
            stories = emptyList()
        )
        val blocks = result.split("\n\n")
        assertEquals("I was born in 1950.", blocks[0])
    }

    @Test
    fun sectionsFollowMemberBiography() {
        val result = RawChapterTextAssembler.assemble(
            member = member(biography = "Bio text"),
            sections = listOf(
                section(sectionType = "origins", fieldsJson = """{"birthplace":"Ohio"}"""),
                section(sectionType = "milestones", id = "sec-2", fieldsJson = """{"education":"OSU"}""")
            ),
            stories = emptyList()
        )
        val blocks = result.split("\n\n")
        assertEquals(3, blocks.size)
        assertEquals("Bio text", blocks[0])
        assertEquals("Ohio", blocks[1])
        assertEquals("OSU", blocks[2])
    }

    @Test
    fun textStoriesFollowSections() {
        val result = RawChapterTextAssembler.assemble(
            member = member(),
            sections = listOf(section(fieldsJson = """{"birthplace":"Ohio"}""")),
            stories = listOf(textStory(textContent = "My favorite memory."))
        )
        val blocks = result.split("\n\n")
        assertEquals(2, blocks.size)
        assertEquals("My favorite memory.", blocks[1])
    }

    @Test
    fun emptyBiographyTextSkipped() {
        val result = RawChapterTextAssembler.assemble(
            member = member(biography = ""),
            sections = listOf(section(fieldsJson = """{"birthplace":"Ohio"}""")),
            stories = emptyList()
        )
        val blocks = result.split("\n\n")
        assertEquals(1, blocks.size)
        assertEquals("Ohio", blocks[0])
    }

    @Test
    fun emptySectionsAndStories_producesEmptyOutput() {
        val result = RawChapterTextAssembler.assemble(
            member = member(biography = ""),
            sections = emptyList(),
            stories = emptyList()
        )
        assertEquals("", result)
    }

    @Test
    fun blankFieldsJsonSectionSkipped() {
        val result = RawChapterTextAssembler.assemble(
            member = member(),
            sections = listOf(section(fieldsJson = "")),
            stories = emptyList()
        )
        assertEquals("", result)
    }

    @Test
    fun nonTextStoriesSkipped() {
        val result = RawChapterTextAssembler.assemble(
            member = member(),
            sections = emptyList(),
            stories = listOf(
                mediaStory(type = "photo"),
                mediaStory(type = "video", id = "story-v"),
                mediaStory(type = "audio", id = "story-a")
            )
        )
        assertEquals("", result)
    }

    @Test
    fun textStoryWithBlankContentSkipped() {
        val result = RawChapterTextAssembler.assemble(
            member = member(),
            sections = emptyList(),
            stories = listOf(textStory(textContent = "   "))
        )
        assertEquals("", result)
    }

    @Test
    fun multipleSectionFieldsConcatenated() {
        val result = RawChapterTextAssembler.assemble(
            member = member(),
            sections = listOf(section(fieldsJson = """{"birthplace":"Ohio","heritage":"Irish"}""")),
            stories = emptyList()
        )
        assertTrue(result.contains("Ohio"))
        assertTrue(result.contains("Irish"))
        assertTrue(result.contains("\n\n"))
    }

    @Test
    fun whitespaceIsTrimmed() {
        val result = RawChapterTextAssembler.assemble(
            member = member(biography = "  Hello world  "),
            sections = emptyList(),
            stories = emptyList()
        )
        assertEquals("Hello world", result)
    }
}
