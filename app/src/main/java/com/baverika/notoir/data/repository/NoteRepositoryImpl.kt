package com.baverika.notoir.data.repository

import com.baverika.notoir.data.local.converter.Converters
import com.baverika.notoir.data.local.dao.NoteDao
import com.baverika.notoir.data.local.entity.NoteEntity
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.model.NoteColor
import com.baverika.notoir.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {
    private val converters = Converters()

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { list ->
            list.map { it.toDomain(converters) }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query.trim()).map { list ->
            list.map { it.toDomain(converters) }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain(converters)
    }

    override suspend fun insertOrUpdateNote(note: Note) {
        noteDao.insert(note.toEntity(converters))
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.delete(note.toEntity(converters))
    }

    override suspend fun insertNotes(notes: List<Note>) {
        noteDao.insertAll(notes.map { it.toEntity(converters) })
    }

    private fun NoteEntity.toDomain(converters: Converters): Note {
        val noteColor = try {
            NoteColor.valueOf(color)
        } catch (e: Exception) {
            NoteColor.DEFAULT
        }
        return Note(
            id = id,
            title = title,
            content = converters.toRichContent(contentJson),
            color = noteColor,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned
        )
    }

    private fun Note.toEntity(converters: Converters): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            contentJson = converters.fromRichContent(content),
            plainText = content.toPlainText(),
            color = color.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned
        )
    }
}
