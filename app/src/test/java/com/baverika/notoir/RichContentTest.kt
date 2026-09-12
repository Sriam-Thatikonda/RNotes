package com.baverika.notoir

import com.baverika.notoir.domain.model.BlockType
import com.baverika.notoir.domain.model.RichBlock
import com.baverika.notoir.domain.model.RichContent
import com.baverika.notoir.domain.model.RichSpan
import com.baverika.notoir.domain.model.SpanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichContentTest {

    @Test
    fun `toggleChecklist toggles target item state while preserving formatting and other blocks`() {
        val block1 = RichBlock(
            id = "b1",
            type = BlockType.CHECKLIST,
            text = "Buy organic milk",
            isChecked = false,
            spans = listOf(RichSpan(start = 4, end = 11, type = SpanType.BOLD))
        )
        val block2 = RichBlock(
            id = "b2",
            type = BlockType.CHECKLIST,
            text = "Clean the workspace",
            isChecked = true,
            spans = emptyList()
        )

        val content = RichContent(listOf(block1, block2))

        // Check stats before toggle
        assertEquals(Pair(1, 2), content.checklistStats())

        // Toggle block1
        val updated = content.toggleChecklist("b1")

        val updatedB1 = updated.blocks.first { it.id == "b1" }
        val updatedB2 = updated.blocks.first { it.id == "b2" }

        assertTrue("Block1 should now be checked", updatedB1.isChecked)
        assertTrue("Block2 should remain checked", updatedB2.isChecked)
        assertEquals("Spans on Block1 must be preserved", 1, updatedB1.spans.size)
        assertEquals(SpanType.BOLD, updatedB1.spans[0].type)
        assertEquals(Pair(2, 2), updated.checklistStats())

        // Toggle block1 again to uncheck
        val unchecked = updated.toggleChecklist("b1")
        assertFalse(unchecked.blocks.first { it.id == "b1" }.isChecked)
    }

    @Test
    fun `checklistStats returns correct completed and total counts`() {
        val blocks = listOf(
            RichBlock(type = BlockType.PARAGRAPH, text = "Header"),
            RichBlock(type = BlockType.CHECKLIST, text = "Task 1", isChecked = true),
            RichBlock(type = BlockType.CHECKLIST, text = "Task 2", isChecked = false),
            RichBlock(type = BlockType.CHECKLIST, text = "Task 3", isChecked = true),
            RichBlock(type = BlockType.BULLET, text = "Bullet note")
        )
        val content = RichContent(blocks)

        assertTrue(content.hasChecklists())
        assertEquals(Pair(2, 3), content.checklistStats())
    }

    @Test
    fun `fromPlainText parses markdown-style checklists, bullets, and numbered items`() {
        val plain = """
            - [ ] Open r-notes
            - [x] Configure password
            • Read offline docs
            1. First step
            Simple paragraph
        """.trimIndent()

        val content = RichContent.fromPlainText(plain)
        assertEquals(5, content.blocks.size)

        assertEquals(BlockType.CHECKLIST, content.blocks[0].type)
        assertFalse(content.blocks[0].isChecked)
        assertEquals("Open r-notes", content.blocks[0].text)

        assertEquals(BlockType.CHECKLIST, content.blocks[1].type)
        assertTrue(content.blocks[1].isChecked)
        assertEquals("Configure password", content.blocks[1].text)

        assertEquals(BlockType.BULLET, content.blocks[2].type)
        assertEquals("Read offline docs", content.blocks[2].text)

        assertEquals(BlockType.NUMBERED, content.blocks[3].type)
        assertEquals("First step", content.blocks[3].text)

        assertEquals(BlockType.PARAGRAPH, content.blocks[4].type)
        assertEquals("Simple paragraph", content.blocks[4].text)
    }
}
