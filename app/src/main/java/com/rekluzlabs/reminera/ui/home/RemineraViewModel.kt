package com.rekluzlabs.reminera.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.MemoryType
import com.rekluzlabs.reminera.data.UploadStatus
import com.rekluzlabs.reminera.data.repository.FamilyMemberRepository
import com.rekluzlabs.reminera.data.repository.MemoryEntryRepository
import com.rekluzlabs.reminera.util.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface MemoryLibraryUiState {
    data object Loading : MemoryLibraryUiState
    data object Empty : MemoryLibraryUiState
    data class Success(val entries: List<MemoryEntryEntity>) : MemoryLibraryUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RemineraViewModel(
    private val repository: MemoryEntryRepository,
    private val memberRepository: FamilyMemberRepository
) : ViewModel() {

    private val _groupId = MutableStateFlow<Long?>(null)

    private val _personTagFilter = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<MemoryLibraryUiState>(MemoryLibraryUiState.Loading)
    val uiState: StateFlow<MemoryLibraryUiState> = _uiState.asStateFlow()

    private val _mediaActionResult = MutableStateFlow<MediaActionResult?>(null)
    val mediaActionResult: StateFlow<MediaActionResult?> = _mediaActionResult.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_groupId, _personTagFilter) { groupId, personTag ->
                Pair(groupId, personTag)
            }.flatMapLatest { (groupId, personTag) ->
                val source: Flow<List<MemoryEntryEntity>> = when {
                    groupId != null && personTag != null ->
                        repository.getEntriesByGroupIdAndPersonTag(groupId, personTag)
                    groupId != null ->
                        repository.getEntriesByGroupId(groupId)
                    else ->
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

    fun setPersonTagFilter(personTag: String?) {
        _personTagFilter.value = personTag
    }

    fun clearActionResult() {
        _mediaActionResult.value = null
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
            val maxSortOrder = getMaxSortOrder(groupId ?: _groupId.value ?: 0L, personTag)
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
                hostedUrl = null,
                sortOrder = maxSortOrder + 1
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
            val maxSortOrder = getMaxSortOrder(groupId ?: _groupId.value ?: 0L, personTag)
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
                secondaryMediaType = secondaryMediaType,
                sortOrder = maxSortOrder + 1
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
            val entry = repository.getEntryById(id).firstOrNull()
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
        }
    }

    fun updateNotes(id: String, notes: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id).firstOrNull()
            if (entry != null) {
                repository.update(entry.copy(notes = notes))
            }
        }
    }

    fun updatePersonTag(id: String, personTag: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id).firstOrNull()
            if (entry != null) {
                repository.update(entry.copy(personTag = personTag))
            }
        }
    }

    fun markUploaded(id: String, hostedUrl: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id).firstOrNull()
            if (entry != null) {
                repository.update(
                    entry.copy(
                        uploadStatus = UploadStatus.UPLOADED.name,
                        hostedUrl = hostedUrl
                    )
                )
            }
        }
    }

    fun moveToGroup(id: String, newGroupId: Long) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id).firstOrNull()
            if (entry != null) {
                repository.update(entry.copy(groupId = newGroupId))
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id).firstOrNull()
            if (entry != null) {
                repository.delete(entry)
                withContext(Dispatchers.IO) {
                    try {
                        val file = File(entry.localFilePath)
                        if (file.exists()) file.delete()
                        entry.thumbnailPath?.let { tp ->
                            val thumbFile = File(tp)
                            if (thumbFile.exists()) thumbFile.delete()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun handleMediaAction(action: MediaAction, context: Context) {
        viewModelScope.launch {
            when (action) {
                is MediaAction.Rename -> {
                    val entry = repository.getEntryById(action.entryId).firstOrNull()
                    if (entry != null) {
                        val newFilePath = withContext(Dispatchers.IO) {
                            val oldFile = java.io.File(entry.localFilePath)
                            if (oldFile.exists() && oldFile.parentFile != null) {
                                val extension = oldFile.extension.ifBlank {
                                    when (entry.type) {
                                        "PHOTO" -> "jpg"
                                        "VIDEO" -> "mp4"
                                        "AUDIO" -> "mp4"
                                        else -> "bin"
                                    }
                                }
                                val sanitized = action.newTitle.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                                val newFile = java.io.File(oldFile.parentFile, "${sanitized}_${entry.id.take(8)}.$extension")
                                if (oldFile.renameTo(newFile)) newFile.absolutePath else entry.localFilePath
                            } else {
                                entry.localFilePath
                            }
                        }
                        repository.update(entry.copy(title = action.newTitle, localFilePath = newFilePath))
                        _mediaActionResult.value = MediaActionResult.Success
                    } else {
                        _mediaActionResult.value = MediaActionResult.Error("Entry not found")
                    }
                }

                is MediaAction.Move -> {
                    val entry = repository.getEntryById(action.entryId).firstOrNull()
                    if (entry != null) {
                        repository.update(
                            entry.copy(
                                groupId = _groupId.value ?: entry.groupId,
                                personTag = action.targetMemberName
                            )
                        )
                        _mediaActionResult.value = MediaActionResult.Success
                    } else {
                        _mediaActionResult.value = MediaActionResult.Error("Entry not found")
                    }
                }

                is MediaAction.Download -> {
                    val entry = repository.getEntryById(action.entryId).firstOrNull()
                    if (entry != null) {
                        val success = withContext(Dispatchers.IO) {
                            DownloadHelper.downloadToDownloads(context, entry)
                        }
                        _mediaActionResult.value = if (success) {
                            MediaActionResult.Success
                        } else {
                            MediaActionResult.Error("Failed to download")
                        }
                    } else {
                        _mediaActionResult.value = MediaActionResult.Error("Entry not found")
                    }
                }

                is MediaAction.Delete -> {
                    val entry = repository.getEntryById(action.entryId).firstOrNull()
                    if (entry != null) {
                        repository.delete(entry)
                        withContext(Dispatchers.IO) {
                            try {
                                val file = File(entry.localFilePath)
                                if (file.exists()) file.delete()
                                entry.thumbnailPath?.let { tp ->
                                    val thumbFile = File(tp)
                                    if (thumbFile.exists()) thumbFile.delete()
                                }
                            } catch (_: Exception) {}
                        }
                        _mediaActionResult.value = MediaActionResult.Success
                    } else {
                        _mediaActionResult.value = MediaActionResult.Error("Entry not found")
                    }
                }
            }
        }
    }

    fun commitReorder(reorderedIds: List<String>) {
        viewModelScope.launch {
            val idToOrder = reorderedIds.mapIndexed { index, id ->
                id to index
            }.toMap()
            repository.updateSortOrders(idToOrder)
        }
    }

    fun getMembersByGroupId(groupId: Long): Flow<List<FamilyMemberEntity>> =
        memberRepository.getMembersByGroupId(groupId)

    fun addMember(
        groupId: Long,
        name: String,
        role: String,
        biography: String = "",
        birthDate: Long? = null,
        photoUri: String? = null,
        sortOrder: Int = 0
    ) {
        viewModelScope.launch {
            memberRepository.insert(
                FamilyMemberEntity(
                    groupId = groupId,
                    name = name,
                    role = role,
                    biography = biography,
                    birthDate = birthDate,
                    photoUri = photoUri,
                    sortOrder = sortOrder
                )
            )
        }
    }

    private suspend fun getMember(memberId: Long): FamilyMemberEntity? =
        memberRepository.getMemberById(memberId)

    fun updateMemberName(memberId: Long, newName: String) {
        viewModelScope.launch {
            val member = getMember(memberId) ?: return@launch
            memberRepository.update(member.copy(name = newName))
        }
    }

    fun updateMemberBiography(memberId: Long, biography: String) {
        viewModelScope.launch {
            val member = getMember(memberId) ?: return@launch
            memberRepository.update(member.copy(biography = biography))
        }
    }

    fun updateMemberDetails(
        memberId: Long,
        name: String,
        role: String,
        birthDate: Long?,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val member = getMember(memberId) ?: return@launch
            memberRepository.update(
                member.copy(
                    name = name,
                    role = role,
                    birthDate = birthDate,
                    photoUri = photoUri ?: member.photoUri
                )
            )
        }
    }

    fun updateMemberPhoto(memberId: Long, photoUri: String?) {
        viewModelScope.launch {
            val member = getMember(memberId) ?: return@launch
            memberRepository.update(member.copy(photoUri = photoUri))
        }
    }

    fun deleteMember(id: Long) {
        viewModelScope.launch {
            memberRepository.deleteById(id)
        }
    }

    private suspend fun getMaxSortOrder(groupId: Long, personTag: String?): Int {
        if (personTag == null) return 0
        val entries = repository.getEntriesByGroupIdAndPersonTagList(groupId, personTag)
        return entries.maxOfOrNull { it.sortOrder } ?: 0
    }
}

class RemineraViewModelFactory(
    private val repository: MemoryEntryRepository,
    private val memberRepository: FamilyMemberRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemineraViewModel::class.java)) {
            return RemineraViewModel(repository, memberRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
