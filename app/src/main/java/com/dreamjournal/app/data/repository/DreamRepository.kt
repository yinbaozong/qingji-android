package com.dreamjournal.app.data.repository

import com.dreamjournal.app.data.local.AiMessageDao
import com.dreamjournal.app.data.local.AiMessageEntity
import com.dreamjournal.app.data.local.DreamEntryDao
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.domain.model.AiAnalysisResult
import com.dreamjournal.app.domain.model.EntryContentBlock
import com.dreamjournal.app.domain.model.EntryContentCodec
import com.dreamjournal.app.domain.model.RecordType
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class DreamRepository(
    private val dreamEntryDao: DreamEntryDao,
    private val aiMessageDao: AiMessageDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeEntries(): Flow<List<DreamEntryEntity>> = dreamEntryDao.observeAll()

    fun observeEntry(entryId: Long): Flow<DreamEntryEntity?> = dreamEntryDao.observeById(entryId)

    fun observeEntriesByDate(date: String): Flow<List<DreamEntryEntity>> = dreamEntryDao.observeByDate(date)

    fun observeMessages(entryId: Long): Flow<List<AiMessageEntity>> = aiMessageDao.observeByEntryId(entryId)

    suspend fun createEntry(
        audioPath: String?,
        recordType: RecordType,
        weatherText: String? = null,
        locationText: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val entry = DreamEntryEntity(
            createdAt = now,
            updatedAt = now,
            dreamDate = LocalDate.now().toString(),
            recordType = recordType.name,
            title = if (recordType == RecordType.DAY) "日常记录" else "梦境记录",
            dreamTag = "",
            content = "",
            photoPaths = "[]",
            todoItems = "[]",
            audioPath = audioPath,
            transcript = null,
            aiSummary = null,
            aiKeywords = null,
            aiEmotion = null,
            aiSuggestions = null,
            contentBlocks = json.encodeToString(listOf(EntryContentBlock.text())),
            weatherText = weatherText,
            locationText = locationText
        )
        return dreamEntryDao.insert(entry)
    }

    suspend fun createEntryWithContent(
        content: String,
        recordType: RecordType,
        weatherText: String? = null,
        locationText: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val text = content.trim()
        val entry = DreamEntryEntity(
            createdAt = now,
            updatedAt = now,
            dreamDate = LocalDate.now().toString(),
            recordType = recordType.name,
            title = if (recordType == RecordType.DAY) "日常记录" else "梦境记录",
            dreamTag = "",
            content = text,
            photoPaths = "[]",
            todoItems = "[]",
            audioPath = null,
            transcript = null,
            aiSummary = null,
            aiKeywords = null,
            aiEmotion = null,
            aiSuggestions = null,
            contentBlocks = json.encodeToString(listOf(EntryContentBlock.text(value = text))),
            weatherText = weatherText,
            locationText = locationText
        )
        return dreamEntryDao.insert(entry)
    }

    suspend fun updateEntry(
        entryId: Long,
        title: String,
        content: String,
        tag: String,
        photoPaths: String,
        todoItems: String,
        contentBlocks: String
    ) {
        val existing = dreamEntryDao.getById(entryId) ?: return
        dreamEntryDao.update(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                dreamTag = tag.ifBlank { existing.dreamTag },
                content = content,
                photoPaths = photoPaths,
                todoItems = todoItems,
                contentBlocks = contentBlocks,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun attachTranscript(entryId: Long, transcript: String) {
        val existing = dreamEntryDao.getById(entryId) ?: return
        val currentBlocks = EntryContentCodec.decode(
            rawBlocks = existing.contentBlocks,
            legacyContent = existing.content,
            legacyPhotoPaths = existing.photoPaths
        )
        val mergedBlocks = EntryContentCodec.appendTextIfMissing(currentBlocks, transcript)
        dreamEntryDao.update(
            existing.copy(
                transcript = transcript,
                content = EntryContentCodec.plainText(mergedBlocks),
                contentBlocks = EntryContentCodec.encode(mergedBlocks),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addAudioClip(entryId: Long, path: String) {
        val existing = dreamEntryDao.getById(entryId) ?: return
        val current = decodeStringList(existing.extraAudioPaths)
        dreamEntryDao.update(
            existing.copy(
                extraAudioPaths = json.encodeToString(
                    ListSerializer(String.serializer()),
                    (current + path).distinct()
                ),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun attachContext(entryId: Long, weatherText: String, locationText: String?) {
        val existing = dreamEntryDao.getById(entryId) ?: return
        dreamEntryDao.update(
            existing.copy(
                weatherText = existing.weatherText?.takeIf(String::isNotBlank) ?: weatherText,
                locationText = existing.locationText?.takeIf(String::isNotBlank) ?: locationText,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun saveAnalysis(entryId: Long, result: AiAnalysisResult) {
        val existing = dreamEntryDao.getById(entryId) ?: return
        dreamEntryDao.update(
            existing.copy(
                aiSummary = result.summary,
                aiKeywords = result.keywords.joinToString(", "),
                aiEmotion = result.emotion,
                aiSuggestions = result.suggestions.joinToString("\n"),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun appendAiMessage(entryId: Long, role: String, message: String) {
        aiMessageDao.insert(
            AiMessageEntity(
                entryId = entryId,
                role = role,
                message = message,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getEntryById(entryId: Long): DreamEntryEntity? = dreamEntryDao.getById(entryId)

    suspend fun getAllEntries(): List<DreamEntryEntity> = dreamEntryDao.getAll()

    suspend fun deleteEntryById(entryId: Long) {
        dreamEntryDao.getById(entryId)?.let { entry ->
            entry.audioPath?.let { File(it).takeIf(File::exists)?.delete() }
            runCatching {
                json.decodeFromString(ListSerializer(String.serializer()), entry.photoPaths)
            }.getOrDefault(emptyList()).forEach { path ->
                File(path).takeIf(File::exists)?.delete()
            }
            dreamEntryDao.deleteById(entryId)
        } ?: dreamEntryDao.deleteById(entryId)
    }
}
