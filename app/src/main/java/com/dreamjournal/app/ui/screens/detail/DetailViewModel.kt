package com.dreamjournal.app.ui.screens.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreamjournal.app.data.local.AiMessageEntity
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.data.repository.AudioPlayerManager
import com.dreamjournal.app.data.repository.AudioRecorderManager
import com.dreamjournal.app.data.repository.DreamRepository
import com.dreamjournal.app.data.repository.EntryAssetRepository
import com.dreamjournal.app.data.repository.SettingsRepository
import com.dreamjournal.app.data.repository.TxtExportRepository
import com.dreamjournal.app.domain.ai.AiService
import com.dreamjournal.app.domain.model.AiChatMessage
import com.dreamjournal.app.domain.model.ContentBlockType
import com.dreamjournal.app.domain.model.EntryContentBlock
import com.dreamjournal.app.domain.model.EntryContentCodec
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.model.TodoItem
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

data class DetailUiState(
    val entry: DreamEntryEntity? = null,
    val availableTags: List<String> = emptyList(),
    val draftTitle: String = "",
    val draftTags: List<String> = emptyList(),
    val draftContent: String = "",
    val draftPhotoPaths: List<String> = emptyList(),
    val draftBlocks: List<EntryContentBlock> = listOf(EntryContentBlock.text()),
    val activeTextBlockId: Long? = null,
    val lastAppliedTranscript: String? = null,
    val draftTodos: List<TodoItem> = emptyList(),
    val messages: List<AiMessageEntity> = emptyList(),
    val chatInput: String = "",
    val isDirty: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isTranscribingAudio: Boolean = false,
    val isSendingChat: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val playingAudioPath: String? = null,
    val audioPaths: List<String> = emptyList(),
    val isRecordingMore: Boolean = false,
    val additionalRecordingSeconds: Int = 0,
    val additionalWaveformLevels: List<Float> = List(12) { 0.08f },
    val statusMessage: String? = null,
    val exportPath: String? = null,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val entryId: Long,
    private val dreamRepository: DreamRepository,
    private val settingsRepository: SettingsRepository,
    private val entryAssetRepository: EntryAssetRepository,
    private val aiService: AiService,
    private val txtExportRepository: TxtExportRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val audioRecorderManager: AudioRecorderManager
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val baseTags = listOf(
        "日常", "工作", "灵感", "旅行", "家人", "健康",
        "开心", "平静", "疲惫", "低落", "噩梦", "混乱"
    )

    private val baseState = combine(
        dreamRepository.observeEntry(entryId),
        dreamRepository.observeMessages(entryId),
        settingsRepository.settingsFlow
    ) { entry, messages, settings ->
        Triple(entry, messages, settings.customTags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(null, emptyList(), ""))

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()
    private val _deletedEvent = MutableSharedFlow<Unit>()
    val deletedEvent = _deletedEvent.asSharedFlow()
    private var additionalAmplitudeJob: Job? = null

    init {
        viewModelScope.launch {
            baseState.collect { (entry, messages, customTagsRaw) ->
                val availableTags = (baseTags + parseCustomTags(customTagsRaw)).distinct()
                _uiState.update { current ->
                    val storedBlocks = if (!current.isDirty) decodeContentBlocks(entry) else current.draftBlocks
                    val incomingTranscript = entry?.transcript.orEmpty()
                    val blocks = if (
                        current.isDirty &&
                        incomingTranscript.isNotBlank() &&
                        incomingTranscript != current.lastAppliedTranscript
                    ) {
                        EntryContentCodec.appendTextIfMissing(storedBlocks, incomingTranscript)
                    } else storedBlocks
                    current.copy(
                        entry = entry,
                        messages = messages,
                        availableTags = availableTags,
                        draftTitle = if (!current.isDirty) entry?.title.orEmpty() else current.draftTitle,
                        draftTags = if (!current.isDirty) {
                            entry?.dreamTag.orEmpty()
                                .split(",")
                                .map { it.trim() }
                                .map { if (it == "快乐") "开心" else it }
                                .filter { it.isNotBlank() && it !in setOf("普通", "白天", "夜间") }
                        } else {
                            current.draftTags
                        },
                        draftContent = blocksToPlainText(blocks),
                        draftPhotoPaths = blocks.filter { it.type == ContentBlockType.IMAGE }.map { it.path },
                        draftBlocks = blocks,
                        activeTextBlockId = current.activeTextBlockId
                            ?: blocks.firstOrNull { it.type == ContentBlockType.TEXT }?.id,
                        lastAppliedTranscript = incomingTranscript.ifBlank { current.lastAppliedTranscript },
                        isDirty = current.isDirty || (current.isDirty && blocks != storedBlocks),
                        draftTodos = if (!current.isDirty) decodeTodoItems(entry?.todoItems) else current.draftTodos,
                        audioPaths = entry?.let {
                            (listOfNotNull(it.audioPath) + decodeStringList(it.extraAudioPaths)).distinct()
                        }.orEmpty()
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(draftTitle = value, isDirty = true, statusMessage = null) }
    }

    fun onDraftChange(value: String) {
        val firstTextId = uiState.value.draftBlocks.firstOrNull { it.type == ContentBlockType.TEXT }?.id
        if (firstTextId != null) updateTextBlock(firstTextId, value)
    }

    fun updateTextBlock(id: Long, value: String) {
        _uiState.update { current ->
            val blocks = current.draftBlocks.map { block ->
                if (block.id == id && block.type == ContentBlockType.TEXT) block.copy(text = value) else block
            }
            current.copy(
                draftBlocks = blocks,
                draftContent = blocksToPlainText(blocks),
                activeTextBlockId = id,
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun focusTextBlock(id: Long) {
        _uiState.update { it.copy(activeTextBlockId = id) }
    }

    fun setImageWidth(id: Long, widthPercent: Int) {
        _uiState.update { current ->
            current.copy(
                draftBlocks = current.draftBlocks.map { block ->
                    if (block.id == id && block.type == ContentBlockType.IMAGE) {
                        block.copy(widthPercent = widthPercent.coerceIn(50, 100))
                    } else block
                },
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun onTagToggle(value: String) {
        _uiState.update { current ->
            val nextTags = current.draftTags.toMutableList().apply {
                if (contains(value)) remove(value) else add(value)
            }
            current.copy(draftTags = nextTags.distinct(), isDirty = true, statusMessage = null)
        }
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = mutableListOf<String>()
            uris.forEach { uri ->
                entryAssetRepository.importPhoto(uri)
                    .onSuccess { imported += it }
                    .onFailure { error ->
                        _uiState.update { current ->
                            current.copy(errorMessage = "插入照片失败：${error.message}")
                        }
                    }
            }
            if (imported.isNotEmpty()) {
                _uiState.update { current ->
                    val next = insertImagesAfterActiveBlock(current, imported)
                    current.copy(
                        draftBlocks = next,
                        draftPhotoPaths = next.filter { it.type == ContentBlockType.IMAGE }.map { it.path },
                        draftContent = blocksToPlainText(next),
                        activeTextBlockId = next.lastOrNull { it.type == ContentBlockType.TEXT }?.id,
                        isDirty = true,
                        statusMessage = "已插入 ${imported.size} 张照片"
                    )
                }
            }
        }
    }

    fun removePhoto(path: String) {
        entryAssetRepository.deletePhoto(path)
        _uiState.update { current ->
            val next = current.draftBlocks.filterNot { it.type == ContentBlockType.IMAGE && it.path == path }
            current.copy(
                draftBlocks = normalizeBlocks(next),
                draftPhotoPaths = current.draftPhotoPaths.filterNot { it == path },
                isDirty = true,
                statusMessage = "已移除照片"
            )
        }
    }

    fun addTodo() {
        val newTodo = TodoItem(id = System.currentTimeMillis(), text = "")
        _uiState.update { current ->
            current.copy(
                draftTodos = current.draftTodos + newTodo,
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun updateTodoText(id: Long, text: String) {
        _uiState.update { current ->
            current.copy(
                draftTodos = current.draftTodos.map { todo ->
                    if (todo.id == id) todo.copy(text = text) else todo
                },
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun toggleTodo(id: Long) {
        _uiState.update { current ->
            current.copy(
                draftTodos = current.draftTodos.map { todo ->
                    if (todo.id == id) todo.copy(isDone = !todo.isDone) else todo
                },
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun removeTodo(id: Long) {
        _uiState.update { current ->
            current.copy(
                draftTodos = current.draftTodos.filterNot { it.id == id },
                isDirty = true,
                statusMessage = null
            )
        }
    }

    fun saveDraft() {
        val title = uiState.value.draftTitle.trim()
        val tag = uiState.value.draftTags.joinToString(", ")
        val content = uiState.value.draftContent
        val photoPaths = json.encodeToString(
            ListSerializer(String.serializer()),
            uiState.value.draftBlocks.filter { it.type == ContentBlockType.IMAGE }.map { it.path }
        )
        val contentBlocks = json.encodeToString(
            ListSerializer(EntryContentBlock.serializer()),
            uiState.value.draftBlocks
        )
        val todoItems = json.encodeToString(
            ListSerializer(TodoItem.serializer()),
            uiState.value.draftTodos.filter { it.text.isNotBlank() }
        )
        viewModelScope.launch {
            runCatching {
                dreamRepository.updateEntry(entryId, title, content, tag, photoPaths, todoItems, contentBlocks)
            }.onSuccess {
                _uiState.update {
                    it.copy(isDirty = false, statusMessage = "保存成功", errorMessage = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = "保存失败：${error.message}")
                }
            }
        }
    }

    fun runAnalysis() {
        val content = uiState.value.draftContent.ifBlank { uiState.value.entry?.content.orEmpty() }
        val recordType = RecordType.fromStorage(uiState.value.entry?.recordType)
        if (content.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = if (recordType == RecordType.DAY) "请先填写白天正文再分析" else "请先填写夜间正文再分析")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null, statusMessage = null) }
            aiService.analyze(content, recordType)
                .onSuccess { result ->
                    dreamRepository.saveAnalysis(entryId, result)
                    dreamRepository.appendAiMessage(
                        entryId = entryId,
                        role = "assistant",
                        message = result.summary
                    )
                    _uiState.update { it.copy(statusMessage = "AI 分析完成") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "AI 分析失败：${error.message}")
                    }
                }
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    fun onChatInputChange(value: String) {
        _uiState.update { it.copy(chatInput = value) }
    }

    fun sendChat() {
        val message = uiState.value.chatInput.trim()
        if (message.isBlank()) return

        val dreamContent = uiState.value.draftContent.ifBlank { uiState.value.entry?.content.orEmpty() }
        val recordType = RecordType.fromStorage(uiState.value.entry?.recordType)

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingChat = true, chatInput = "") }
            dreamRepository.appendAiMessage(entryId, "user", message)

            val history = uiState.value.messages.map {
                AiChatMessage(role = it.role, content = it.message)
            }

            aiService.chat(
                dreamContent = dreamContent,
                history = history,
                userMessage = message,
                recordType = recordType
            ).onSuccess { reply ->
                dreamRepository.appendAiMessage(entryId, "assistant", reply)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "对话失败：${error.message}") }
            }

            _uiState.update { it.copy(isSendingChat = false) }
        }
    }

    fun toggleAudioPlayback(path: String) {
        if (uiState.value.playingAudioPath == path) {
            audioPlayerManager.stop()
            _uiState.update { it.copy(isPlayingAudio = false, playingAudioPath = null) }
            return
        }

        audioPlayerManager.play(path) {
            _uiState.update { it.copy(isPlayingAudio = false, playingAudioPath = null) }
        }.onSuccess {
            _uiState.update { it.copy(isPlayingAudio = true, playingAudioPath = path) }
        }.onFailure { error ->
            _uiState.update { it.copy(errorMessage = "录音播放失败：${error.message}") }
        }
    }

    fun startAdditionalRecording() {
        if (uiState.value.isRecordingMore) return
        audioPlayerManager.stop()
        audioRecorderManager.start()
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isRecordingMore = true,
                        additionalRecordingSeconds = 0,
                        additionalWaveformLevels = List(12) { 0.08f },
                        isPlayingAudio = false,
                        playingAudioPath = null,
                        statusMessage = null,
                        errorMessage = null
                    )
                }
                startAdditionalAmplitudeMonitoring()
            }
            .onFailure { error ->
                _uiState.update { it.copy(errorMessage = "开始追加录音失败：${error.message}") }
            }
    }

    fun stopAdditionalRecording() {
        if (!uiState.value.isRecordingMore) return
        additionalAmplitudeJob?.cancel()
        additionalAmplitudeJob = null
        _uiState.update {
            it.copy(
                isRecordingMore = false,
                additionalRecordingSeconds = 0,
                additionalWaveformLevels = List(12) { 0.08f }
            )
        }

        audioRecorderManager.stop()
            .onFailure { error ->
                _uiState.update { it.copy(errorMessage = "保存追加录音失败：${error.message}") }
            }
            .onSuccess { path ->
                if (path.isNullOrBlank()) {
                    _uiState.update { it.copy(errorMessage = "没有生成有效录音文件") }
                    return@onSuccess
                }
                viewModelScope.launch {
                    dreamRepository.addAudioClip(entryId, path)
                    _uiState.update { it.copy(statusMessage = "录音已追加，正在转成文字…", isTranscribingAudio = true) }
                    aiService.transcribe(File(path))
                        .onSuccess { transcript ->
                            if (transcript.isNotBlank()) {
                                _uiState.update { current ->
                                    val blocks = EntryContentCodec.appendTextIfMissing(current.draftBlocks, transcript)
                                    current.copy(
                                        draftBlocks = blocks,
                                        draftContent = blocksToPlainText(blocks),
                                        isDirty = true,
                                        statusMessage = "已追加录音并转成文字"
                                    )
                                }
                                saveDraft()
                            } else {
                                _uiState.update { it.copy(statusMessage = "录音已保存，可稍后重新转写") }
                            }
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(statusMessage = "录音已保存；转写失败：${error.message}") }
                        }
                    _uiState.update { it.copy(isTranscribingAudio = false) }
                }
            }
    }

    fun exportSingle() {
        val entry = uiState.value.entry ?: return
        val result = txtExportRepository.exportSingle(entry)
        result.onSuccess { file ->
            _uiState.update { it.copy(exportPath = file.absolutePath, errorMessage = null) }
        }.onFailure { error ->
            _uiState.update { it.copy(errorMessage = "导出失败：${error.message}") }
        }
    }

    fun transcribeAudio(audioPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribingAudio = true, errorMessage = null, statusMessage = null) }
            aiService.transcribe(File(audioPath))
                .onSuccess { transcript ->
                    if (transcript.isNotBlank()) {
                        dreamRepository.attachTranscript(entryId, transcript)
                        _uiState.update { current ->
                            val blocks = EntryContentCodec.appendTextIfMissing(current.draftBlocks, transcript)
                            current.copy(
                                draftBlocks = blocks,
                                draftContent = EntryContentCodec.plainText(blocks),
                                lastAppliedTranscript = transcript,
                                isDirty = true,
                                statusMessage = "转写完成，文字已加入正文"
                            )
                        }
                    } else {
                        _uiState.update { it.copy(errorMessage = "没有拿到可用的转写结果") }
                    }
                }
                .onFailure { error ->
                    val detail = buildString {
                        append(error.message ?: error.toString())
                        error.cause?.message?.takeIf { it.isNotBlank() }?.let {
                            append("；原因：")
                            append(it)
                        }
                    }
                    _uiState.update { it.copy(errorMessage = "语音转文字失败：$detail") }
                }
            _uiState.update { it.copy(isTranscribingAudio = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun deleteCurrentEntry() {
        viewModelScope.launch {
            runCatching { dreamRepository.deleteEntryById(entryId) }
                .onSuccess { _deletedEvent.emit(Unit) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "删除失败：${error.message}") }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        additionalAmplitudeJob?.cancel()
        if (uiState.value.isRecordingMore) audioRecorderManager.cancel()
        audioPlayerManager.stop()
    }

    private fun startAdditionalAmplitudeMonitoring() {
        additionalAmplitudeJob?.cancel()
        additionalAmplitudeJob = viewModelScope.launch {
            while (uiState.value.isRecordingMore) {
                val amplitude = audioRecorderManager.currentAmplitude()
                val normalized = (amplitude.coerceAtLeast(0).toFloat() / 32767f).coerceIn(0f, 1f)
                val shaped = (0.12f + normalized * 0.88f).coerceIn(0.08f, 1f)
                _uiState.update { current ->
                    current.copy(
                        additionalRecordingSeconds = audioRecorderManager.currentDurationSeconds(),
                        additionalWaveformLevels = (current.additionalWaveformLevels + shaped).takeLast(12)
                    )
                }
                delay(120)
            }
        }
    }

    private fun parseCustomTags(raw: String): List<String> {
        return raw.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun decodePhotoPaths(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeContentBlocks(entry: DreamEntryEntity?): List<EntryContentBlock> {
        if (entry == null) return listOf(EntryContentBlock.text())
        return EntryContentCodec.decode(
            entry.contentBlocks,
            entry.content.ifBlank { entry.transcript.orEmpty() },
            entry.photoPaths
        )
    }

    private fun insertImagesAfterActiveBlock(
        state: DetailUiState,
        paths: List<String>
    ): List<EntryContentBlock> {
        val blocks = state.draftBlocks.toMutableList()
        val activeIndex = blocks.indexOfFirst { it.id == state.activeTextBlockId }.takeIf { it >= 0 }
            ?: blocks.indexOfLast { it.type == ContentBlockType.TEXT }.coerceAtLeast(0)
        var insertAt = (activeIndex + 1).coerceAtMost(blocks.size)
        paths.forEach { path ->
            blocks.add(insertAt++, EntryContentBlock.image(path = path))
            blocks.add(insertAt++, EntryContentBlock.text())
        }
        return EntryContentCodec.normalize(blocks)
    }

    private fun normalizeBlocks(blocks: List<EntryContentBlock>): List<EntryContentBlock> {
        return EntryContentCodec.normalize(blocks)
    }

    private fun blocksToPlainText(blocks: List<EntryContentBlock>): String = EntryContentCodec.plainText(blocks)

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

    companion object {
        fun factory(
            entryId: Long,
            dreamRepository: DreamRepository,
            settingsRepository: SettingsRepository,
            entryAssetRepository: EntryAssetRepository,
            aiService: AiService,
            txtExportRepository: TxtExportRepository,
            audioPlayerManager: AudioPlayerManager,
            audioRecorderManager: AudioRecorderManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(
                        entryId = entryId,
                        dreamRepository = dreamRepository,
                        settingsRepository = settingsRepository,
                        entryAssetRepository = entryAssetRepository,
                        aiService = aiService,
                        txtExportRepository = txtExportRepository,
                        audioPlayerManager = audioPlayerManager,
                        audioRecorderManager = audioRecorderManager
                    ) as T
                }
            }
        }
    }
}
