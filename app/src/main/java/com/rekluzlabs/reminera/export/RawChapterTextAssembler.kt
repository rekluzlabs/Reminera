package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity

object RawChapterTextAssembler {

    fun assemble(
        member: FamilyMemberEntity,
        sections: List<BiographySectionEntity>,
        stories: List<StoryEntryEntity>
    ): String {
        val blocks = mutableListOf<String>()

        if (member.biography.isNotBlank()) {
            blocks.add(member.biography.trim())
        }

        for (section in sections) {
            val sectionText = extractSectionText(section)
            if (sectionText.isNotBlank()) {
                blocks.add(sectionText)
            }
        }

        for (story in stories) {
            if (story.type == "text" && !story.textContent.isNullOrBlank()) {
                blocks.add(story.textContent.trim())
            }
        }

        return blocks.joinToString("\n\n")
    }

    private fun extractSectionText(section: BiographySectionEntity): String {
        if (section.fieldsJson.isBlank()) return ""
        return try {
            val cleaned = section.fieldsJson.trim().removeSurrounding("{", "}")
            if (cleaned.isBlank()) return ""
            val values = mutableListOf<String>()
            cleaned.split(",").forEach { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) {
                    val value = parts[1].trim().removeSurrounding("\"")
                    if (value.isNotBlank()) {
                        values.add(value)
                    }
                }
            }
            values.joinToString("\n\n")
        } catch (_: Exception) {
            ""
        }
    }
}
