package com.rekluzlabs.reminera.ui.tutorial

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TutorialViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val coordinator = TutorialCoordinator()

    init {
        savedStateHandle.get<Boolean>("tutorial_is_active")?.let { active ->
            coordinator.isActive = active
            savedStateHandle.get<Int>("tutorial_step_index")?.let { index ->
                coordinator.currentStepIndex = index
            }
        }

        viewModelScope.launch {
            snapshotFlow {
                val active = coordinator.isActive
                val index = coordinator.currentStepIndex
                Pair<Boolean, Int>(active, index)
            }.collectLatest { pair ->
                savedStateHandle["tutorial_is_active"] = pair.first
                savedStateHandle["tutorial_step_index"] = pair.second
            }
        }
    }
}
