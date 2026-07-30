package com.rekluzlabs.reminera.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GeminiBiographyProvider(
    private val apiKey: String,
    private val modelId: String = "gemini-2.0-flash-lite"
) : BiographyGenerationProvider {

    companion object {
        internal const val INPUT_LENGTH_CAP = 100_000
        internal const val OUTPUT_TOKEN_CAP = 16_384
        private const val API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    override suspend fun generateBiography(input: BiographyGenerationInput): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(input)
                val responseText = callGeminiApi(prompt)
                Result.success(responseText)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    internal fun buildInput(
        name: String,
        relationship: String,
        dateOfBirth: Long?,
        biographyText: String,
        sections: List<Pair<String, String>>,
        stories: List<String>
    ): BiographyGenerationInput {
        return BiographyGenerationInput(
            name = name,
            relationship = relationship,
            dateOfBirth = dateOfBirth,
            biographyText = biographyText,
            sections = sections,
            stories = stories
        )
    }

    internal fun truncateInput(input: BiographyGenerationInput): BiographyGenerationInput {
        val headerLen = buildString {
            append("Name: ${input.name}\nRelationship: ${input.relationship}\n")
            if (input.dateOfBirth != null) append("Date of birth: ${input.dateOfBirth}\n")
        }.length

        val materialLen = buildString {
            if (input.biographyText.isNotBlank()) append(input.biographyText)
            for ((_, content) in input.sections) {
                if (content.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(content)
                }
            }
            for (story in input.stories) {
                if (story.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(story)
                }
            }
        }.length

        if (headerLen + materialLen <= INPUT_LENGTH_CAP) return input

        val overheadPerSection = 12
        val overheadPerStory = 4

        var budget = INPUT_LENGTH_CAP - headerLen - "\n\n[Some material omitted due to length limits]".length

        val keptBiography = if (input.biographyText.length <= budget) {
            budget -= input.biographyText.length
            input.biographyText
        } else {
            val truncated = input.biographyText.take(budget)
            budget = 0
            truncated + "\n[Biography truncated]"
        }

        val keptSections = mutableListOf<Pair<String, String>>()
        for ((title, content) in input.sections) {
            if (content.isBlank()) continue
            val needed = overheadPerSection + content.length
            if (needed <= budget) {
                keptSections.add(title to content)
                budget -= needed
            }
        }

        val keptStories = mutableListOf<String>()
        for (story in input.stories) {
            val needed = overheadPerStory + story.length
            if (needed <= budget) {
                keptStories.add(story)
                budget -= needed
            }
        }

        return input.copy(
            biographyText = keptBiography,
            sections = keptSections,
            stories = keptStories
        )
    }

    internal fun buildPrompt(input: BiographyGenerationInput): String {
        val truncated = truncateInput(input)

        val material = buildString {
            append("Name: ${truncated.name}\n")
            append("Relationship: ${truncated.relationship}\n")
            if (truncated.dateOfBirth != null) {
                append("Date of birth: ${truncated.dateOfBirth}\n")
            }
            append("\n--- Biography Material ---\n\n")

            if (truncated.biographyText.isNotBlank()) {
                append(truncated.biographyText)
                append("\n\n")
            }

            for ((title, content) in truncated.sections) {
                if (content.isNotBlank()) {
                    append("[$title]\n$content\n\n")
                }
            }

            for (story in truncated.stories) {
                if (story.isNotBlank()) {
                    append(story)
                    append("\n\n")
                }
            }
        }

        return """You are a professional biographer. Rewrite the following family biography material into polished, plain prose paragraphs suitable for a printed book chapter.

Rules:
- Output ONLY flowing prose paragraphs. No headings, no bullet points, no markdown formatting.
- Preserve all factual details exactly as provided — do not invent or embellish facts.
- Organize naturally: opening about the person, life events in logical order, closing reflections.
- Use a warm, respectful tone appropriate for a family keepsake book.
- Be comprehensive and detailed. Use ALL the provided material. Write a full-length biography chapter, not a short summary.
- If the material is sparse, work with what is provided — do not pad with filler.

--- Material ---
$material
--- End Material ---"""
    }

    private fun callGeminiApi(prompt: String): String {
        val url = URL("${API_BASE_URL}${modelId}:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", OUTPUT_TOKEN_CAP)
                    put("temperature", 0.7)
                })
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw GeminiApiException("API returned HTTP $responseCode: $errorBody")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val candidates = json.optJSONArray("candidates")
                ?: throw GeminiApiException("No candidates in response")
            if (candidates.length() == 0) {
                throw GeminiApiException("Empty candidates array")
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
                ?: throw GeminiApiException("No content in candidate")
            val parts = content.optJSONArray("parts")
                ?: throw GeminiApiException("No parts in content")
            if (parts.length() == 0) {
                throw GeminiApiException("Empty parts array")
            }

            parts.getJSONObject(0).optString("text", "")
                .ifBlank { throw GeminiApiException("Empty text in response") }
        } finally {
            connection.disconnect()
        }
    }
}

class GeminiApiException(message: String) : Exception(message)
