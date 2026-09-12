package com.baverika.notoir.domain.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: RichContent = RichContent(),
    val color: NoteColor = NoteColor.DEFAULT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
) {
    val displayTitle: String
        get() = if (title.isNotBlank()) title else "Untitled Note"

    val isBlank: Boolean
        get() = title.isBlank() && content.blocks.all { it.text.isBlank() }
}
