package com.baverika.notoir.domain.model

import java.util.UUID

enum class BlockType {
    PARAGRAPH,
    BULLET,
    NUMBERED,
    CHECKLIST
}

enum class SpanType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    COLOR
}

data class RichSpan(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val colorHex: String? = null
) {
    init {
        require(start >= 0) { "start must be >= 0, was $start" }
        require(end >= start) { "end must be >= start, was start=$start end=$end" }
    }
}

data class RichBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType = BlockType.PARAGRAPH,
    val text: String = "",
    val isChecked: Boolean = false,
    val spans: List<RichSpan> = emptyList()
) {
    fun toggleChecked(): RichBlock {
        return if (type == BlockType.CHECKLIST) {
            copy(isChecked = !isChecked)
        } else {
            this
        }
    }
}

data class RichContent(
    val blocks: List<RichBlock> = listOf(RichBlock())
) {
    fun toPlainText(): String {
        return blocks.joinToString("\n") { it.text }
    }

    fun hasChecklists(): Boolean {
        return blocks.any { it.type == BlockType.CHECKLIST }
    }

    fun checklistStats(): Pair<Int, Int> {
        val checklists = blocks.filter { it.type == BlockType.CHECKLIST }
        val checked = checklists.count { it.isChecked }
        return Pair(checked, checklists.size)
    }

    fun toggleChecklist(blockId: String): RichContent {
        return copy(
            blocks = blocks.map {
                if (it.id == blockId) it.toggleChecked() else it
            }
        )
    }

    companion object {
        fun fromPlainText(text: String): RichContent {
            if (text.isEmpty()) return RichContent(listOf(RichBlock()))
            val lines = text.split("\n")
            val blocks = lines.map { line ->
                when {
                    line.startsWith("- [x] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = line.removePrefix("- [x] "),
                        isChecked = true
                    )
                    line.startsWith("- [ ] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = line.removePrefix("- [ ] "),
                        isChecked = false
                    )
                    line.startsWith("• ") || line.startsWith("* ") -> RichBlock(
                        type = BlockType.BULLET,
                        text = line.substring(2)
                    )
                    line.matches(Regex("^\\d+\\.\\s.*")) -> {
                        val dotIndex = line.indexOf(". ")
                        RichBlock(
                            type = BlockType.NUMBERED,
                            text = line.substring(dotIndex + 2)
                        )
                    }
                    else -> RichBlock(
                        type = BlockType.PARAGRAPH,
                        text = line
                    )
                }
            }
            return RichContent(blocks)
        }
    }
}
