package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.BiographyEntity
import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import java.security.MessageDigest

object ChapterHasher {

    private const val DELIMITER = "\u001F"

    fun computeSourceHash(
        member: FamilyMemberEntity,
        biography: BiographyEntity?,
        biographySections: List<BiographySectionEntity>,
        storyEntries: List<StoryEntryEntity>,
        mediaEntries: List<MemoryEntryEntity>
    ): String {
        val sb = StringBuilder()

        sb.append(member.name)
        sb.append(DELIMITER)
        sb.append(member.role)
        sb.append(DELIMITER)
        sb.append(member.birthDate ?: "")
        sb.append(DELIMITER)
        sb.append(member.biography)
        sb.append(DELIMITER)

        if (biography != null) {
            sb.append(biography.fullName)
            sb.append(DELIMITER)
            sb.append(biography.relationship)
            sb.append(DELIMITER)
            sb.append(biography.birthDate ?: "")
        }
        sb.append(DELIMITER)

        for (section in biographySections) {
            sb.append(section.sectionType)
            sb.append(DELIMITER)
            sb.append(section.fieldsJson)
            sb.append(DELIMITER)
        }

        for (story in storyEntries) {
            sb.append(story.type)
            sb.append(DELIMITER)
            sb.append(story.textContent ?: "")
            sb.append(DELIMITER)
            sb.append(story.contributedBy)
            sb.append(DELIMITER)
        }

        for (media in mediaEntries) {
            sb.append(media.id)
            sb.append(DELIMITER)
        }

        val canonical = sb.toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(canonical.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
