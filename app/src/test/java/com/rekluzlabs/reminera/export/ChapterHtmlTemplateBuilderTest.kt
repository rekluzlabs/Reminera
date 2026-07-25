package com.rekluzlabs.reminera.export

import android.graphics.Bitmap
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChapterHtmlTemplateBuilderTest {

    private lateinit var tempImageFile: File

    @Before
    fun setUp() {
        tempImageFile = File.createTempFile("test_photo", ".jpg")
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        tempImageFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
    }

    @After
    fun tearDown() {
        if (::tempImageFile.isInitialized && tempImageFile.exists()) {
            tempImageFile.delete()
        }
    }

    private fun fakeMember(
        id: Long = 1L,
        groupId: Long = 10L,
        name: String = "Alice Smith",
        role: String = "Mother",
        birthDate: Long? = 946684800000L,
        photoUri: String? = null
    ) = FamilyMemberEntity(
        id = id,
        groupId = groupId,
        name = name,
        role = role,
        biography = "",
        birthDate = birthDate,
        photoUri = photoUri,
        sortOrder = 0,
        createdAt = System.currentTimeMillis()
    )

    private fun fakeChapter(
        memberId: Long = 1L,
        groupId: Long = 10L,
        bioText: String = "First paragraph.\n\nSecond paragraph.",
        mediaManifestJson: String = "[]"
    ) = ChapterExportEntity(
        memberId = memberId,
        groupId = groupId,
        sourceDataHash = "abc123",
        generatedBioText = bioText,
        mediaManifestJson = mediaManifestJson,
        lastGenerated = System.currentTimeMillis()
    )

    private fun fakePhoto(
        id: String = "photo-1",
        groupId: Long = 10L,
        title: String = "Family Photo",
        personTag: String = "Alice Smith",
        sortOrder: Int = 0
    ) = MemoryEntryEntity(
        id = id,
        groupId = groupId,
        title = title,
        type = "PHOTO",
        localFilePath = tempImageFile.absolutePath,
        thumbnailPath = null,
        personTag = personTag,
        notes = null,
        dateCaptured = 946684800000L,
        dateAdded = 946684800000L,
        durationMillis = null,
        isImported = true,
        uploadStatus = "NOT_UPLOADED",
        hostedUrl = null,
        sortOrder = sortOrder
    )

    private fun fakeVideo(
        id: String = "video-1",
        groupId: Long = 10L,
        title: String = "Birthday Video",
        personTag: String = "Alice Smith",
        durationMillis: Long = 125000L,
        sortOrder: Int = 0
    ) = MemoryEntryEntity(
        id = id,
        groupId = groupId,
        title = title,
        type = "VIDEO",
        localFilePath = "/nonexistent/video.mp4",
        thumbnailPath = null,
        personTag = personTag,
        notes = null,
        dateCaptured = 946684800000L,
        dateAdded = 946684800000L,
        durationMillis = durationMillis,
        isImported = true,
        uploadStatus = "NOT_UPLOADED",
        hostedUrl = null,
        sortOrder = sortOrder
    )

    private fun fakeAudio(
        id: String = "audio-1",
        groupId: Long = 10L,
        title: String = "Voice Memo",
        personTag: String = "Alice Smith",
        durationMillis: Long = 65000L,
        sortOrder: Int = 0
    ) = MemoryEntryEntity(
        id = id,
        groupId = groupId,
        title = title,
        type = "AUDIO",
        localFilePath = "/nonexistent/audio.m4a",
        thumbnailPath = null,
        personTag = personTag,
        notes = null,
        dateCaptured = 946684800000L,
        dateAdded = 946684800000L,
        durationMillis = durationMillis,
        isImported = true,
        uploadStatus = "NOT_UPLOADED",
        hostedUrl = null,
        sortOrder = sortOrder
    )

    @Test
    fun htmlContainsMemberName() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(name = "Alice Smith"),
                mediaEntries = emptyList()
            )
        )
        assertTrue(html.contains("Alice Smith"))
        assertTrue(html.contains("<h1>"))
    }

    @Test
    fun htmlContainsRoleAndBirthYear() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(role = "Mother", birthDate = 867734400000L),
                mediaEntries = emptyList()
            )
        )
        assertTrue(html.contains("Mother"))
        assertTrue(html.contains("Born 1997"))
    }

    @Test
    fun bioTextSplitIntoParagraphs() {
        val bioText = "First paragraph about Alice.\n\nSecond paragraph about her life.\n\nThird paragraph."
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(bioText = bioText),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        val pTagCount = html.split("<p>").size - 1
        assertEquals(3, pTagCount)
    }

    @Test
    fun singleParagraphBioStillRenders() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(bioText = "Just one paragraph."),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        assertTrue(html.contains("<p>"))
        assertTrue(html.contains("Just one paragraph."))
    }

    @Test
    fun emptyBioTextProducesNoBiographySection() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(bioText = ""),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        assertFalse(html.contains("Biography</div>"))
    }

    @Test
    fun photoCountMatchesHtmlTiles() {
        val photos = listOf(fakePhoto(id = "p1"), fakePhoto(id = "p2"), fakePhoto(id = "p3"))
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = photos
            )
        )
        assertTrue(html.contains("Photographs"))
        val tileCount = html.split("class=\"photo-tile\"").size - 1
        assertEquals(3, tileCount)
    }

    @Test
    fun videoAudioCountMatchesPlaceholderBlocks() {
        val media = listOf(fakeVideo(id = "v1"), fakeAudio(id = "a1"))
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = media
            )
        )
        assertTrue(html.contains("Recordings"))
        val placeholderCount = html.split("class=\"media-placeholder\"").size - 1
        assertEquals(2, placeholderCount)
    }

    @Test
    fun qrPlaceholderPresentForVideoAndAudio() {
        val media = listOf(fakeVideo(), fakeAudio())
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = media
            )
        )
        assertTrue(html.contains("QR code"))
        assertTrue(html.contains("coming in a"))
        assertTrue(html.contains("later step"))
    }

    @Test
    fun noPhotosNoPhotoSection() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = listOf(fakeVideo())
            )
        )
        assertFalse(html.contains("Photographs"))
    }

    @Test
    fun noAudioVideoNoRecordingsSection() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = listOf(fakePhoto())
            )
        )
        assertFalse(html.contains("Recordings"))
    }

    @Test
    fun htmlIsSelfContainedNoExternalReferences() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        assertFalse(html.contains("href="))
        assertFalse(html.contains("src=\"http"))
        assertFalse(html.contains("src=\"https"))
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains(":root"))
    }

    @Test
    fun cssVariablesPresent() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        assertTrue(html.contains("--page-margin"))
        assertTrue(html.contains("--heading-font"))
        assertTrue(html.contains("--body-font"))
        assertTrue(html.contains("--accent-color"))
        assertTrue(html.contains("--text-color"))
    }

    @Test
    fun mixedMediaCountsMatch() {
        val media = listOf(
            fakePhoto(id = "p1"),
            fakePhoto(id = "p2"),
            fakeVideo(id = "v1"),
            fakeAudio(id = "a1")
        )
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = media
            )
        )
        val photoTiles = html.split("class=\"photo-tile\"").size - 1
        val mediaPlaceholders = html.split("class=\"media-placeholder\"").size - 1
        assertEquals(2, photoTiles)
        assertEquals(2, mediaPlaceholders)
    }

    @Test
    fun footerContainsAppBranding() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = emptyList()
            )
        )
        assertTrue(html.contains("Reminera"))
        assertTrue(html.contains("Preserve your family"))
    }

    @Test
    fun breakInsideAvoidOnPhotoTiles() {
        val html = ChapterHtmlTemplateBuilder.buildHtml(
            ChapterHtmlTemplateBuilder.ChapterInput(
                chapter = fakeChapter(),
                member = fakeMember(),
                mediaEntries = listOf(fakePhoto())
            )
        )
        assertTrue(html.contains("break-inside: avoid"))
        assertTrue(html.contains("page-break-inside: avoid"))
    }
}
