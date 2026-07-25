package com.rekluzlabs.reminera.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiBiographyProviderTest {

    private val provider = GeminiBiographyProvider("test-key")

    @Test
    fun buildPrompt_containsAllInputSections() {
        val input = BiographyGenerationInput(
            name = "Alice",
            relationship = "Mother",
            dateOfBirth = 867734400000L,
            biographyText = "Born in Ohio.",
            sections = listOf("Origins" to "Grew up in a farming community."),
            stories = listOf("My first memory is of the old oak tree.")
        )
        val prompt = provider.buildPrompt(input)
        assertTrue(prompt.contains("Alice"))
        assertTrue(prompt.contains("Mother"))
        assertTrue(prompt.contains("Born in Ohio."))
        assertTrue(prompt.contains("Grew up in a farming community."))
        assertTrue(prompt.contains("My first memory"))
        assertTrue(prompt.contains("plain prose paragraphs"))
        assertTrue(prompt.contains("no markdown"))
    }

    @Test
    fun buildPrompt_emptyMaterial_stillFormed() {
        val input = BiographyGenerationInput(
            name = "Bob",
            relationship = "Father",
            dateOfBirth = null,
            biographyText = "",
            sections = emptyList(),
            stories = emptyList()
        )
        val prompt = provider.buildPrompt(input)
        assertTrue(prompt.contains("Bob"))
        assertTrue(prompt.contains("Father"))
        assertTrue(prompt.contains("--- Material ---"))
    }

    @Test
    fun truncateInput_underCap_returnsUnchanged() {
        val input = BiographyGenerationInput(
            name = "Alice",
            relationship = "Mother",
            dateOfBirth = null,
            biographyText = "Short bio.",
            sections = listOf("Origins" to "Short."),
            stories = emptyList()
        )
        val truncated = provider.truncateInput(input)
        assertEquals("Short bio.", truncated.biographyText)
        assertEquals(1, truncated.sections.size)
    }

    @Test
    fun truncateInput_overCap_truncatesAndClearsSections() {
        val longText = "A".repeat(25_000)
        val input = BiographyGenerationInput(
            name = "Alice",
            relationship = "Mother",
            dateOfBirth = null,
            biographyText = longText,
            sections = listOf("Origins" to "Section text."),
            stories = listOf("Story text.")
        )
        val truncated = provider.truncateInput(input)
        assertTrue(truncated.biographyText.length <= GeminiBiographyProvider.INPUT_LENGTH_CAP + 100)
        assertTrue(truncated.biographyText.contains("[Content truncated"))
        assertTrue(truncated.sections.isEmpty())
        assertTrue(truncated.stories.isEmpty())
    }

    @Test
    fun truncateInput_boundaryExactlyAtCap_notTruncated() {
        val text = "A".repeat(GeminiBiographyProvider.INPUT_LENGTH_CAP)
        val input = BiographyGenerationInput(
            name = "Alice",
            relationship = "Mother",
            dateOfBirth = null,
            biographyText = text,
            sections = emptyList(),
            stories = emptyList()
        )
        val truncated = provider.truncateInput(input)
        assertEquals(text, truncated.biographyText)
    }

    @Test
    fun buildPrompt_doesNotContainMarkdownInstructions() {
        val input = BiographyGenerationInput(
            name = "Alice",
            relationship = "Mother",
            dateOfBirth = null,
            biographyText = "Hello.",
            sections = emptyList(),
            stories = emptyList()
        )
        val prompt = provider.buildPrompt(input)
        assertFalse(prompt.contains("**"))
        assertFalse(prompt.contains("# "))
        assertTrue(prompt.contains("No headings"))
    }
}
