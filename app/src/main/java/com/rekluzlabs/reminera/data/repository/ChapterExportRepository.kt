package com.rekluzlabs.reminera.data.repository

import com.rekluzlabs.reminera.data.BiographyDao
import com.rekluzlabs.reminera.data.BiographySectionDao
import com.rekluzlabs.reminera.data.ChapterExportDao
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.MemoryEntryDao
import com.rekluzlabs.reminera.data.StoryEntryDao
import com.rekluzlabs.reminera.export.BiographyGenerationInput
import com.rekluzlabs.reminera.export.BiographyGenerationProvider
import com.rekluzlabs.reminera.export.ChapterHasher
import com.rekluzlabs.reminera.export.RawChapterTextAssembler
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

        val rawText = RawChapterTextAssembler.assemble(member, sections, stories)

        val chapter = ChapterExportEntity(
            memberId = memberId,
            groupId = member.groupId,
            sourceDataHash = newHash,
            generatedBioText = rawText,
            mediaManifestJson = mediaManifest.toString(),
            lastGenerated = System.currentTimeMillis(),
            biographySource = "RAW",
            aiPolishedAt = null
        )

        chapterDao.upsert(chapter)
        return chapter
    }

    internal suspend fun buildBiographyInput(memberId: Long): BiographyGenerationInput {
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

        val sectionPairs = sections.map { section ->
            val title = section.sectionType.replaceFirstChar { it.uppercase() }
            val content = try {
                val json = org.json.JSONObject(section.fieldsJson)
                val values = json.keys().asSequence().map { json.optString(it, "") }.filter { it.isNotBlank() }.toList()
                values.joinToString(" ")
            } catch (_: Exception) { "" }
            title to content
        }

        val storyTexts = stories
            .filter { !it.textContent.isNullOrBlank() }
            .map { it.textContent!!.trim() }

        return BiographyGenerationInput(
            name = member.name,
            relationship = member.role,
            dateOfBirth = member.birthDate,
            biographyText = member.biography,
            sections = sectionPairs,
            stories = storyTexts
        )
    }

    suspend fun requestAiPolish(
        memberId: Long,
        provider: BiographyGenerationProvider
    ): Result<Unit> {
        val existing = chapterDao.getByMemberId(memberId)
            ?: return Result.failure(IllegalStateException("No chapter for member $memberId"))

        return try {
            val input = buildBiographyInput(memberId)
            val result = provider.generateBiography(input)
            result.fold(
                onSuccess = { polishedText ->
                    val newHash = existing.sourceDataHash
                    val now = System.currentTimeMillis()
                    chapterDao.updateAiPolished(
                        memberId = memberId,
                        text = polishedText,
                        source = "AI_POLISHED",
                        hash = newHash,
                        aiPolishedAt = now,
                        lastGenerated = now
                    )
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
