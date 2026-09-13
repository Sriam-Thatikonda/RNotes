package com.baverika.notoir.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichSpan
import com.baverika.notoir.domain.model.SpanType

@Composable
fun RichBlockItem(
    block: RichBlock,
    index: Int,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onTextChanged: (String, TextRange) -> Unit,
    onFocusGained: (TextRange) -> Unit,
    onToggleChecked: (String) -> Unit,
    onEnterPressed: (textBefore: String, textAfter: String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    targetCursorPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(block.id) {
        val initialCursor = targetCursorPosition?.coerceIn(0, block.text.length) ?: block.text.length
        mutableStateOf(TextFieldValue(text = block.text, selection = TextRange(initialCursor)))
    }

    // Keep internal text in sync if external changes occur
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

    val visualTransformation = remember(block.spans, block.isChecked, block.type) {
        RichTextVisualTransformation(block.spans, block.type == BlockType.CHECKLIST && block.isChecked)
    }

    val textColor by animateColorAsState(
        targetValue = if (block.type == BlockType.CHECKLIST && block.isChecked) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(150),
        label = "textColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (block.type) {
            BlockType.CHECKLIST -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onToggleChecked(block.id) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (block.isChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = if (block.isChecked) "Checked" else "Unchecked",
                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            BlockType.BULLET -> {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            BlockType.NUMBERED -> {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 36.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            BlockType.PARAGRAPH,
            BlockType.DIALOGUE,
            BlockType.NARRATOR -> {
                // No prefix for standard paragraphs
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Handle enter press if newline is inserted
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
                        if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
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
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    lineHeight = 26.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val text = textFieldValue.text
                        onEnterPressed(text, "")
                    }
                )
            )

            if (textFieldValue.text.isEmpty() && isFocused) {
                Text(
                    text = when (block.type) {
                        BlockType.CHECKLIST -> "To-do item..."
                        BlockType.BULLET -> "List item..."
                        BlockType.NUMBERED -> "List item..."
                        BlockType.PARAGRAPH,
                        BlockType.DIALOGUE,
                        BlockType.NARRATOR -> "Type something..."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        lineHeight = 26.sp
                    )
                )
            }
        }
    }
}

class RichTextVisualTransformation(
    private val spans: List<RichSpan>,
    private val forceStrikeThrough: Boolean
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        if (forceStrikeThrough) {
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                0,
                raw.length
            )
        }

        for (span in spans) {
            val start = span.start.coerceIn(0, raw.length)
            val end = span.end.coerceIn(start, raw.length)
            if (start < end) {
                val style = when (span.type) {
                    SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    SpanType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    SpanType.COLOR -> {
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(span.colorHex ?: "#71717A"))
                        } catch (e: Exception) {
                            Color.Unspecified
                        }
                        SpanStyle(color = parsedColor)
                    }
                }
                builder.addStyle(style, start, end)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
