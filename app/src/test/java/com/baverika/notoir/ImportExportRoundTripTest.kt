package com.baverika.notoir

import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.domain.model.NoteColor
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichContent
import com.baverika.notoir.domain.model.RichSpan
import com.baverika.notoir.domain.model.SpanType
import com.baverika.notoir.util.export.JsonExporter
import com.baverika.notoir.util.import.DuplicateStrategy
import com.baverika.notoir.util.import.JsonImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ImportExportRoundTripTest {

    @Test
    fun `full round trip preserves all rich text formatting, colors, and checklist states`() {
        val originalNote = Note(
            id = "note-uuid-123",
            title = "Meeting Notes & Strategy",
            content = RichContent(
                blocks = listOf(
                    RichBlock(
                        id = "block-1",
                        type = BlockType.PARAGRAPH,
                        text = "This is a normal paragraph with RED TEXT in middle.",
                        spans = listOf(
                            RichSpan(start = 0, end = 4, type = SpanType.BOLD),
                            RichSpan(start = 32, end = 40, type = SpanType.COLOR, colorHex = "#EF4444")
                        )
                    ),
                    RichBlock(
                        id = "block-2",
                        type = BlockType.CHECKLIST,
                        text = "Complete quarterly report",
                        isChecked = true,
                        spans = listOf(
                            RichSpan(start = 0, end = 8, type = SpanType.UNDERLINE)
                        )
                    ),
                    RichBlock(
                        id = "block-3",
                        type = BlockType.CHECKLIST,
                        text = "Draft next sprint goals",
                        isChecked = false,
                        spans = emptyList()
                    ),
                    RichBlock(
                        id = "block-4",
                        type = BlockType.BULLET,
                        text = "Key objective: offline stability",
                        spans = listOf(
                            RichSpan(start = 15, end = 32, type = SpanType.ITALIC)
                        )
                    )
                )
            ),
            color = NoteColor.AMBER,
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L,
            isPinned = true
        )

        val jsonString = JsonExporter.exportNotesToString(listOf(originalNote))
        assertTrue(jsonString.contains("Meeting Notes & Strategy"))
        assertTrue(jsonString.contains("formatVersion"))

        val importResult = JsonImporter.importNotesFromString(
            jsonString = jsonString,
            existingNotes = emptyList(),
            strategy = DuplicateStrategy.CREATE_NEW
        )

        assertTrue(importResult.isSuccess)
        val importedNotes = importResult.getOrThrow().finalNotesToSave
        assertEquals(1, importedNotes.size)

        val imported = importedNotes.first()
        assertEquals(originalNote.id, imported.id)
        assertEquals(originalNote.title, imported.title)
        assertEquals(originalNote.color, imported.color)
        assertEquals(originalNote.createdAt, imported.createdAt)
        assertEquals(originalNote.updatedAt, imported.updatedAt)
        assertEquals(originalNote.isPinned, imported.isPinned)

        // Verify rich blocks
        assertEquals(4, imported.content.blocks.size)
        val block1 = imported.content.blocks[0]
        assertEquals(BlockType.PARAGRAPH, block1.type)
        assertEquals("This is a normal paragraph with RED TEXT in middle.", block1.text)
        assertEquals(2, block1.spans.size)
        assertEquals(SpanType.BOLD, block1.spans[0].type)
        assertEquals(SpanType.COLOR, block1.spans[1].type)
        assertEquals("#EF4444", block1.spans[1].colorHex)

        // Verify checklist states
        val block2 = imported.content.blocks[1]
        assertEquals(BlockType.CHECKLIST, block2.type)
        assertTrue(block2.isChecked)

        val block3 = imported.content.blocks[2]
        assertEquals(BlockType.CHECKLIST, block3.type)
        assertFalse(block3.isChecked)
    }

    @Test
    fun `duplicate strategy CREATE_NEW assigns fresh IDs`() {
        val existingNote = Note(
            id = "id-1",
            title = "Existing Task",
            content = RichContent(listOf(RichBlock(text = "Existing content")))
        )

        val exportedJson = JsonExporter.exportNotesToString(listOf(existingNote))

        val result = JsonImporter.importNotesFromString(
            jsonString = exportedJson,
            existingNotes = listOf(existingNote),
            strategy = DuplicateStrategy.CREATE_NEW
        ).getOrThrow()

        assertEquals(1, result.importedCount)
        assertEquals(0, result.replacedCount)
        assertEquals(0, result.skippedCount)
        assertNotEquals("id-1", result.finalNotesToSave.first().id)
        assertEquals("Existing Task", result.finalNotesToSave.first().title)
    }

    @Test
    fun `duplicate strategy REPLACE_EXISTING overwrites matching note`() {
        val existingNote = Note(
            id = "id-1",
            title = "Old Note",
            content = RichContent(listOf(RichBlock(text = "Old content")))
        )

        val updatedNote = Note(
            id = "id-1",
            title = "Old Note",
            content = RichContent(listOf(RichBlock(text = "Brand new updated content")))
        )

        val exportedJson = JsonExporter.exportNotesToString(listOf(updatedNote))

        val result = JsonImporter.importNotesFromString(
            jsonString = exportedJson,
            existingNotes = listOf(existingNote),
            strategy = DuplicateStrategy.REPLACE_EXISTING
        ).getOrThrow()

        assertEquals(0, result.importedCount)
        assertEquals(1, result.replacedCount)
        assertEquals(0, result.skippedCount)
        assertEquals("id-1", result.finalNotesToSave.first().id)
        assertEquals("Brand new updated content", result.finalNotesToSave.first().content.blocks.first().text)
    }

    @Test
    fun `duplicate strategy SKIP_DUPLICATES skips duplicate notes`() {
        val existingNote = Note(
            id = "id-1",
            title = "Identical Title",
            content = RichContent(listOf(RichBlock(text = "Test")))
        )

        val exportedJson = JsonExporter.exportNotesToString(listOf(existingNote))

        val result = JsonImporter.importNotesFromString(
            jsonString = exportedJson,
            existingNotes = listOf(existingNote),
            strategy = DuplicateStrategy.SKIP_DUPLICATES
        ).getOrThrow()

        assertEquals(0, result.importedCount)
        assertEquals(0, result.replacedCount)
        assertEquals(1, result.skippedCount)
        assertTrue(result.finalNotesToSave.isEmpty())
    }

    @Test
    fun `import handles empty and malformed JSON gracefully`() {
        val emptyResult = JsonImporter.importNotesFromString("", emptyList(), DuplicateStrategy.CREATE_NEW)
        assertTrue(emptyResult.isFailure)

        val malformedResult = JsonImporter.importNotesFromString("{ not valid json", emptyList(), DuplicateStrategy.CREATE_NEW)
        assertTrue(malformedResult.isFailure)

        val unsupportedVersionJson = """{"formatVersion": 999, "notes": []}"""
        val versionResult = JsonImporter.importNotesFromString(unsupportedVersionJson, emptyList(), DuplicateStrategy.CREATE_NEW)
        assertTrue(versionResult.isFailure)
    }
}
