package com.dreamjournal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EntryContentBlock(
    val id: Long,
    val type: ContentBlockType,
    val text: String = "",
    val path: String = "",
    val widthPercent: Int = 100
) {
    companion object {
        fun text(id: Long = System.nanoTime(), value: String = "") = EntryContentBlock(
            id = id,
            type = ContentBlockType.TEXT,
            text = value
        )

        fun image(id: Long = System.nanoTime(), path: String, widthPercent: Int = 100) = EntryContentBlock(
            id = id,
            type = ContentBlockType.IMAGE,
            path = path,
            widthPercent = widthPercent
        )
    }
}

@Serializable
enum class ContentBlockType { TEXT, IMAGE }
