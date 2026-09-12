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
import com.baverika.notoir.ui.editor.components.RichBlockItem
import com.baverika.notoir.ui.editor.components.RichFormattingToolbar
import com.baverika.notoir.util.time.DateFormatter
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
    var showColorDropdown by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Autofocus title on new note creation
    LaunchedEffect(state.isNewNote) {
        if (state.isNewNote && state.title.isEmpty()) {
            titleFocusRequester.requestFocus()
        }
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
        topBar = {
            TopAppBar(
                title = {},
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
                            text = "Title",
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

            // Rich Text Content Blocks
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // If user taps empty space below blocks, focus last block or add new block
                            val lastIndex = state.blocks.lastIndex
                            if (lastIndex >= 0) {
                                val lastBlock = state.blocks[lastIndex]
                                if (lastBlock.text.isNotBlank()) {
                                    viewModel.addNewBlockAfter(lastIndex)
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
                        onEnterPressed = {
                            // Continue list/checklist or create normal block
                            val nextType = if (block.type == BlockType.CHECKLIST || block.type == BlockType.BULLET || block.type == BlockType.NUMBERED) {
                                block.type
                            } else {
                                BlockType.PARAGRAPH
                            }
                            viewModel.addNewBlockAfter(index, nextType)
                            scope.launch {
                                listState.animateScrollToItem(index + 1)
                            }
                        },
                        onBackspaceOnEmpty = {
                            if (block.type != BlockType.PARAGRAPH) {
                                viewModel.setBlockType(BlockType.PARAGRAPH)
                            } else if (state.blocks.size > 1) {
                                viewModel.removeBlockAt(index)
                            }
                        }
                    )
                }

                // Bottom spacer for comfortable typing above toolbar
                item {
                    Spacer(modifier = Modifier.height(60.dp))
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
}
