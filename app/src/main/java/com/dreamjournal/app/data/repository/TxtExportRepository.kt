package com.dreamjournal.app.data.repository

import android.content.Context
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.domain.model.ExportFormat
import com.dreamjournal.app.domain.model.ContentBlockType
import com.dreamjournal.app.domain.model.EntryContentBlock
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.model.TodoItem
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class TxtExportRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun exportSingle(
        entry: DreamEntryEntity,
        format: ExportFormat = ExportFormat.TXT
    ): Result<File> = runCatching {
        exportEntries(
            entries = listOf(entry),
            baseName = "record_${entry.id}_${timestamp()}",
            documentName = "record_${entry.id}.${format.extension}",
            format = format
        )
    }

    fun exportAll(
        entries: List<DreamEntryEntity>,
        format: ExportFormat = ExportFormat.TXT
    ): Result<File> = runCatching {
        exportEntries(
            entries = entries,
            baseName = "records_all_${timestamp()}",
            documentName = "all_records.${format.extension}",
            format = format
        )
    }

    fun exportRange(
        entries: List<DreamEntryEntity>,
        startDate: String,
        endDate: String,
        format: ExportFormat = ExportFormat.TXT
    ): Result<File> = runCatching {
        exportEntries(
            entries = entries,
            baseName = "records_${startDate}_to_${endDate}_${timestamp()}",
            documentName = "records_${startDate}_to_${endDate}.${format.extension}",
            format = format
        )
    }

    fun exportedFilesSize(): Long = ensureRootDir()
        .walkTopDown()
        .filter(File::isFile)
        .sumOf(File::length)

    fun clearExportedFiles(): Result<Long> = runCatching {
        val root = ensureRootDir()
        val releasedBytes = root.walkTopDown().filter(File::isFile).sumOf(File::length)
        root.listFiles()?.forEach { it.deleteRecursively() }
        releasedBytes
    }

    private fun exportEntries(
        entries: List<DreamEntryEntity>,
        baseName: String,
        documentName: String,
        format: ExportFormat
    ): File {
        require(entries.isNotEmpty()) { "没有可导出的记录" }
        val sortedEntries = entries.sortedByDescending { it.createdAt }
        val exportRoot = ensureRootDir()
        val assets = collectAssets(sortedEntries)
        val content = when (format) {
            ExportFormat.MARKDOWN -> buildMarkdown(sortedEntries, assets)
            ExportFormat.TXT -> buildEntriesText(sortedEntries)
        }

        if (assets.isEmpty()) {
            return File(exportRoot, documentName).apply { writeText(content) }
        }

        val packageDir = File(exportRoot, baseName).apply { mkdirs() }
        File(packageDir, documentName).writeText(content)
        assets.forEach { asset ->
            val target = File(packageDir, asset.relativePath)
            target.parentFile?.mkdirs()
            asset.source.copyTo(target, overwrite = true)
        }

        val zipFile = File(exportRoot, "$baseName.zip")
        zipDirectory(packageDir, zipFile)
        packageDir.deleteRecursively()
        return zipFile
    }

    private fun buildMarkdown(entries: List<DreamEntryEntity>, assets: List<ExportAsset>): String {
        val assetsByEntry = assets.groupBy { it.entryId }
        return buildString {
            appendLine("# 瞬记导出")
            appendLine()
            appendLine("> 共 ${entries.size} 条记录 · 导出于 ${formatTime(System.currentTimeMillis())}")
            appendLine()
            appendLine("<a id=\"catalog\"></a>")
            appendLine("## 目录")
            appendLine()
            entries.forEach { entry ->
                appendLine("- [${escapeInline(entry.dreamDate)} · ${escapeInline(entry.title)}](#entry-${entry.id})")
            }
            entries.forEach { entry ->
                val entryAssets = assetsByEntry[entry.id].orEmpty()
                val photos = entryAssets.filter { it.kind == AssetKind.PHOTO }
                val audio = entryAssets.filter { it.kind == AssetKind.AUDIO }
                val todos = decodeTodoItems(entry.todoItems)
                val blocks = decodeContentBlocks(entry.contentBlocks)
                val photoAssetsBySource = photos.associateBy { it.source.absolutePath }

                appendLine()
                appendLine("---")
                appendLine()
                appendLine("<a id=\"entry-${entry.id}\"></a>")
                appendLine("## ${escapeInline(entry.title.ifBlank { "未命名记录" })}")
                appendLine()
                append("`${entry.dreamDate}` · `${RecordType.fromStorage(entry.recordType).titleLabel}` · `${formatTime(entry.createdAt)}`")
                entry.weatherText?.takeIf(String::isNotBlank)?.let { append(" · `${escapeInline(it)}`") }
                entry.locationText?.takeIf(String::isNotBlank)?.let { append(" · `${escapeInline(it)}`") }
                appendLine()
                entry.dreamTag.takeIf { it.isNotBlank() }?.let {
                    appendLine()
                    appendLine("标签：${it.split(',').joinToString(" · ") { tag -> "`${escapeInline(tag.trim())}`" }}")
                }
                appendLine()
                if (blocks.isNotEmpty()) {
                    blocks.forEach { block ->
                        when (block.type) {
                            ContentBlockType.TEXT -> block.text.takeIf(String::isNotBlank)?.let {
                                appendLine(it)
                                appendLine()
                            }
                            ContentBlockType.IMAGE -> photoAssetsBySource[block.path]?.let { asset ->
                                appendLine("<img src=\"${asset.relativePath}\" alt=\"正文图片\" width=\"${block.widthPercent.coerceIn(50, 100)}%\" />")
                                appendLine()
                            }
                        }
                    }
                } else {
                    appendLine(entry.content.ifBlank { "_暂无正文_" })
                }

                if (todos.isNotEmpty()) {
                    appendLine()
                    appendLine("### 待办")
                    appendLine()
                    todos.forEach { item ->
                        appendLine("- [${if (item.isDone) "x" else " "}] ${escapeInline(item.text)}")
                    }
                }
                if (!entry.transcript.isNullOrBlank()) {
                    appendLine()
                    appendLine("### 语音转写")
                    appendLine()
                    appendLine(entry.transcript)
                }
                if (photos.isNotEmpty() && blocks.none { it.type == ContentBlockType.IMAGE }) {
                    appendLine()
                    appendLine("### 照片")
                    appendLine()
                    photos.forEachIndexed { index, asset ->
                        appendLine("![${escapeInline(entry.title)} · 照片 ${index + 1}](${asset.relativePath})")
                        appendLine()
                    }
                }
                if (audio.isNotEmpty()) {
                    appendLine()
                    appendLine("### 录音")
                    appendLine()
                    audio.forEachIndexed { index, asset ->
                        appendLine("- [播放录音 ${index + 1}](${asset.relativePath})")
                    }
                }
                if (!entry.aiSummary.isNullOrBlank()) {
                    appendLine()
                    appendLine("### AI 分析")
                    appendLine()
                    appendLine(entry.aiSummary)
                    appendLine()
                    appendLine("> AI 内容仅作为整理与回顾参考。")
                }
                appendLine()
                appendLine("[返回目录](#catalog)")
            }
        }
    }

    private fun buildEntriesText(entries: List<DreamEntryEntity>): String {
        return entries.joinToString("\n\n------------------------\n\n") { entry ->
            val photoPaths = decodeStringList(entry.photoPaths)
            val audioPaths = audioPaths(entry)
            val todoItems = decodeTodoItems(entry.todoItems)
            buildString {
                appendLine("日期：${entry.dreamDate}")
                appendLine("记录类型：${RecordType.fromStorage(entry.recordType).titleLabel}")
                appendLine("创建时间：${formatTime(entry.createdAt)}")
                entry.weatherText?.takeIf(String::isNotBlank)?.let { appendLine("天气：$it") }
                entry.locationText?.takeIf(String::isNotBlank)?.let { appendLine("位置：$it") }
                appendLine("标题：${entry.title}")
                appendLine("标签：${entry.dreamTag}")
                appendLine("录音数量：${audioPaths.size}")
                appendLine("照片数量：${photoPaths.size}")
                appendLine()
                appendLine("正文：")
                appendLine(entry.content)
                if (!entry.transcript.isNullOrBlank()) {
                    appendLine()
                    appendLine("语音转写：")
                    appendLine(entry.transcript)
                }
                if (todoItems.isNotEmpty()) {
                    appendLine()
                    appendLine("待办事项：")
                    todoItems.forEach { item -> appendLine("- [${if (item.isDone) "x" else " "}] ${item.text}") }
                }
                if (!entry.aiSummary.isNullOrBlank()) {
                    appendLine()
                    appendLine("AI 分析：")
                    appendLine(entry.aiSummary)
                }
            }.trimEnd()
        }
    }

    private fun collectAssets(entries: List<DreamEntryEntity>): List<ExportAsset> {
        return entries.flatMap { entry ->
            val folder = "assets/entry_${entry.id}"
            val photoAssets = decodeStringList(entry.photoPaths).mapIndexedNotNull { index, path ->
                val source = File(path).takeIf(File::exists) ?: return@mapIndexedNotNull null
                val ext = source.extension.ifBlank { "jpg" }
                ExportAsset(entry.id, source, "$folder/photo_${index + 1}.$ext", AssetKind.PHOTO)
            }
            val audioAssets = audioPaths(entry).mapIndexedNotNull { index, path ->
                val source = File(path).takeIf(File::exists) ?: return@mapIndexedNotNull null
                val ext = source.extension.ifBlank { "amr" }
                ExportAsset(entry.id, source, "$folder/audio_${index + 1}.$ext", AssetKind.AUDIO)
            }
            photoAssets + audioAssets
        }
    }

    private fun audioPaths(entry: DreamEntryEntity): List<String> {
        return (listOfNotNull(entry.audioPath) + decodeStringList(entry.extraAudioPaths))
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeTodoItems(raw: String?): List<TodoItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(TodoItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeContentBlocks(raw: String?): List<EntryContentBlock> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(EntryContentBlock.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            sourceDir.walkTopDown().filter(File::isFile).forEach { file ->
                zipOut.putNextEntry(ZipEntry(file.relativeTo(sourceDir).invariantSeparatorsPath))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }

    private fun escapeInline(input: String): String {
        return input.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
    }

    private fun ensureRootDir(): File {
        return File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
    }

    private fun formatTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    private data class ExportAsset(
        val entryId: Long,
        val source: File,
        val relativePath: String,
        val kind: AssetKind
    )

    private enum class AssetKind { PHOTO, AUDIO }
}
