package com.baverika.notoir.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.model.NoteColor
import com.baverika.notoir.domain.model.NoteType
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichContent
import com.baverika.notoir.domain.model.RichSpan
import com.baverika.notoir.domain.model.SpanType
import com.baverika.notoir.domain.model.StoryCharacter
import com.baverika.notoir.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class StoryViewMode {
    CHAT,
    SCRIPT
}

data class FocusRequest(
    val blockId: String,
    val cursorPosition: Int = 0,
    val requestId: Long = System.currentTimeMillis()
)

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
    val isNewNote: Boolean = true,
    val focusRequest: FocusRequest? = null,
    // Storymode properties
    val noteType: NoteType = NoteType.STANDARD,
    val characters: List<StoryCharacter> = emptyList(),
    val activeCharacterId: String? = null,
    val storyViewMode: StoryViewMode = StoryViewMode.CHAT
)

private data class EditorSnapshot(
    val title: String,
    val blocks: List<RichBlock>,
    val color: NoteColor,
    val noteType: NoteType,
    val characters: List<StoryCharacter>
)

class EditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: String?,
    private val initialNoteType: NoteType = NoteType.STANDARD
) : ViewModel() {

    private val _state = MutableStateFlow(
        if (initialNoteType == NoteType.STORY && initialNoteId.isNullOrBlank()) {
            val charA = StoryCharacter(name = "Character 1", avatarEmoji = "🧙", colorHex = "#8B5CF6")
            val charB = StoryCharacter(name = "Character 2", avatarEmoji = "🤖", colorHex = "#06B6D4")
            EditorState(
                noteType = NoteType.STORY,
                characters = listOf(charA, charB),
                activeCharacterId = charA.id,
                blocks = listOf(RichBlock(type = BlockType.DIALOGUE, characterId = charA.id))
            )
        } else {
            EditorState(noteType = initialNoteType)
        }
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = mutableListOf<EditorSnapshot>()
    private val redoStack = mutableListOf<EditorSnapshot>()

    private var autoSaveJob: Job? = null

    init {
        if (!initialNoteId.isNullOrBlank()) {
            loadNote(initialNoteId)
        } else {
            recordSnapshot()
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                val blocks = if (note.content.blocks.isEmpty()) listOf(RichBlock()) else note.content.blocks
                val activeChar = note.content.characters.firstOrNull()?.id
                _state.value = EditorState(
                    noteId = note.id,
                    title = note.title,
                    blocks = blocks,
                    color = note.color,
                    isPinned = note.isPinned,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    activeBlockIndex = 0,
                    isNewNote = false,
                    noteType = note.content.noteType,
                    characters = note.content.characters,
                    activeCharacterId = activeChar
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
            color = currentState.color,
            noteType = currentState.noteType,
            characters = currentState.characters
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
        val block = _state.value.blocks.getOrNull(index)
        _state.value = _state.value.copy(
            activeBlockIndex = index,
            activeSelection = selection,
            activeCharacterId = block?.characterId ?: _state.value.activeCharacterId
        )
    }

    fun clearFocusRequest() {
        _state.value = _state.value.copy(focusRequest = null)
    }

    fun splitBlock(index: Int, textBefore: String, textAfter: String) {
        recordSnapshot()
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index !in currentBlocks.indices) return

        val oldBlock = currentBlocks[index]

        // If in standard mode and user pressed Enter on an empty checklist/bullet/numbered item:
        // Convert current block to normal paragraph instead of creating a new list item
        if (_state.value.noteType == NoteType.STANDARD &&
            (oldBlock.type == BlockType.CHECKLIST || oldBlock.type == BlockType.BULLET || oldBlock.type == BlockType.NUMBERED) &&
            textBefore.isEmpty() && textAfter.isEmpty()
        ) {
            currentBlocks[index] = oldBlock.copy(type = BlockType.PARAGRAPH)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = index,
                focusRequest = FocusRequest(oldBlock.id, cursorPosition = 0),
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
            return
        }

        // Determine new block type and character if storymode
        val newBlock: RichBlock = if (_state.value.noteType == NoteType.STORY) {
            val chars = _state.value.characters
            val nextCharId = when {
                oldBlock.type == BlockType.NARRATOR -> {
                    // After a scene / narrator beat, pressing Enter transitions to dialogue for the active speaker
                    _state.value.activeCharacterId ?: chars.firstOrNull()?.id
                }
                chars.size == 2 -> {
                    // Smart back-and-forth conversational dialogue alternation
                    if (oldBlock.characterId == chars[0].id) chars[1].id else chars[0].id
                }
                chars.isNotEmpty() -> {
                    _state.value.activeCharacterId ?: chars.first().id
                }
                else -> null
            }
            RichBlock(
                type = BlockType.DIALOGUE,
                text = textAfter,
                characterId = nextCharId
            )
        } else {
            val nextType = when (oldBlock.type) {
                BlockType.CHECKLIST, BlockType.BULLET, BlockType.NUMBERED -> oldBlock.type
                else -> BlockType.PARAGRAPH
            }
            RichBlock(
                type = nextType,
                text = textAfter
            )
        }

        // Adjust spans on the current block
        val adjustedSpans = oldBlock.spans.filter { it.start < textBefore.length }.map {
            it.copy(end = minOf(it.end, textBefore.length))
        }
        currentBlocks[index] = oldBlock.copy(text = textBefore, spans = adjustedSpans)

        // Insert new block directly after current block
        val insertIndex = index + 1
        currentBlocks.add(insertIndex, newBlock)

        _state.value = _state.value.copy(
            blocks = currentBlocks,
            activeBlockIndex = insertIndex,
            activeCharacterId = newBlock.characterId ?: _state.value.activeCharacterId,
            focusRequest = FocusRequest(newBlock.id, cursorPosition = 0),
            updatedAt = System.currentTimeMillis()
        )
        recordSnapshot()
        saveImmediately()
    }

    fun addNewBlockAfter(
        index: Int,
        type: BlockType = BlockType.PARAGRAPH,
        characterId: String? = null
    ) {
        val currentBlocks = _state.value.blocks.toMutableList()
        val insertIndex = (index + 1).coerceAtMost(currentBlocks.size)
        val chosenCharId = characterId ?: _state.value.activeCharacterId ?: _state.value.characters.firstOrNull()?.id
        val newBlock = RichBlock(
            type = type,
            characterId = if (type == BlockType.DIALOGUE) chosenCharId else null
        )
        currentBlocks.add(insertIndex, newBlock)
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            activeBlockIndex = insertIndex,
            activeSelection = TextRange.Zero,
            focusRequest = FocusRequest(newBlock.id, cursorPosition = 0),
            updatedAt = System.currentTimeMillis()
        )
        recordSnapshot()
        saveImmediately()
    }

    fun removeBlockAt(index: Int) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (currentBlocks.size > 1 && index in currentBlocks.indices) {
            currentBlocks.removeAt(index)
            val newActive = (index - 1).coerceAtLeast(0)
            val targetBlock = currentBlocks[newActive]
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = newActive,
                focusRequest = FocusRequest(targetBlock.id, cursorPosition = targetBlock.text.length),
                updatedAt = System.currentTimeMillis()
            )
            recordSnapshot()
            saveImmediately()
        } else if (currentBlocks.size == 1 && index == 0) {
            val resetBlock = currentBlocks[0].copy(text = "", spans = emptyList(), parenthetical = null)
            currentBlocks[0] = resetBlock
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                focusRequest = FocusRequest(resetBlock.id, cursorPosition = 0),
                updatedAt = System.currentTimeMillis()
            )
            recordSnapshot()
            saveImmediately()
        }
    }

    fun toggleChecklist(blockId: String) {
        val currentBlocks = _state.value.blocks.map {
            if (it.id == blockId) it.toggleChecked() else it
        }
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            updatedAt = System.currentTimeMillis()
        )
        recordSnapshot()
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

            val (start, end) = if (selection.min != selection.max) {
                Pair(selection.min, selection.max)
            } else {
                findWordBoundaries(text, selection.start)
            }

            if (start < end && end <= text.length) {
                recordSnapshot()
                val currentSpans = block.spans.toMutableList()

                if (spanType == SpanType.COLOR && colorHex != null) {
                    currentSpans.removeAll {
                        it.type == SpanType.COLOR && !(it.end <= start || it.start >= end)
                    }
                    currentSpans.add(RichSpan(start = start, end = end, type = SpanType.COLOR, colorHex = colorHex))
                } else {
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

    fun setNoteColor(color: NoteColor) {
        recordSnapshot()
        _state.value = _state.value.copy(
            color = color,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    // Storymode Actions
    fun setNoteType(type: NoteType) {
        recordSnapshot()
        val defaultCharacters = if (type == NoteType.STORY && _state.value.characters.isEmpty()) {
            listOf(
                StoryCharacter(name = "Character 1", avatarEmoji = "🧙", colorHex = "#8B5CF6"),
                StoryCharacter(name = "Character 2", avatarEmoji = "🤖", colorHex = "#06B6D4")
            )
        } else {
            _state.value.characters
        }

        val updatedBlocks = if (type == NoteType.STORY) {
            _state.value.blocks.map { block ->
                if (block.type == BlockType.PARAGRAPH && block.characterId == null) {
                    block.copy(type = BlockType.DIALOGUE, characterId = defaultCharacters.firstOrNull()?.id)
                } else block
            }
        } else {
            _state.value.blocks
        }

        _state.value = _state.value.copy(
            noteType = type,
            characters = defaultCharacters,
            activeCharacterId = defaultCharacters.firstOrNull()?.id,
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun toggleStoryViewMode() {
        val nextMode = if (_state.value.storyViewMode == StoryViewMode.CHAT) StoryViewMode.SCRIPT else StoryViewMode.CHAT
        _state.value = _state.value.copy(storyViewMode = nextMode)
    }

    fun addCharacter(name: String, emoji: String, colorHex: String, role: String? = null) {
        recordSnapshot()
        val newChar = StoryCharacter(name = name.trim().ifEmpty { "Character" }, avatarEmoji = emoji, colorHex = colorHex, role = role?.trim())
        val updated = _state.value.characters + newChar
        _state.value = _state.value.copy(
            characters = updated,
            activeCharacterId = newChar.id,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun updateCharacter(updatedChar: StoryCharacter) {
        recordSnapshot()
        val list = _state.value.characters.map { if (it.id == updatedChar.id) updatedChar else it }
        _state.value = _state.value.copy(characters = list, updatedAt = System.currentTimeMillis())
        saveImmediately()
    }

    fun deleteCharacter(characterId: String) {
        recordSnapshot()
        val list = _state.value.characters.filter { it.id != characterId }
        val newActive = if (_state.value.activeCharacterId == characterId) list.firstOrNull()?.id else _state.value.activeCharacterId
        _state.value = _state.value.copy(
            characters = list,
            activeCharacterId = newActive,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun setActiveCharacter(characterId: String) {
        val activeIdx = _state.value.activeBlockIndex
        val currentBlocks = _state.value.blocks.toMutableList()
        if (activeIdx in currentBlocks.indices) {
            recordSnapshot()
            currentBlocks[activeIdx] = currentBlocks[activeIdx].copy(
                type = BlockType.DIALOGUE,
                characterId = characterId
            )
            _state.value = _state.value.copy(
                activeCharacterId = characterId,
                blocks = currentBlocks,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
            return
        }
        _state.value = _state.value.copy(activeCharacterId = characterId)
    }

    fun setBlockCharacter(index: Int, characterId: String) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            recordSnapshot()
            currentBlocks[index] = currentBlocks[index].copy(type = BlockType.DIALOGUE, characterId = characterId)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeCharacterId = characterId,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun setBlockParenthetical(index: Int, parenthetical: String?) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            currentBlocks[index] = currentBlocks[index].copy(parenthetical = parenthetical)
            _state.value = _state.value.copy(blocks = currentBlocks, updatedAt = System.currentTimeMillis())
            recordSnapshot()
            saveImmediately()
        }
    }

    fun toggleActiveBlockNarrator() {
        val index = _state.value.activeBlockIndex
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            val old = currentBlocks[index]
            val nextType = if (old.type == BlockType.NARRATOR) BlockType.DIALOGUE else BlockType.NARRATOR
            val charId = if (nextType == BlockType.DIALOGUE) {
                _state.value.activeCharacterId ?: _state.value.characters.firstOrNull()?.id
            } else null
            currentBlocks[index] = old.copy(type = nextType, characterId = charId)
            _state.value = _state.value.copy(blocks = currentBlocks, updatedAt = System.currentTimeMillis())
            recordSnapshot()
            saveImmediately()
        }
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            val previous = undoStack.last()

            val safeActiveIndex = _state.value.activeBlockIndex.coerceIn(0, previous.blocks.lastIndex.coerceAtLeast(0))
            val targetBlock = previous.blocks.getOrNull(safeActiveIndex)

            _state.value = _state.value.copy(
                title = previous.title,
                blocks = previous.blocks,
                color = previous.color,
                noteType = previous.noteType,
                characters = previous.characters,
                activeBlockIndex = safeActiveIndex,
                focusRequest = targetBlock?.let { FocusRequest(it.id, cursorPosition = it.text.length) },
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

            val safeActiveIndex = _state.value.activeBlockIndex.coerceIn(0, next.blocks.lastIndex.coerceAtLeast(0))
            val targetBlock = next.blocks.getOrNull(safeActiveIndex)

            _state.value = _state.value.copy(
                title = next.title,
                blocks = next.blocks,
                color = next.color,
                noteType = next.noteType,
                characters = next.characters,
                activeBlockIndex = safeActiveIndex,
                focusRequest = targetBlock?.let { FocusRequest(it.id, cursorPosition = it.text.length) },
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
                content = RichContent(
                    blocks = currentState.blocks,
                    noteType = currentState.noteType,
                    characters = currentState.characters
                ),
                color = currentState.color,
                createdAt = currentState.createdAt,
                updatedAt = currentState.updatedAt,
                isPinned = currentState.isPinned
            )

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
                content = RichContent(
                    blocks = currentState.blocks,
                    noteType = currentState.noteType,
                    characters = currentState.characters
                ),
                color = currentState.color
            )
            repository.deleteNote(note)
            onDeleted()
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: String?,
        private val initialNoteType: NoteType = NoteType.STANDARD
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(repository, noteId, initialNoteType) as T
        }
    }
}
