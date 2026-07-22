package com.rekluzlabs.reminera.ui.familygroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.reminera.data.FamilyGroupDao
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.GroupType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyGroupsViewModel(private val dao: FamilyGroupDao) : ViewModel() {

    val groups: StateFlow<List<FamilyGroupEntity>> = dao.getAllOrderedBySortOrder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val entryCounts: StateFlow<Map<Long, Int>> = combine(
        dao.getAllOrderedBySortOrder(),
        dao.getEntryCounts()
    ) { groups, counts ->
        val countMap = counts.associate { it.groupId to it.cnt }
        groups.associate { it.id to (countMap[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addGroup(type: GroupType, customName: String? = null) {
        viewModelScope.launch {
            val currentList = groups.value
            val currentSortOrder = (currentList.lastOrNull()?.sortOrder ?: -1) + 1
            val name = when (type) {
                GroupType.CUSTOM -> customName ?: "Custom"
                else -> type.label
            }
            dao.insert(
                FamilyGroupEntity(
                    name = name,
                    groupType = type.name,
                    sortOrder = currentSortOrder
                )
            )
        }
    }

    fun renameGroup(id: Long, newName: String) {
        viewModelScope.launch {
            val current = groups.value.find { it.id == id } ?: return@launch
            dao.update(current.copy(name = newName))
        }
    }

    fun enterSelectionMode(initialId: Long) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
        if (_selectedIds.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun deleteGroups(ids: List<Long>) {
        viewModelScope.launch {
            dao.deleteByIds(ids)
        }
    }

    fun moveToFront(id: Long) {
        viewModelScope.launch {
            val list = groups.value.sortedBy { it.sortOrder }
            val index = list.indexOfFirst { it.id == id }
            if (index <= 0) return@launch
            val targetOrder = list.first().sortOrder
            val idToOrder = mutableMapOf<Long, Int>()
            idToOrder[id] = targetOrder
            for (i in 0 until index) {
                idToOrder[list[i].id] = list[i].sortOrder + 1
            }
            dao.updateSortOrders(idToOrder)
        }
    }

    fun moveUp(id: Long) {
        viewModelScope.launch {
            val list = groups.value.sortedBy { it.sortOrder }
            val index = list.indexOfFirst { it.id == id }
            if (index <= 0) return@launch
            val idToOrder = mutableMapOf<Long, Int>()
            idToOrder[list[index].id] = list[index - 1].sortOrder
            idToOrder[list[index - 1].id] = list[index].sortOrder
            dao.updateSortOrders(idToOrder)
        }
    }

    fun moveDown(id: Long) {
        viewModelScope.launch {
            val list = groups.value.sortedBy { it.sortOrder }
            val index = list.indexOfFirst { it.id == id }
            if (index < 0 || index >= list.size - 1) return@launch
            val idToOrder = mutableMapOf<Long, Int>()
            idToOrder[list[index].id] = list[index + 1].sortOrder
            idToOrder[list[index + 1].id] = list[index].sortOrder
            dao.updateSortOrders(idToOrder)
        }
    }

    fun moveToEnd(id: Long) {
        viewModelScope.launch {
            val list = groups.value.sortedBy { it.sortOrder }
            val index = list.indexOfFirst { it.id == id }
            if (index < 0 || index >= list.size - 1) return@launch
            val targetOrder = list.last().sortOrder
            val idToOrder = mutableMapOf<Long, Int>()
            idToOrder[id] = targetOrder
            for (i in (index + 1) until list.size) {
                idToOrder[list[i].id] = list[i].sortOrder - 1
            }
            dao.updateSortOrders(idToOrder)
        }
    }
}

class FamilyGroupsViewModelFactory(private val dao: FamilyGroupDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyGroupsViewModel::class.java)) {
            return FamilyGroupsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
