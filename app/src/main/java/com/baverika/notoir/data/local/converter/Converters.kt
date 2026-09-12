package com.baverika.notoir.data.local.converter

import androidx.room.TypeConverter
import com.baverika.notoir.domain.model.RichContent
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRichContent(content: RichContent?): String {
        return if (content == null) "" else gson.toJson(content)
    }

    @TypeConverter
    fun toRichContent(json: String?): RichContent {
        if (json.isNullOrBlank()) return RichContent()
        return try {
            gson.fromJson(json, RichContent::class.java) ?: RichContent()
        } catch (e: Exception) {
            RichContent.fromPlainText(json)
        }
    }
}
