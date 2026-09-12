package com.baverika.notoir.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["title"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentJson: String,
    val plainText: String,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false
)
