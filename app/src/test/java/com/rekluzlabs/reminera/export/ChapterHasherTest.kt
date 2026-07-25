package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.BiographyEntity
import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChapterHasherTest {

    private fun fakeMember(
        name: String = "Alice",
        role: String = "Mother",
        birthDate: Long? = 1000000L,
        biography: String = "Loved gardening"
    ) = FamilyMemberEntity(
        id = 1L,
        groupId = 10L,
        name = name,
        role = role,
        biography = biography,
        birthDate = birthDate,
        sortOrder = 0,
        createdAt = 500L
    )

    private fun fakeBiography(
        fullName: String = "Alice Smith",
        relationship: String = "Mother"
    ) = BiographyEntity(
        id = "bio-1",
        personId = 1L,
        fullName = fullName,
        relationship = relationship,
        birthDate = 1000000L,
        familyGroupId = 10L,
        photoUri = null,
        createdAt = 500L,
        updatedAt = 600L
    )

    private fun fakeSection(
        sectionType: String = "early-life",
        fieldsJson: String = """{"text":"Grew up in Ohio"}"""
    ) = BiographySectionEntity(
        id = "sec-1",
        biographyId = "bio-1",
        sectionType = sectionType,
        fieldsJson = fieldsJson,
        updatedAt = 600L
    )

    private fun fakeStory(
        type: String = "audio",
        textContent: String = "I remember the garden...",
        contributedBy: String = "Bob"
    ) = StoryEntryEntity(
        id = "story-1",
        biographyId = "bio-1",
        contributedBy = contributedBy,
        type = type,
        mediaUri = null,
        textContent = textContent,
        recordedAt = 700L,
        createdAt = 700L
    )

    private fun fakeMedia(id: String = "media-1", sortOrder: Int = 0) = MemoryEntryEntity(
        id = id,
        groupId = 10L,
        title = "Photo",
        type = "PHOTO",
        localFilePath = "/tmp/photo.jpg",
        thumbnailPath = null,
        personTag = "Alice",
        notes = null,
        dateCaptured = 800L,
        dateAdded = 800L,
        durationMillis = null,
        isImported = true,
        uploadStatus = "NOT_UPLOADED",
        hostedUrl = null,
        sortOrder = sortOrder
    )

    @Test
    fun hashIsStableForIdenticalInput() {
        val member = fakeMember()
        val bio = fakeBiography()
        val sections = listOf(fakeSection())
        val stories = listOf(fakeStory())
        val media = listOf(fakeMedia())

        val hash1 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media)
        val hash2 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media)

        assertEquals(hash1, hash2)
    }

    @Test
    fun hashChangesWhenMemberNameChanges() {
        val member1 = fakeMember(name = "Alice")
        val member2 = fakeMember(name = "Alice Marie")
        val bio = fakeBiography()
        val sections = listOf(fakeSection())
        val stories = listOf(fakeStory())
        val media = listOf(fakeMedia())

        val hash1 = ChapterHasher.computeSourceHash(member1, bio, sections, stories, media)
        val hash2 = ChapterHasher.computeSourceHash(member2, bio, sections, stories, media)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashChangesWhenSectionContentChanges() {
        val member = fakeMember()
        val bio = fakeBiography()
        val sections1 = listOf(fakeSection(fieldsJson = """{"text":"Old content"}"""))
        val sections2 = listOf(fakeSection(fieldsJson = """{"text":"New content"}"""))
        val stories = listOf(fakeStory())
        val media = listOf(fakeMedia())

        val hash1 = ChapterHasher.computeSourceHash(member, bio, sections1, stories, media)
        val hash2 = ChapterHasher.computeSourceHash(member, bio, sections2, stories, media)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashChangesWhenStoryTextChanges() {
        val member = fakeMember()
        val bio = fakeBiography()
        val sections = listOf(fakeSection())
        val stories1 = listOf(fakeStory(textContent = "Old story"))
        val stories2 = listOf(fakeStory(textContent = "New story"))
        val media = listOf(fakeMedia())

        val hash1 = ChapterHasher.computeSourceHash(member, bio, sections, stories1, media)
        val hash2 = ChapterHasher.computeSourceHash(member, bio, sections, stories2, media)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashChangesWhenMediaReordered() {
        val member = fakeMember()
        val bio = fakeBiography()
        val sections = listOf(fakeSection())
        val stories = listOf(fakeStory())
        val media1 = listOf(fakeMedia(id = "a", sortOrder = 0), fakeMedia(id = "b", sortOrder = 1))
        val media2 = listOf(fakeMedia(id = "b", sortOrder = 0), fakeMedia(id = "a", sortOrder = 1))

        val hash1 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media1)
        val hash2 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashChangesWhenMediaAdded() {
        val member = fakeMember()
        val bio = fakeBiography()
        val sections = listOf(fakeSection())
        val stories = listOf(fakeStory())
        val media1 = listOf(fakeMedia(id = "a"))
        val media2 = listOf(fakeMedia(id = "a"), fakeMedia(id = "b"))

        val hash1 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media1)
        val hash2 = ChapterHasher.computeSourceHash(member, bio, sections, stories, media2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashHandlesNullBiographyGracefully() {
        val member = fakeMember()
        val sections = emptyList<BiographySectionEntity>()
        val stories = emptyList<StoryEntryEntity>()
        val media = emptyList<MemoryEntryEntity>()

        val hash = ChapterHasher.computeSourceHash(member, null, sections, stories, media)
        assertEquals(64, hash.length)
    }

    @Test
    fun hashIsSha256HexFormat() {
        val hash = ChapterHasher.computeSourceHash(
            fakeMember(), fakeBiography(), listOf(fakeSection()),
            listOf(fakeStory()), listOf(fakeMedia())
        )

        assertEquals(64, hash.length)
        assert(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
