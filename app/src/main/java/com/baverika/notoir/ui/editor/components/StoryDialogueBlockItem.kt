package com.baverika.notoir.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.StoryCharacter
import com.baverika.notoir.ui.editor.StoryViewMode

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryDialogueBlockItem(
    block: RichBlock,
    index: Int,
    character: StoryCharacter?,
    characters: List<StoryCharacter>,
    storyViewMode: StoryViewMode,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onTextChanged: (String, TextRange) -> Unit,
    onFocusGained: (TextRange) -> Unit,
    onEnterPressed: (textBefore: String, textAfter: String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onEditParenthetical: (String?) -> Unit,
    onDeleteBlock: () -> Unit = {},
    targetCursorPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(block.id) {
        val initialCursor = targetCursorPosition?.coerceIn(0, block.text.length) ?: block.text.length
        mutableStateOf(TextFieldValue(text = block.text, selection = TextRange(initialCursor)))
    }

    var showAvatarMenu by remember { mutableStateOf(false) }

    if (textFieldValue.text != block.text) {
        val newSelection = if (targetCursorPosition != null) {
            TextRange(targetCursorPosition.coerceIn(0, block.text.length))
        } else {
            val start = textFieldValue.selection.start.coerceIn(0, block.text.length)
            val end = textFieldValue.selection.end.coerceIn(start, block.text.length)
            TextRange(start, end)
        }
        textFieldValue = textFieldValue.copy(text = block.text, selection = newSelection)
    }

    LaunchedEffect(targetCursorPosition) {
        targetCursorPosition?.let { pos ->
            val clamped = pos.coerceIn(0, textFieldValue.text.length)
            textFieldValue = textFieldValue.copy(selection = TextRange(clamped))
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            kotlinx.coroutines.delay(20)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val visualTransformation = remember(block.spans) {
        RichTextVisualTransformation(block.spans, forceStrikeThrough = false)
    }

    val parsedCharColor = try {
        Color(android.graphics.Color.parseColor(character?.colorHex ?: "#6366F1"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Determine bubble alignment: if 2 characters, char 0 on left, char 1 on right
    val isRightAligned = characters.size >= 2 && character?.id == characters.getOrNull(1)?.id

    when (block.type) {
        BlockType.NARRATOR -> {
            // Scene / Narrator Beat Block
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SCENE / NARRATOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }

                        if (isFocused) {
                            IconButton(
                                onClick = onDeleteBlock,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete Scene",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (newValue.text.contains('\n') && !textFieldValue.text.contains('\n')) {
                                    val newlineIndex = newValue.text.indexOf('\n')
                                    val before = newValue.text.substring(0, newlineIndex)
                                    val after = newValue.text.substring(newlineIndex + 1)
                                    textFieldValue = TextFieldValue(before, TextRange(before.length))
                                    onTextChanged(before, TextRange(before.length))
                                    onEnterPressed(before, after)
                                } else {
                                    textFieldValue = newValue
                                    onTextChanged(newValue.text, newValue.selection)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onFocusGained(textFieldValue.selection)
                                    }
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when {
                                            keyEvent.key == Key.Enter -> {
                                                val text = textFieldValue.text
                                                val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
                                                val before = text.substring(0, cursor)
                                                val after = text.substring(cursor)
                                                textFieldValue = TextFieldValue(before, TextRange(before.length))
                                                onTextChanged(before, TextRange(before.length))
                                                onEnterPressed(before, after)
                                                true
                                            }
                                            keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty() -> {
                                                onBackspaceOnEmpty()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = visualTransformation,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { onEnterPressed(textFieldValue.text, "") })
                        )

                        if (textFieldValue.text.isEmpty() && isFocused) {
                            Text(
                                text = "Describe the setting, action, or narrator note...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            )
                        }
                    }
                }
            }
        }

        else -> {
            // Dialogue Block
            if (storyViewMode == StoryViewMode.SCRIPT) {
                // Screenplay / Script Style
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Character Name and Delete button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = (character?.name ?: "SPEAKER").uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = parsedCharColor,
                            letterSpacing = 1.2.sp
                        )
                        if (isFocused) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onDeleteBlock,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete Message",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Parenthetical if any (e.g. "(whispering)")
                    if (!block.parenthetical.isNullOrBlank()) {
                        Text(
                            text = "(${block.parenthetical})",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Indented Dialogue Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (newValue.text.contains('\n') && !textFieldValue.text.contains('\n')) {
                                    val newlineIndex = newValue.text.indexOf('\n')
                                    val before = newValue.text.substring(0, newlineIndex)
                                    val after = newValue.text.substring(newlineIndex + 1)
                                    textFieldValue = TextFieldValue(before, TextRange(before.length))
                                    onTextChanged(before, TextRange(before.length))
                                    onEnterPressed(before, after)
                                } else {
                                    textFieldValue = newValue
                                    onTextChanged(newValue.text, newValue.selection)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onFocusGained(textFieldValue.selection)
                                    }
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when {
                                            keyEvent.key == Key.Enter -> {
                                                val text = textFieldValue.text
                                                val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
                                                val before = text.substring(0, cursor)
                                                val after = text.substring(cursor)
                                                textFieldValue = TextFieldValue(before, TextRange(before.length))
                                                onTextChanged(before, TextRange(before.length))
                                                onEnterPressed(before, after)
                                                true
                                            }
                                            keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty() -> {
                                                onBackspaceOnEmpty()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(parsedCharColor),
                            visualTransformation = visualTransformation,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { onEnterPressed(textFieldValue.text, "") })
                        )

                        if (textFieldValue.text.isEmpty() && isFocused) {
                            Text(
                                text = "Dialogue...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Modern Chat Bubble View
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!isRightAligned) {
                        // Left Avatar with Options Menu
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parsedCharColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, parsedCharColor, CircleShape)
                                    .clickable { showAvatarMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = character?.avatarEmoji ?: "👤",
                                    fontSize = 16.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showAvatarMenu,
                                onDismissRequest = { showAvatarMenu = false }
                            ) {
                                characters.forEach { char ->
                                    DropdownMenuItem(
                                        text = { Text("${char.avatarEmoji}  ${char.name}") },
                                        onClick = {
                                            onSelectCharacter(char.id)
                                            showAvatarMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete Message",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showAvatarMenu = false
                                        onDeleteBlock()
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Dialogue Bubble Card
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (!isRightAligned) 4.dp else 16.dp,
                            bottomEnd = if (isRightAligned) 4.dp else 16.dp
                        ),
                        color = parsedCharColor.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, parsedCharColor.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .widthIn(min = 130.dp, max = 320.dp)
                            .combinedClickable(
                                onClick = { focusRequester.requestFocus() },
                                onLongClick = { showAvatarMenu = true }
                            )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                            // Character Name, Parenthetical and Delete Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = character?.name ?: "Speaker",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = parsedCharColor
                                    )
                                    if (!block.parenthetical.isNullOrBlank()) {
                                        Text(
                                            text = "(${block.parenthetical})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }

                                if (isFocused) {
                                    IconButton(
                                        onClick = onDeleteBlock,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Message",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Editable Text Field inside bubble
                            Box(modifier = Modifier.fillMaxWidth()) {
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        if (newValue.text.contains('\n') && !textFieldValue.text.contains('\n')) {
                                            val newlineIndex = newValue.text.indexOf('\n')
                                            val before = newValue.text.substring(0, newlineIndex)
                                            val after = newValue.text.substring(newlineIndex + 1)
                                            textFieldValue = TextFieldValue(before, TextRange(before.length))
                                            onTextChanged(before, TextRange(before.length))
                                            onEnterPressed(before, after)
                                        } else {
                                            textFieldValue = newValue
                                            onTextChanged(newValue.text, newValue.selection)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                onFocusGained(textFieldValue.selection)
                                            }
                                        }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when {
                                                    keyEvent.key == Key.Enter -> {
                                                        val text = textFieldValue.text
                                                        val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
                                                        val before = text.substring(0, cursor)
                                                        val after = text.substring(cursor)
                                                        textFieldValue = TextFieldValue(before, TextRange(before.length))
                                                        onTextChanged(before, TextRange(before.length))
                                                        onEnterPressed(before, after)
                                                        true
                                                    }
                                                    keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty() -> {
                                                        onBackspaceOnEmpty()
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    ),
                                    cursorBrush = SolidColor(parsedCharColor),
                                    visualTransformation = visualTransformation,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                    keyboardActions = KeyboardActions(onDone = { onEnterPressed(textFieldValue.text, "") })
                                )

                                if (textFieldValue.text.isEmpty() && isFocused) {
                                    Text(
                                        text = "Dialogue...",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            lineHeight = 22.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (isRightAligned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // Right Avatar with Options Menu
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parsedCharColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, parsedCharColor, CircleShape)
                                    .clickable { showAvatarMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = character?.avatarEmoji ?: "👤",
                                    fontSize = 16.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showAvatarMenu,
                                onDismissRequest = { showAvatarMenu = false }
                            ) {
                                characters.forEach { char ->
                                    DropdownMenuItem(
                                        text = { Text("${char.avatarEmoji}  ${char.name}") },
                                        onClick = {
                                            onSelectCharacter(char.id)
                                            showAvatarMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete Message",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showAvatarMenu = false
                                        onDeleteBlock()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
