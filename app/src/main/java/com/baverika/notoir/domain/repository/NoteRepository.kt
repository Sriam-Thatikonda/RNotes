package com.baverika.notoir.domain.repository

import com.baverika.notoir.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun insertOrUpdateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun insertNotes(notes: List<Note>)
}
