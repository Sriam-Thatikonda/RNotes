package com.baverika.notoir.ui.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.SpanType

val CuratedTextColors = listOf(
    Pair("Default", "#71717A"),
    Pair("Coral Red", "#EF4444"),
    Pair("Ocean Blue", "#3B82F6"),
    Pair("Sage Green", "#10B981"),
    Pair("Sunset Amber", "#F59E0B"),
    Pair("Lavender Purple", "#8B5CF6"),
    Pair("Nordic Teal", "#14B8A6")
)

@Composable
fun RichFormattingToolbar(
    activeBlockType: BlockType,
    canUndo: Boolean,
    canRedo: Boolean,
    onFormatClick: (SpanType, String?) -> Unit,
    onListTypeClick: (BlockType) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPalette by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Animated Color Palette Bar
            AnimatedVisibility(
                visible = showColorPalette,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Text Color:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for ((name, hex) in CuratedTextColors) {
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    onFormatClick(SpanType.COLOR, hex)
                                    showColorPalette = false
                                }
                        )
                    }
                }
            }

            // Main Formatting Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolbarIconButton(
                    icon = Icons.Rounded.FormatBold,
                    description = "Bold",
                    onClick = { onFormatClick(SpanType.BOLD, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatItalic,
                    description = "Italic",
                    onClick = { onFormatClick(SpanType.ITALIC, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatUnderlined,
                    description = "Underline",
                    onClick = { onFormatClick(SpanType.UNDERLINE, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatStrikethrough,
                    description = "Strikethrough",
                    onClick = { onFormatClick(SpanType.STRIKETHROUGH, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatColorText,
                    description = "Text Color",
                    isActive = showColorPalette,
                    onClick = { showColorPalette = !showColorPalette }
                )

                ToolbarDivider()

                ToolbarIconButton(
                    icon = Icons.Rounded.CheckBox,
                    description = "Checklist",
                    isActive = activeBlockType == BlockType.CHECKLIST,
                    onClick = { onListTypeClick(BlockType.CHECKLIST) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatListBulleted,
                    description = "Bullet List",
                    isActive = activeBlockType == BlockType.BULLET,
                    onClick = { onListTypeClick(BlockType.BULLET) }
                )

                ToolbarIconButton(
                    icon = Icons.Rounded.FormatListNumbered,
                    description = "Numbered List",
                    isActive = activeBlockType == BlockType.NUMBERED,
                    onClick = { onListTypeClick(BlockType.NUMBERED) }
                )

                ToolbarDivider()

                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    description = "Undo",
                    enabled = canUndo,
                    onClick = onUndoClick
                )

                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Rounded.Redo,
                    description = "Redo",
                    enabled = canRedo,
                    onClick = onRedoClick
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
