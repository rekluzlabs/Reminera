package com.rekluzlabs.reminera.export

data class BiographyGenerationInput(
    val name: String,
    val relationship: String,
    val dateOfBirth: Long?,
    val biographyText: String,
    val sections: List<Pair<String, String>>,
    val stories: List<String>
)

fun interface BiographyGenerationProvider {
    suspend fun generateBiography(input: BiographyGenerationInput): Result<String>
}
