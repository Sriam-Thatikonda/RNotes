package com.baverika.notoir.util.export

import com.baverika.notoir.domain.model.Note
import com.google.gson.GsonBuilder
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

data class NotoirExportContainer(
    val formatVersion: Int = 1,
    val appName: String = "r-notes",
    val exportedAt: Long = System.currentTimeMillis(),
    val notesCount: Int,
    val notes: List<Note>
)

object JsonExporter {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun exportNotesToJson(notes: List<Note>, outputStream: OutputStream): Result<Int> {
        return try {
            val container = NotoirExportContainer(
                formatVersion = 1,
                appName = "r-notes",
                exportedAt = System.currentTimeMillis(),
                notesCount = notes.size,
                notes = notes
            )
            OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                gson.toJson(container, writer)
            }
            Result.success(notes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportNotesToString(notes: List<Note>): String {
        val container = NotoirExportContainer(
            formatVersion = 1,
            appName = "r-notes",
            exportedAt = System.currentTimeMillis(),
            notesCount = notes.size,
            notes = notes
        )
        return gson.toJson(container)
    }
}
