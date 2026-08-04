package com.rekluzlabs.reminera.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.reminera.data.repository.BookExportManifestRepository
import com.rekluzlabs.reminera.export.BookAssembler
import com.rekluzlabs.reminera.export.PdfExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookExportViewModel(
    private val exportManager: PdfExportManager,
    private val manifestRepository: BookExportManifestRepository
) : ViewModel() {
    
    val progress = exportManager.progress
    
    private val _exportResult = MutableStateFlow<BookAssembler.AssembleResult?>(null)
    val exportResult: StateFlow<BookAssembler.AssembleResult?> = _exportResult.asStateFlow()

    fun exportBook(groupId: Long) {
        viewModelScope.launch {
            val manifest = manifestRepository.getOrCreateManifestForGroup(groupId)
            val result = exportManager.exportBook(manifest.id)
            _exportResult.value = result
        }
    }

    fun clearResult() {
        _exportResult.value = null
    }
}

class BookExportViewModelFactory(
    private val exportManager: PdfExportManager,
    private val manifestRepository: BookExportManifestRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookExportViewModel(exportManager, manifestRepository) as T
    }
}
