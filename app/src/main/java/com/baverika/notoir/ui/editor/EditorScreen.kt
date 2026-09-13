package com.baverika.notoir.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.NoteColor
import com.baverika.notoir.domain.model.NoteType
import com.baverika.notoir.domain.model.StoryCharacter
import com.baverika.notoir.ui.editor.components.CharacterManagementDialog
import com.baverika.notoir.ui.editor.components.RichBlockItem
import com.baverika.notoir.ui.editor.components.RichFormattingToolbar
import com.baverika.notoir.ui.editor.components.StoryCharacterBar
import com.baverika.notoir.ui.editor.components.StoryDialogueBlockItem
import com.baverika.notoir.util.time.DateFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isDark = isSystemInDarkTheme()

    val titleFocusRequester = remember { FocusRequester() }
    val blockFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var blockIndexToDelete by remember { mutableStateOf<Int?>(null) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showCharacterDialog by remember { mutableStateOf(false) }
    var characterToEdit by remember { mutableStateOf<StoryCharacter?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Autofocus title on new note creation
    LaunchedEffect(state.isNewNote) {
        if (state.isNewNote && state.title.isEmpty()) {
            titleFocusRequester.requestFocus()
        }
    }

    // Programmatic focus navigation observer (for Enter, Backspace, or new block creation)
    LaunchedEffect(state.focusRequest?.requestId) {
        val request = state.focusRequest ?: return@LaunchedEffect
        val targetIndex = state.blocks.indexOfFirst { it.id == request.blockId }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
            delay(20)
            try {
                blockFocusRequesters[request.blockId]?.requestFocus()
            } catch (_: Exception) {
                delay(40)
                try {
                    blockFocusRequesters[request.blockId]?.requestFocus()
                } catch (_: Exception) {}
            }
        }
        viewModel.clearFocusRequest()
    }

    // Auto-save on system back button
    BackHandler {
        viewModel.saveImmediately()
        onNavigateBack()
    }

    val backgroundColor by animateColorAsState(
        targetValue = state.color.containerColor(isDark),
        label = "editorBackgroundColor"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (state.noteType == NoteType.STORY) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Storymode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveImmediately()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = state.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = state.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }

                    // Story Mode Toggle Button
                    IconButton(onClick = {
                        val nextType = if (state.noteType == NoteType.STORY) NoteType.STANDARD else NoteType.STORY
                        viewModel.setNoteType(nextType)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = if (state.noteType == NoteType.STORY) "Convert to Standard Note" else "Convert to Storymode",
                            tint = if (state.noteType == NoteType.STORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Note Color Picker Pill
                    Box {
                        IconButton(onClick = { showColorDropdown = true }) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(state.color.dot())
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }

                        DropdownMenu(
                            expanded = showColorDropdown,
                            onDismissRequest = { showColorDropdown = false }
                        ) {
                            NoteColor.entries.forEach { noteColor ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(noteColor.dot())
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(noteColor.displayName)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setNoteColor(noteColor)
                                        showColorDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Delete Note
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            Column {
                if (state.noteType == NoteType.STORY) {
                    val activeBlock = state.blocks.getOrNull(state.activeBlockIndex)
                    val isSceneActive = activeBlock?.type == BlockType.NARRATOR
                    StoryCharacterBar(
                        characters = state.characters,
                        activeCharacterId = state.activeCharacterId,
                        storyViewMode = state.storyViewMode,
                        isSceneActive = isSceneActive,
                        onSelectCharacter = { charId -> viewModel.setActiveCharacter(charId) },
                        onEditCharacter = { char ->
                            characterToEdit = char
                            showCharacterDialog = true
                        },
                        onAddCharacterClick = {
                            characterToEdit = null
                            showCharacterDialog = true
                        },
                        onNarratorClick = { viewModel.toggleActiveBlockNarrator() },
                        onToggleViewMode = { viewModel.toggleStoryViewMode() }
                    )
                }

                val activeType = state.blocks.getOrNull(state.activeBlockIndex)?.type ?: BlockType.PARAGRAPH
                RichFormattingToolbar(
                    activeBlockType = activeType,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onFormatClick = { type, colorHex ->
                        viewModel.applyFormatting(type, colorHex)
                    },
                    onListTypeClick = { type ->
                        viewModel.setBlockType(type)
                    },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Note Title
            BasicTextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .focusRequester(titleFocusRequester),
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 36.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        val firstBlock = state.blocks.firstOrNull()
                        if (firstBlock != null) {
                            blockFocusRequesters[firstBlock.id]?.requestFocus()
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (state.title.isEmpty()) {
                        Text(
                            text = if (state.noteType == NoteType.STORY) "Story Title" else "Title",
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                lineHeight = 36.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )

            // Metadata info
            Text(
                text = "Last edited ${DateFormatter.formatRelativeTime(state.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Content Blocks
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val lastIndex = state.blocks.lastIndex
                            if (lastIndex >= 0) {
                                val lastBlock = state.blocks[lastIndex]
                                if (lastBlock.text.isNotBlank()) {
                                    val defaultType = if (state.noteType == NoteType.STORY) BlockType.DIALOGUE else BlockType.PARAGRAPH
                                    viewModel.addNewBlockAfter(lastIndex, defaultType)
                                } else {
                                    blockFocusRequesters[lastBlock.id]?.requestFocus()
                                }
                            }
                        }
                    )
            ) {
                itemsIndexed(
                    items = state.blocks,
                    key = { _, block -> block.id }
                ) { index, block ->
                    val requester = blockFocusRequesters.getOrPut(block.id) { FocusRequester() }
                    val targetPos = if (state.focusRequest?.blockId == block.id) state.focusRequest?.cursorPosition else null

                    if (state.noteType == NoteType.STORY && (block.type == BlockType.DIALOGUE || block.type == BlockType.NARRATOR)) {
                        val character = state.characters.find { it.id == block.characterId }
                        StoryDialogueBlockItem(
                            block = block,
                            index = index,
                            character = character,
                            characters = state.characters,
                            storyViewMode = state.storyViewMode,
                            isFocused = state.activeBlockIndex == index,
                            focusRequester = requester,
                            onTextChanged = { text, selection ->
                                viewModel.onBlockTextChanged(index, text, selection)
                            },
                            onFocusGained = { selection ->
                                viewModel.onBlockFocusChanged(index, selection)
                            },
                            onEnterPressed = { before, after ->
                                viewModel.splitBlock(index, before, after)
                            },
                            onBackspaceOnEmpty = {
                                if (state.blocks.size > 1) {
                                    viewModel.removeBlockAt(index)
                                }
                            },
                            onSelectCharacter = { charId ->
                                viewModel.setBlockCharacter(index, charId)
                            },
                            onEditParenthetical = { parenthetical ->
                                viewModel.setBlockParenthetical(index, parenthetical)
                            },
                            onDeleteBlock = {
                                blockIndexToDelete = index
                            },
                            targetCursorPosition = targetPos
                        )
                    } else {
                        RichBlockItem(
                            block = block,
                            index = index,
                            isFocused = state.activeBlockIndex == index,
                            focusRequester = requester,
                            onTextChanged = { text, selection ->
                                viewModel.onBlockTextChanged(index, text, selection)
                            },
                            onFocusGained = { selection ->
                                viewModel.onBlockFocusChanged(index, selection)
                            },
                            onToggleChecked = { blockId ->
                                viewModel.toggleChecklist(blockId)
                            },
                            onEnterPressed = { before, after ->
                                viewModel.splitBlock(index, before, after)
                            },
                            onBackspaceOnEmpty = {
                                if (block.type != BlockType.PARAGRAPH) {
                                    viewModel.setBlockType(BlockType.PARAGRAPH)
                                } else if (state.blocks.size > 1) {
                                    viewModel.removeBlockAt(index)
                                }
                            },
                            targetCursorPosition = targetPos
                        )
                    }
                }

                // Bottom spacer for comfortable typing above toolbar
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Note?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This note will be permanently removed from your vault.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNote {
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    blockIndexToDelete?.let { index ->
        val block = state.blocks.getOrNull(index)
        val isScene = block?.type == BlockType.NARRATOR
        val char = state.characters.find { it.id == block?.characterId }
        val speakerName = if (isScene) "Scene beat" else (char?.name ?: "Speaker")

        AlertDialog(
            onDismissRequest = { blockIndexToDelete = null },
            title = {
                Text(
                    text = if (isScene) "Delete Scene Beat?" else "Delete Message?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val textSnippet = block?.text?.trim()
                if (!textSnippet.isNullOrBlank()) {
                    val preview = if (textSnippet.length > 70) textSnippet.take(70) + "..." else textSnippet
                    Text("Are you sure you want to delete this line by $speakerName?\n\n\"$preview\"")
                } else {
                    Text("Are you sure you want to delete this empty $speakerName line?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetIndex = index
                        blockIndexToDelete = null
                        viewModel.removeBlockAt(targetIndex)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = if (isScene) "Scene beat deleted" else "Message deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undo()
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { blockIndexToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showCharacterDialog) {
        CharacterManagementDialog(
            characterToEdit = characterToEdit,
            onDismiss = { showCharacterDialog = false },
            onSave = { name, emoji, colorHex, role ->
                val editing = characterToEdit
                if (editing != null) {
                    viewModel.updateCharacter(editing.copy(name = name, avatarEmoji = emoji, colorHex = colorHex, role = role))
                } else {
                    viewModel.addCharacter(name, emoji, colorHex, role)
                }
            },
            onDelete = { charId ->
                viewModel.deleteCharacter(charId)
            }
        )
    }
}
