package com.baverika.notoir.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.model.NoteColor
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichContent
import com.baverika.notoir.domain.model.RichSpan
import com.baverika.notoir.domain.model.SpanType
import com.baverika.notoir.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorState(
    val noteId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val blocks: List<RichBlock> = listOf(RichBlock()),
    val color: NoteColor = NoteColor.DEFAULT,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeBlockIndex: Int = 0,
    val activeSelection: TextRange = TextRange.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val isNewNote: Boolean = true
)

private data class EditorSnapshot(
    val title: String,
    val blocks: List<RichBlock>,
    val color: NoteColor
)

class EditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = mutableListOf<EditorSnapshot>()
    private val redoStack = mutableListOf<EditorSnapshot>()

    private var autoSaveJob: Job? = null

    init {
        if (!initialNoteId.isNullOrBlank()) {
            loadNote(initialNoteId)
        } else {
            // Push initial empty snapshot
            recordSnapshot()
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                val blocks = if (note.content.blocks.isEmpty()) listOf(RichBlock()) else note.content.blocks
                _state.value = EditorState(
                    noteId = note.id,
                    title = note.title,
                    blocks = blocks,
                    color = note.color,
                    isPinned = note.isPinned,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    activeBlockIndex = 0,
                    isNewNote = false
                )
                undoStack.clear()
                redoStack.clear()
                recordSnapshot()
            }
        }
    }

    private fun recordSnapshot() {
        val currentState = _state.value
        val snapshot = EditorSnapshot(
            title = currentState.title,
            blocks = currentState.blocks,
            color = currentState.color
        )
        if (undoStack.lastOrNull() != snapshot) {
            undoStack.add(snapshot)
            redoStack.clear()
            updateUndoRedoAvailability()
        }
    }

    private fun updateUndoRedoAvailability() {
        _state.value = _state.value.copy(
            canUndo = undoStack.size > 1,
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun onTitleChanged(newTitle: String) {
        _state.value = _state.value.copy(title = newTitle, updatedAt = System.currentTimeMillis())
        scheduleAutoSave()
    }

    fun onBlockTextChanged(index: Int, newText: String, selection: TextRange) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            val oldBlock = currentBlocks[index]
            // Adjust spans if text shrunk
            val adjustedSpans = oldBlock.spans.filter { it.start < newText.length }.map {
                it.copy(end = minOf(it.end, newText.length))
            }
            currentBlocks[index] = oldBlock.copy(text = newText, spans = adjustedSpans)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = index,
                activeSelection = selection,
                updatedAt = System.currentTimeMillis()
            )
            scheduleAutoSave()
        }
    }

    fun onBlockFocusChanged(index: Int, selection: TextRange) {
        _state.value = _state.value.copy(
            activeBlockIndex = index,
            activeSelection = selection
        )
    }

    fun toggleChecklist(blockId: String) {
        recordSnapshot()
        val currentBlocks = _state.value.blocks.map {
            if (it.id == blockId) it.toggleChecked() else it
        }
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun setBlockType(type: BlockType) {
        val index = _state.value.activeBlockIndex
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            recordSnapshot()
            val oldBlock = currentBlocks[index]
            currentBlocks[index] = oldBlock.copy(
                type = if (oldBlock.type == type) BlockType.PARAGRAPH else type,
                isChecked = if (type == BlockType.CHECKLIST) oldBlock.isChecked else false
            )
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun applyFormatting(spanType: SpanType, colorHex: String? = null) {
        val index = _state.value.activeBlockIndex
        val selection = _state.value.activeSelection
        val currentBlocks = _state.value.blocks.toMutableList()

        if (index in currentBlocks.indices) {
            val block = currentBlocks[index]
            val text = block.text

            // Calculate range to apply formatting
            val (start, end) = if (selection.min != selection.max) {
                Pair(selection.min, selection.max)
            } else {
                // If cursor is at a word, format the current word
                findWordBoundaries(text, selection.start)
            }

            if (start < end && end <= text.length) {
                recordSnapshot()
                val currentSpans = block.spans.toMutableList()

                if (spanType == SpanType.COLOR && colorHex != null) {
                    // Remove existing color spans overlapping this range
                    currentSpans.removeAll {
                        it.type == SpanType.COLOR && !(it.end <= start || it.start >= end)
                    }
                    currentSpans.add(RichSpan(start = start, end = end, type = SpanType.COLOR, colorHex = colorHex))
                } else {
                    // Toggle style: if already fully covered by this span type, remove it, otherwise add it
                    val existing = currentSpans.firstOrNull {
                        it.type == spanType && it.start <= start && it.end >= end
                    }
                    if (existing != null) {
                        currentSpans.remove(existing)
                    } else {
                        currentSpans.add(RichSpan(start = start, end = end, type = spanType))
                    }
                }

                currentBlocks[index] = block.copy(spans = currentSpans)
                _state.value = _state.value.copy(
                    blocks = currentBlocks,
                    updatedAt = System.currentTimeMillis()
                )
                saveImmediately()
            }
        }
    }

    private fun findWordBoundaries(text: String, cursor: Int): Pair<Int, Int> {
        if (text.isEmpty() || cursor < 0 || cursor > text.length) return Pair(0, 0)
        var start = cursor
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }
        var end = cursor
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }
        return Pair(start, end)
    }

    fun addNewBlockAfter(index: Int, type: BlockType = BlockType.PARAGRAPH) {
        recordSnapshot()
        val currentBlocks = _state.value.blocks.toMutableList()
        val insertIndex = (index + 1).coerceAtMost(currentBlocks.size)
        val newBlock = RichBlock(type = type)
        currentBlocks.add(insertIndex, newBlock)
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            activeBlockIndex = insertIndex,
            activeSelection = TextRange.Zero,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun removeBlockAt(index: Int) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (currentBlocks.size > 1 && index in currentBlocks.indices) {
            recordSnapshot()
            currentBlocks.removeAt(index)
            val newActive = (index - 1).coerceAtLeast(0)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = newActive,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun setNoteColor(color: NoteColor) {
        recordSnapshot()
        _state.value = _state.value.copy(
            color = color,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            val previous = undoStack.last()

            _state.value = _state.value.copy(
                title = previous.title,
                blocks = previous.blocks,
                color = previous.color,
                updatedAt = System.currentTimeMillis()
            )
            updateUndoRedoAvailability()
            saveImmediately()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(next)

            _state.value = _state.value.copy(
                title = next.title,
                blocks = next.blocks,
                color = next.color,
                updatedAt = System.currentTimeMillis()
            )
            updateUndoRedoAvailability()
            saveImmediately()
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            recordSnapshot()
            saveImmediately()
        }
    }

    fun saveImmediately() {
        viewModelScope.launch {
            val currentState = _state.value
            val note = Note(
                id = currentState.noteId,
                title = currentState.title,
                content = RichContent(blocks = currentState.blocks),
                color = currentState.color,
                createdAt = currentState.createdAt,
                updatedAt = currentState.updatedAt,
                isPinned = currentState.isPinned
            )

            // Only save if it has some content or is an existing note
            if (!note.isBlank || !currentState.isNewNote) {
                _state.value = _state.value.copy(isSaving = true)
                repository.insertOrUpdateNote(note)
                _state.value = _state.value.copy(isSaving = false, isNewNote = false)
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val currentState = _state.value
            val note = Note(
                id = currentState.noteId,
                title = currentState.title,
                content = RichContent(blocks = currentState.blocks),
                color = currentState.color
            )
            repository.deleteNote(note)
            onDeleted()
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(repository, noteId) as T
        }
    }
}
