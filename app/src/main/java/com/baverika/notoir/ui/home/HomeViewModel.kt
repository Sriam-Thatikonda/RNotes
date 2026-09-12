package com.baverika.notoir.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.repository.NoteRepository
import com.baverika.notoir.ui.theme.ThemeMode
import com.baverika.notoir.util.export.JsonExporter
import com.baverika.notoir.util.import.DuplicateStrategy
import com.baverika.notoir.util.import.ImportResult
import com.baverika.notoir.util.import.JsonImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

enum class NoteSortOrder {
    RECENTLY_MODIFIED,
    RECENTLY_CREATED,
    ALPHABETICAL
}

enum class ViewLayoutMode {
    STAGGERED_GRID,
    LIST
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.RECENTLY_MODIFIED)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _layoutMode = MutableStateFlow(ViewLayoutMode.STAGGERED_GRID)
    val layoutMode: StateFlow<ViewLayoutMode> = _layoutMode.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Holds recently deleted note for instant undo
    private var recentlyDeletedNote: Note? = null

    val notes: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllNotes()
            } else {
                repository.searchNotes(query)
            }
        }
        .combine(_sortOrder) { list, sort ->
            when (sort) {
                NoteSortOrder.RECENTLY_MODIFIED -> list.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
                )
                NoteSortOrder.RECENTLY_CREATED -> list.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt }
                )
                NoteSortOrder.ALPHABETICAL -> list.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenBy { it.displayTitle.lowercase() }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun toggleLayoutMode() {
        _layoutMode.value = if (_layoutMode.value == ViewLayoutMode.STAGGERED_GRID) {
            ViewLayoutMode.LIST
        } else {
            ViewLayoutMode.STAGGERED_GRID
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun deleteNote(note: Note, onShowUndoSnackbar: (String) -> Unit) {
        viewModelScope.launch {
            recentlyDeletedNote = note
            repository.deleteNote(note)
            onShowUndoSnackbar("Note deleted")
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            recentlyDeletedNote?.let { note ->
                repository.insertOrUpdateNote(note)
                recentlyDeletedNote = null
                _snackbarMessage.value = "Note restored"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun exportNotes(outputStream: OutputStream, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val allNotes = notes.value
            val result = JsonExporter.exportNotesToJson(allNotes, outputStream)
            onResult(result)
        }
    }

    fun importNotes(
        inputStream: InputStream,
        strategy: DuplicateStrategy,
        onResult: (Result<ImportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val currentNotes = notes.value
            val result = JsonImporter.importNotesFromStream(inputStream, currentNotes, strategy)
            result.onSuccess { importResult ->
                repository.insertNotes(importResult.finalNotesToSave)
            }
            onResult(result)
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
