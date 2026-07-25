package com.rekluzlabs.reminera.data.repository

import com.rekluzlabs.reminera.data.BiographyDao
import com.rekluzlabs.reminera.data.BiographySectionDao
import com.rekluzlabs.reminera.data.ChapterExportDao
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.MemoryEntryDao
import com.rekluzlabs.reminera.data.StoryEntryDao
import com.rekluzlabs.reminera.export.ChapterHasher
import org.json.JSONArray

class ChapterExportRepository(
    private val chapterDao: ChapterExportDao,
    private val memberDao: FamilyMemberDao,
    private val biographyDao: BiographyDao,
    private val sectionDao: BiographySectionDao,
    private val storyDao: StoryEntryDao,
    private val memoryDao: MemoryEntryDao
) {

    suspend fun getOrGenerateChapter(memberId: Long): ChapterExportEntity {
        val member = memberDao.getMemberById(memberId)
            ?: throw IllegalArgumentException("Member $memberId not found")

        val biography = biographyDao.getByPersonIdOnce(memberId)
        val sections = if (biography != null) {
            sectionDao.getByBiographyIdOnce(biography.id)
        } else {
            emptyList()
        }
        val stories = if (biography != null) {
            storyDao.getByBiographyIdOnce(biography.id)
        } else {
            emptyList()
        }
        val media = memoryDao.getEntriesByGroupIdAndPersonTagList(member.groupId, member.name)

        val newHash = ChapterHasher.computeSourceHash(member, biography, sections, stories, media)

        val existing = chapterDao.getByMemberId(memberId)
        if (existing != null && existing.sourceDataHash == newHash) {
            return existing
        }

        val mediaIds = media.map { it.id }
        val mediaManifest = JSONArray().apply { mediaIds.forEach { put(it) } }

        // TODO(step-4): replace with BYOK-generated biography text
        val placeholder = buildString {
            append("[Placeholder biography for ${member.name}, ${member.role}. ")
            if (member.birthDate != null) {
                append("Born ${member.birthDate}. ")
            }
            append("${sections.size} biography section(s), ")
            append("${stories.size} story entr${if (stories.size == 1) "y" else "ies"}, ")
            append("${media.size} media item(s) on file. ")
            append("Generated ${System.currentTimeMillis()}.]")
        }

        val chapter = ChapterExportEntity(
            memberId = memberId,
            groupId = member.groupId,
            sourceDataHash = newHash,
            generatedBioText = placeholder,
            mediaManifestJson = mediaManifest.toString(),
            lastGenerated = System.currentTimeMillis()
        )

        chapterDao.upsert(chapter)
        return chapter
    }
}
