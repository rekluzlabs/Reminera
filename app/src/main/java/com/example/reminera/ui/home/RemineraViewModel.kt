package com.example.reminera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminera.data.MemoryEntryEntity
import com.example.reminera.data.MemoryType
import com.example.reminera.data.UploadStatus
import com.example.reminera.data.repository.MemoryEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface MemoryLibraryUiState {
    data object Loading : MemoryLibraryUiState
    data object Empty : MemoryLibraryUiState
    data class Success(val entries: List<MemoryEntryEntity>) : MemoryLibraryUiState
}

class RemineraViewModel(private val repository: MemoryEntryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MemoryLibraryUiState>(MemoryLibraryUiState.Loading)
    val uiState: StateFlow<MemoryLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allEntries
                .map { entries ->
                    if (entries.isEmpty()) MemoryLibraryUiState.Empty
                    else MemoryLibraryUiState.Success(entries)
                }
                .onStart { emit(MemoryLibraryUiState.Loading) }
                .catch { emit(MemoryLibraryUiState.Empty) }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun addRecordedMemory(
        title: String,
        type: MemoryType,
        localFilePath: String,
        durationMillis: Long,
        personTag: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                type = type.name,
                localFilePath = localFilePath,
                thumbnailPath = null,
                personTag = personTag,
                notes = null,
                dateCaptured = now,
                dateAdded = now,
                durationMillis = durationMillis,
                isImported = false,
                uploadStatus = UploadStatus.NOT_UPLOADED.name,
                hostedUrl = null
            )
            repository.insert(entry)
        }
    }

    fun addImportedPhoto(
        title: String,
        localFilePath: String,
        dateCaptured: Long,
        personTag: String?
    ) {
        viewModelScope.launch {
            val entry = MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                type = MemoryType.PHOTO.name,
                localFilePath = localFilePath,
                thumbnailPath = null,
                personTag = personTag,
                notes = null,
                dateCaptured = dateCaptured,
                dateAdded = System.currentTimeMillis(),
                durationMillis = null,
                isImported = true,
                uploadStatus = UploadStatus.NOT_UPLOADED.name,
                hostedUrl = null
            )
            repository.insert(entry)
        }
    }

    fun updateEntryDetails(
        id: String,
        title: String,
        notes: String?,
        personTag: String?,
        type: String? = null,
        localFilePath: String? = null
    ) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    repository.update(
                        entry.copy(
                            title = title,
                            notes = notes,
                            personTag = personTag,
                            type = type ?: entry.type,
                            localFilePath = localFilePath ?: entry.localFilePath
                        )
                    )
                }
                return@collect
            }
        }
    }

    fun updateNotes(id: String, notes: String) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    repository.update(entry.copy(notes = notes))
                }
                return@collect
            }
        }
    }

    fun updatePersonTag(id: String, personTag: String) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    repository.update(entry.copy(personTag = personTag))
                }
                return@collect
            }
        }
    }

    /**
     * Called once a future upload pipeline succeeds.
     */
    fun markUploaded(id: String, hostedUrl: String) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    repository.update(
                        entry.copy(
                            uploadStatus = UploadStatus.UPLOADED.name,
                            hostedUrl = hostedUrl
                        )
                    )
                }
                return@collect
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    repository.delete(entry)
                }
                return@collect
            }
        }
    }
}
