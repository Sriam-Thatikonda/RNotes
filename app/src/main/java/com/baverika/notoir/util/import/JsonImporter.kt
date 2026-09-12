package com.baverika.notoir.util.import

import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.util.export.NotoirExportContainer
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class DuplicateStrategy {
    CREATE_NEW,
    REPLACE_EXISTING,
    SKIP_DUPLICATES
}

data class ImportResult(
    val importedCount: Int,
    val replacedCount: Int,
    val skippedCount: Int,
    val finalNotesToSave: List<Note>
)

object JsonImporter {
    private val gson = Gson()
    private const val SUPPORTED_FORMAT_VERSION = 1

    fun importNotesFromStream(
        inputStream: InputStream,
        existingNotes: List<Note>,
        strategy: DuplicateStrategy
    ): Result<ImportResult> {
        return try {
            val content = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                reader.readText()
            }
            importNotesFromString(content, existingNotes, strategy)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importNotesFromString(
        jsonString: String,
        existingNotes: List<Note>,
        strategy: DuplicateStrategy
    ): Result<ImportResult> {
        if (jsonString.isBlank()) {
            return Result.failure(IllegalArgumentException("The selected file is empty."))
        }

        val container = try {
            gson.fromJson(jsonString, NotoirExportContainer::class.java)
        } catch (e: JsonSyntaxException) {
            return Result.failure(IllegalArgumentException("Malformed JSON data or corrupted file."))
        }

        if (container == null || container.notes == null) {
            return Result.failure(IllegalArgumentException("Invalid r-notes backup file format."))
        }

        if (container.formatVersion > SUPPORTED_FORMAT_VERSION) {
            return Result.failure(
                IllegalArgumentException("Unsupported backup version (${container.formatVersion}). Please update r-notes.")
            )
        }

        val existingById = existingNotes.associateBy { it.id }.toMutableMap()
        val existingByTitle = existingNotes.associateBy { it.title.trim().lowercase() }.toMutableMap()

        val notesToSave = mutableListOf<Note>()
        var importedCount = 0
        var replacedCount = 0
        var skippedCount = 0

        for (importedNote in container.notes) {
            val duplicateById = existingById[importedNote.id]
            val duplicateByTitle = if (importedNote.title.isNotBlank()) {
                existingByTitle[importedNote.title.trim().lowercase()]
            } else null

            val isDuplicate = duplicateById != null || duplicateByTitle != null
            val existingTarget = duplicateById ?: duplicateByTitle

            when {
                !isDuplicate -> {
                    notesToSave.add(importedNote)
                    importedCount++
                }
                strategy == DuplicateStrategy.CREATE_NEW -> {
                    // Generate new ID so existing note is not overwritten
                    val newNote = importedNote.copy(
                        id = UUID.randomUUID().toString(),
                        updatedAt = System.currentTimeMillis()
                    )
                    notesToSave.add(newNote)
                    importedCount++
                }
                strategy == DuplicateStrategy.REPLACE_EXISTING -> {
                    val replacedNote = importedNote.copy(
                        id = existingTarget!!.id,
                        updatedAt = System.currentTimeMillis()
                    )
                    notesToSave.add(replacedNote)
                    replacedCount++
                }
                strategy == DuplicateStrategy.SKIP_DUPLICATES -> {
                    skippedCount++
                }
            }
        }

        return Result.success(
            ImportResult(
                importedCount = importedCount,
                replacedCount = replacedCount,
                skippedCount = skippedCount,
                finalNotesToSave = notesToSave
            )
        )
    }
}
