package com.dreamjournal.app.domain.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EntryContentCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(
        rawBlocks: String?,
        legacyContent: String = "",
        legacyPhotoPaths: String? = null
    ): List<EntryContentBlock> {
        val stored = runCatching {
            json.decodeFromString(ListSerializer(EntryContentBlock.serializer()), rawBlocks.orEmpty())
        }.getOrDefault(emptyList())

        if (stored.isNotEmpty()) {
            val normalized = normalize(stored)
            if (plainText(normalized).isNotBlank() || legacyContent.isBlank()) return normalized
            return replaceFirstText(normalized, legacyContent)
        }

        val legacyPhotos = runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), legacyPhotoPaths.orEmpty())
        }.getOrDefault(emptyList())
        return normalize(buildList {
            add(EntryContentBlock.text(value = legacyContent))
            legacyPhotos.forEach { path ->
                add(EntryContentBlock.image(path = path))
                add(EntryContentBlock.text())
            }
        })
    }

    fun encode(blocks: List<EntryContentBlock>): String = json.encodeToString(normalize(blocks))

    fun plainText(blocks: List<EntryContentBlock>): String = blocks
        .filter { it.type == ContentBlockType.TEXT }
        .map { it.text.trim() }
        .filter(String::isNotBlank)
        .joinToString("\n\n")

    fun appendTextIfMissing(
        blocks: List<EntryContentBlock>,
        value: String
    ): List<EntryContentBlock> {
        val cleanValue = value.trim()
        if (cleanValue.isBlank()) return normalize(blocks)
        val currentText = plainText(blocks)
        if (currentText.contains(cleanValue)) return normalize(blocks)

        val result = normalize(blocks).toMutableList()
        val targetIndex = result.indexOfLast { it.type == ContentBlockType.TEXT }
        if (targetIndex >= 0) {
            val target = result[targetIndex]
            result[targetIndex] = target.copy(
                text = listOf(target.text.trimEnd(), cleanValue)
                    .filter(String::isNotBlank)
                    .joinToString("\n\n")
            )
        } else {
            result += EntryContentBlock.text(value = cleanValue)
        }
        return normalize(result)
    }

    fun normalize(blocks: List<EntryContentBlock>): List<EntryContentBlock> {
        val result = blocks
            .filterNot { it.type == ContentBlockType.IMAGE && it.path.isBlank() }
            .toMutableList()
        if (result.isEmpty() || result.first().type != ContentBlockType.TEXT) {
            result.add(0, EntryContentBlock.text())
        }
        if (result.last().type != ContentBlockType.TEXT) result += EntryContentBlock.text()
        return result
    }

    private fun replaceFirstText(
        blocks: List<EntryContentBlock>,
        text: String
    ): List<EntryContentBlock> {
        var replaced = false
        return blocks.map { block ->
            if (!replaced && block.type == ContentBlockType.TEXT) {
                replaced = true
                block.copy(text = text)
            } else block
        }
    }
}
