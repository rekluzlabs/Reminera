package com.rekluzlabs.reminera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.MemoryType
import com.rekluzlabs.reminera.data.UploadStatus
import com.rekluzlabs.reminera.data.repository.MemoryEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface MemoryLibraryUiState {
    data object Loading : MemoryLibraryUiState
    data object Empty : MemoryLibraryUiState
    data class Success(val entries: List<MemoryEntryEntity>) : MemoryLibraryUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RemineraViewModel(private val repository: MemoryEntryRepository) : ViewModel() {

    private val _groupId = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow<MemoryLibraryUiState>(MemoryLibraryUiState.Loading)
    val uiState: StateFlow<MemoryLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _groupId.flatMapLatest { groupId ->
                val source = if (groupId != null) {
                    repository.getEntriesByGroupId(groupId)
                } else {
                    repository.allEntries
                }
                source
                    .map { entries ->
                        if (entries.isEmpty()) MemoryLibraryUiState.Empty
                        else MemoryLibraryUiState.Success(entries)
                    }
                    .onStart { emit(MemoryLibraryUiState.Loading) }
                    .catch { emit(MemoryLibraryUiState.Empty) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setGroupId(groupId: Long?) {
        _groupId.value = groupId
    }

    fun addRecordedMemory(
        title: String,
        type: MemoryType,
        localFilePath: String,
        durationMillis: Long,
        personTag: String?,
        groupId: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId ?: _groupId.value ?: 0L,
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
        personTag: String?,
        groupId: Long? = null,
        secondaryMediaPath: String? = null,
        secondaryMediaType: String? = null
    ) {
        viewModelScope.launch {
            val entry = MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId ?: _groupId.value ?: 0L,
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
                hostedUrl = null,
                secondaryMediaPath = secondaryMediaPath,
                secondaryMediaType = secondaryMediaType
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

class RemineraViewModelFactory(
    private val repository: MemoryEntryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemineraViewModel::class.java)) {
            return RemineraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
