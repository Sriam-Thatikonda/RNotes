package com.baverika.notoir

import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchTest {

    private val sampleNotes = listOf(
        Note(
            id = "1",
            title = "Grocery Shopping",
            content = RichContent(
                listOf(
                    RichBlock(type = BlockType.CHECKLIST, text = "Apples"),
                    RichBlock(type = BlockType.CHECKLIST, text = "Almond Milk")
                )
            )
        ),
        Note(
            id = "2",
            title = "Project Arch",
            content = RichContent(
                listOf(
                    RichBlock(text = "Designing the SQLite schema for local vault storage.")
                )
            )
        ),
        Note(
            id = "3",
            title = "Weekend Trip Ideas",
            content = RichContent(
                listOf(
                    RichBlock(text = "Visit the national park and hike the trail.")
                )
            )
        )
    )

    private fun searchInMemory(notes: List<Note>, query: String): List<Note> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return notes
        return notes.filter { note ->
            note.title.contains(trimmed, ignoreCase = true) ||
                    note.content.toPlainText().contains(trimmed, ignoreCase = true)
        }
    }

    @Test
    fun `search finds note by title with case insensitivity`() {
        val results = searchInMemory(sampleNotes, "grocery")
        assertEquals(1, results.size)
        assertEquals("Grocery Shopping", results.first().title)
    }

    @Test
    fun `search finds note by body content`() {
        val results = searchInMemory(sampleNotes, "sqlite")
        assertEquals(1, results.size)
        assertEquals("Project Arch", results.first().title)
    }

    @Test
    fun `search with partial match finds multiple notes`() {
        // "the" appears in body of Project Arch ("the") and Weekend Trip ("the")
        val results = searchInMemory(sampleNotes, "the")
        assertTrue(results.size >= 2)
    }

    @Test
    fun `search with empty or blank query returns all notes`() {
        val results = searchInMemory(sampleNotes, "   ")
        assertEquals(sampleNotes.size, results.size)
    }

    @Test
    fun `search with non-matching query returns empty list`() {
        val results = searchInMemory(sampleNotes, "NonExistentKeywordXYZ")
        assertTrue(results.isEmpty())
    }
}
