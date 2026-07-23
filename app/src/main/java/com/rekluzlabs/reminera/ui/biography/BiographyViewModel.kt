package com.rekluzlabs.reminera.ui.biography

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.reminera.data.BiographyEntity
import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import com.rekluzlabs.reminera.data.repository.BiographyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class BiographyUiState(
    val biography: BiographyEntity? = null,
    val sections: List<BiographySectionUiState> = emptyList(),
    val storyEntries: List<StoryEntryEntity> = emptyList(),
    val isLoading: Boolean = true
)

data class BiographySectionUiState(
    val type: String,
    val label: String,
    val fields: Map<String, String> = emptyMap(),
    val isPopulated: Boolean = false
)

val KNOWN_SECTION_TYPES = listOf(
    "origins" to "Origins",
    "milestones" to "Milestones",
    "personality" to "Personality & Character",
    "legacy" to "Legacy & Impact"
)

@OptIn(ExperimentalCoroutinesApi::class)
class BiographyViewModel(
    private val personId: Long,
    private val member: FamilyMemberEntity?,
    private val repository: BiographyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiographyUiState())
    val uiState: StateFlow<BiographyUiState> = _uiState.asStateFlow()

    private val _biographyId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val existing = repository.getBiographyByPersonId(personId).firstOrNull()
            val bioId: String
            if (existing != null) {
                bioId = existing.id
            } else {
                bioId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                repository.upsertBiography(
                    BiographyEntity(
                        id = bioId,
                        personId = personId,
                        fullName = member?.name ?: "",
                        relationship = member?.role ?: "",
                        birthDate = member?.birthDate,
                        familyGroupId = member?.groupId ?: 0L,
                        photoUri = member?.photoUri,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            _biographyId.value = bioId
        }

        viewModelScope.launch {
            _biographyId.flatMapLatest { bioId ->
                if (bioId != null) {
                    combine(
                        repository.getBiographyByPersonId(personId),
                        repository.getSectionsByBiographyId(bioId),
                        repository.getStoryEntriesByBiographyId(bioId)
                    ) { bio, sections, stories ->
                        val sectionUiStates = KNOWN_SECTION_TYPES.map { (type, label) ->
                            val entity = sections.find { it.sectionType == type }
                            val fields = if (entity != null) {
                                parseFieldsJson(entity.fieldsJson)
                            } else emptyMap()
                            BiographySectionUiState(
                                type = type,
                                label = label,
                                fields = fields,
                                isPopulated = entity != null
                            )
                        }
                        BiographyUiState(
                            biography = bio,
                            sections = sectionUiStates,
                            storyEntries = stories,
                            isLoading = false
                        )
                    }
                } else {
                    MutableStateFlow(BiographyUiState(isLoading = true))
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun saveSection(sectionType: String, fields: Map<String, String>) {
        viewModelScope.launch {
            val bioId = _biographyId.value ?: return@launch
            val now = System.currentTimeMillis()
            val existing = repository.getSectionByBiographyIdAndType(bioId, sectionType)
            val entity = existing?.copy(
                fieldsJson = encodeFieldsJson(fields),
                updatedAt = now
            ) ?: BiographySectionEntity(
                id = UUID.randomUUID().toString(),
                biographyId = bioId,
                sectionType = sectionType,
                fieldsJson = encodeFieldsJson(fields),
                updatedAt = now
            )
            repository.upsertSection(entity)
            repository.getBiographyById(bioId)?.let { bio ->
                repository.upsertBiography(bio.copy(updatedAt = now))
            }
        }
    }

    fun updatePhotoUri(photoUri: String?) {
        viewModelScope.launch {
            val bioId = _biographyId.value ?: return@launch
            repository.getBiographyById(bioId)?.let { bio ->
                repository.upsertBiography(bio.copy(photoUri = photoUri, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun addStoryEntry(
        contributedBy: String,
        type: String,
        mediaUri: String?,
        textContent: String?,
        recordedAt: Long
    ) {
        viewModelScope.launch {
            val bioId = _biographyId.value ?: return@launch
            val now = System.currentTimeMillis()
            repository.insertStoryEntry(
                StoryEntryEntity(
                    id = UUID.randomUUID().toString(),
                    biographyId = bioId,
                    contributedBy = contributedBy,
                    type = type,
                    mediaUri = mediaUri,
                    textContent = textContent,
                    recordedAt = recordedAt,
                    createdAt = now
                )
            )
        }
    }

    fun deleteStoryEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteStoryEntry(entryId)
        }
    }

    fun updateStoryEntryMedia(entryId: String, mediaUri: String?) {
        viewModelScope.launch {
            repository.updateStoryEntryMedia(entryId, mediaUri)
        }
    }

    private fun parseFieldsJson(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            val result = mutableMapOf<String, String>()
            val cleaned = json.trim().removeSurrounding("{", "}")
            if (cleaned.isBlank()) return emptyMap()
            cleaned.split(",").forEach { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    result[key] = value
                }
            }
            result
        } catch (_: Exception) { emptyMap() }
    }

    private fun encodeFieldsJson(fields: Map<String, String>): String {
        return fields.entries.joinToString(",") { (k, v) ->
            "\"$k\":\"${v.replace("\"", "\\\"")}\""
        }.let { "{$it}" }
    }
}

class BiographyViewModelFactory(
    private val personId: Long,
    private val member: FamilyMemberEntity?,
    private val repository: BiographyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiographyViewModel::class.java)) {
            return BiographyViewModel(personId, member, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
