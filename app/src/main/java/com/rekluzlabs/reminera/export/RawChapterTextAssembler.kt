package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import org.json.JSONObject

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
            if (!story.textContent.isNullOrBlank()) {
                blocks.add(story.textContent.trim())
            }
        }

        return blocks.joinToString("\n\n")
    }

    private fun extractSectionText(section: BiographySectionEntity): String {
        if (section.fieldsJson.isBlank()) return ""
        return try {
            val json = JSONObject(section.fieldsJson)
            val values = json.keys().asSequence().map { json.optString(it, "") }.filter { it.isNotBlank() }.toList()
            values.joinToString("\n\n")
        } catch (_: Exception) {
            ""
        }
    }
}
