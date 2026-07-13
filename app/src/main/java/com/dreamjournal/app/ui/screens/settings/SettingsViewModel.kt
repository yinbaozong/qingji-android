package com.dreamjournal.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.data.repository.DreamRepository
import com.dreamjournal.app.data.repository.SettingsRepository
import com.dreamjournal.app.data.repository.TxtExportRepository
import com.dreamjournal.app.domain.ai.AiService
import com.dreamjournal.app.domain.model.ExportFormat
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.settings.AnalysisProviderType
import com.dreamjournal.app.domain.settings.AnalysisServiceType
import com.dreamjournal.app.domain.settings.AppSettings
import com.dreamjournal.app.domain.settings.SpeechProviderType
import com.dreamjournal.app.domain.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val APP_VERSION_NAME = "1.3.7"
private const val MINIMAX_BASE_URL = "https://api.minimaxi.com/v1"
private const val MINIMAX_API_PATH = "/chat/completions"
private const val QWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
private const val QWEN_API_PATH = "/chat/completions"
private const val ALIYUN_SPEECH_BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
private const val ALIYUN_SPEECH_API_PATH = "/services/aigc/multimodal-generation/generation"

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val versionName: String = APP_VERSION_NAME,
    val totalCount: Int = 0,
    val todayDreamCount: Int = 0,
    val dayRecordCount: Int = 0,
    val dreamRecordCount: Int = 0,
    val withAudioCount: Int = 0,
    val entriesForExport: List<DreamEntryEntity> = emptyList(),
    val exportStartDate: String = "",
    val exportEndDate: String = "",
    val exportFormat: ExportFormat = ExportFormat.MARKDOWN,
    val showAdvancedSettings: Boolean = false,
    val isTestingSpeechConfig: Boolean = false,
    val isTestingAnalysisConfig: Boolean = false,
    val isGeneratingDailySummary: Boolean = false,
    val dailySummary: String? = null,
    val statusMessage: String? = null,
    val exportPath: String? = null,
    val exportedFilesBytes: Long = 0L
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dreamRepository: DreamRepository,
    private val txtExportRepository: TxtExportRepository,
    private val aiService: AiService
) : ViewModel() {

    private val settingsFlow = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val entriesFlow = dreamRepository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settingsFlow, entriesFlow) { settings, entries ->
                settings to entries.sortedByDescending { it.createdAt }
            }.collect { (settings, entries) ->
                val today = LocalDate.now().toString()
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        totalCount = entries.size,
                        todayDreamCount = entries.count { it.dreamDate == today },
                        dayRecordCount = entries.count { RecordType.fromStorage(it.recordType) == RecordType.DAY },
                        dreamRecordCount = entries.count { RecordType.fromStorage(it.recordType) == RecordType.DREAM },
                        withAudioCount = entries.count { !it.audioPath.isNullOrBlank() || it.extraAudioPaths != "[]" },
                        entriesForExport = entries,
                        exportStartDate = current.exportStartDate.ifBlank { entries.lastOrNull()?.dreamDate ?: today },
                        exportEndDate = current.exportEndDate.ifBlank { today }
                    )
                }
            }
        }
        refreshExportedFilesSize()
    }

    fun setThemeMode(mode: ThemeMode) {
        updateSettings { it.copy(themeMode = mode) }
    }

    fun setSpeechProviderType(type: SpeechProviderType) {
        updateSettings {
            val next = it.copy(speechProviderType = type)
            if (type == SpeechProviderType.ALIYUN_QWEN_ASR) {
                next.copy(
                    aliyunSpeechBaseUrl = ALIYUN_SPEECH_BASE_URL,
                    aliyunSpeechApiPath = ALIYUN_SPEECH_API_PATH,
                    aliyunSpeechModel = next.aliyunSpeechModel.ifBlank { "qwen3-asr-flash" }
                )
            } else {
                next
            }
        }
    }

    fun setAnalysisProviderType(type: AnalysisProviderType) {
        updateSettings {
            val next = it.copy(analysisProviderType = type)
            if (type == AnalysisProviderType.OPENAI_COMPATIBLE) {
                applyAnalysisServicePreset(next.analysisServiceType, next)
            } else {
                next
            }
        }
    }

    fun setAnalysisServiceType(type: AnalysisServiceType) {
        updateSettings { current ->
            applyAnalysisServicePreset(type, current.copy(analysisServiceType = type))
        }
    }

    fun setSpeechBaseUrl(value: String) {
        updateSettings { it.copy(speechBaseUrl = value) }
    }

    fun setSpeechApiPath(value: String) {
        updateSettings { it.copy(speechApiPath = value) }
    }

    fun setSpeechModel(value: String) {
        updateSettings { it.copy(speechModel = value) }
    }

    fun setSpeechApiKey(value: String) {
        updateSettings { it.copy(speechApiKey = value) }
    }

    fun setAliyunSpeechApiKey(value: String) {
        updateSettings { it.copy(aliyunSpeechApiKey = value) }
    }

    fun setAliyunSpeechModel(value: String) {
        updateSettings { current ->
            current.copy(aliyunSpeechModel = value.ifBlank { "qwen3-asr-flash" })
        }
    }

    fun useAliyunSpeechDefaults() {
        updateSettings {
            it.copy(
                speechProviderType = SpeechProviderType.ALIYUN_QWEN_ASR,
                aliyunSpeechBaseUrl = ALIYUN_SPEECH_BASE_URL,
                aliyunSpeechApiPath = ALIYUN_SPEECH_API_PATH,
                aliyunSpeechModel = "qwen3-asr-flash"
            )
        }
    }

    fun setBaiduSpeechUrl(value: String) {
        updateSettings { it.copy(baiduSpeechUrl = value) }
    }

    fun setBaiduTokenUrl(value: String) {
        updateSettings { it.copy(baiduTokenUrl = value) }
    }

    fun setBaiduApiKey(value: String) {
        updateSettings { it.copy(baiduApiKey = value) }
    }

    fun setBaiduSecretKey(value: String) {
        updateSettings { it.copy(baiduSecretKey = value) }
    }

    fun setBaiduAppId(value: String) {
        updateSettings { it.copy(baiduAppId = value) }
    }

    fun setBaiduDevPid(value: String) {
        updateSettings { it.copy(baiduDevPid = value) }
    }

    fun setAnalysisModel(value: String) {
        updateSettings { it.copy(analysisModel = value) }
    }

    fun setAnalysisApiKey(value: String) {
        updateSettings { it.copy(analysisApiKey = value) }
    }

    fun setAnalysisPromptTemplate(value: String) {
        updateSettings { it.copy(analysisPromptTemplate = value) }
    }

    fun addCustomTag(value: String) {
        val tag = value.trim()
        if (tag.isBlank()) return
        updateSettings { current ->
            val nextTags = (parseCustomTags(current.customTags) + tag).distinct()
            current.copy(customTags = nextTags.joinToString("\n"))
        }
        _uiState.update { it.copy(statusMessage = "已添加标签：$tag") }
    }

    fun removeCustomTag(value: String) {
        val tag = value.trim()
        if (tag.isBlank()) return
        updateSettings { current ->
            current.copy(
                customTags = parseCustomTags(current.customTags)
                    .filterNot { it == tag }
                    .joinToString("\n")
            )
        }
        _uiState.update { it.copy(statusMessage = "已移除标签：$tag") }
    }

    fun useMiniMaxDefaults() {
        updateSettings {
            applyAnalysisServicePreset(
                AnalysisServiceType.MINIMAX,
                it.copy(
                    analysisProviderType = AnalysisProviderType.OPENAI_COMPATIBLE,
                    analysisServiceType = AnalysisServiceType.MINIMAX
                )
            )
        }
    }

    fun useQwenDefaults() {
        updateSettings {
            applyAnalysisServicePreset(
                AnalysisServiceType.QWEN,
                it.copy(
                    analysisProviderType = AnalysisProviderType.OPENAI_COMPATIBLE,
                    analysisServiceType = AnalysisServiceType.QWEN
                )
            )
        }
    }

    fun toggleAdvancedSettings() {
        _uiState.update { it.copy(showAdvancedSettings = !it.showAdvancedSettings) }
    }

    fun setExportStartDate(value: String) {
        _uiState.update { it.copy(exportStartDate = value) }
    }

    fun setExportEndDate(value: String) {
        _uiState.update { it.copy(exportEndDate = value) }
    }

    fun setExportFormat(value: ExportFormat) {
        _uiState.update { it.copy(exportFormat = value, statusMessage = null, exportPath = null) }
    }

    fun generateTodaySummary() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val todayEntries = entriesFlow.value.filter { it.dreamDate == today }
            val text = buildTodayText(todayEntries)
            if (text.isBlank()) {
                _uiState.update { it.copy(statusMessage = "今天还没有可回顾的记录") }
                return@launch
            }

            _uiState.update { it.copy(isGeneratingDailySummary = true, statusMessage = null) }
            aiService.analyze(
                "请把以下同一天的白天与夜间记录整理成一段温和、易读的今日回顾。提炼今天的重要片段、仍值得关注的想法，以及一个简短的明日提醒。不要过度解读。\n\n$text"
            ).onSuccess { result ->
                val finalText = buildString {
                    appendLine(result.summary)
                    result.suggestions.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                        append("明日提醒：$it")
                    }
                }
                _uiState.update {
                    it.copy(
                        dailySummary = finalText,
                        isGeneratingDailySummary = false,
                        statusMessage = "今日回顾已生成"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isGeneratingDailySummary = false,
                        statusMessage = "今日总结失败：${error.message}"
                    )
                }
            }
        }
    }

    fun exportAll() {
        viewModelScope.launch {
            val entries = dreamRepository.getAllEntries().sortedByDescending { it.createdAt }
            if (entries.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "还没有可导出的记录") }
                return@launch
            }
            val format = uiState.value.exportFormat
            txtExportRepository.exportAll(entries, format)
                .onSuccess { file ->
                    val bytes = withContext(Dispatchers.IO) { txtExportRepository.exportedFilesSize() }
                    _uiState.update {
                        it.copy(
                            statusMessage = "已导出全部记录（${format.displayName}）。带图片或录音时会自动整理成 ZIP。",
                            exportPath = file.absolutePath,
                            exportedFilesBytes = bytes
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(statusMessage = "导出失败：${error.message}")
                    }
                }
        }
    }

    fun exportSelectedRange() {
        viewModelScope.launch {
            val start = uiState.value.exportStartDate
            val end = uiState.value.exportEndDate
            if (start.isBlank() || end.isBlank()) {
                _uiState.update { it.copy(statusMessage = "请先选一个开始日期和结束日期") }
                return@launch
            }
            val entries = dreamRepository.getAllEntries()
                .filter { it.dreamDate in start..end }
                .sortedByDescending { it.createdAt }
            if (entries.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "所选范围内没有记录") }
                return@launch
            }
            val format = uiState.value.exportFormat
            txtExportRepository.exportRange(entries, start, end, format)
                .onSuccess { file ->
                    val bytes = withContext(Dispatchers.IO) { txtExportRepository.exportedFilesSize() }
                    _uiState.update {
                        it.copy(
                            statusMessage = "已导出这段时间的记录（${format.displayName}），文字、录音和图片都整理好了。",
                            exportPath = file.absolutePath,
                            exportedFilesBytes = bytes
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = "导出失败：${error.message}") }
                }
        }
    }

    fun exportSingle(entryId: Long) {
        viewModelScope.launch {
            val entry = dreamRepository.getEntryById(entryId)
            if (entry == null) {
                _uiState.update { it.copy(statusMessage = "没有找到这条记录") }
                return@launch
            }
            val format = uiState.value.exportFormat
            txtExportRepository.exportSingle(entry, format)
                .onSuccess { file ->
                    val bytes = withContext(Dispatchers.IO) { txtExportRepository.exportedFilesSize() }
                    _uiState.update {
                        it.copy(
                            statusMessage = "已导出单条记录（${format.displayName}），相关媒体已一起整理。",
                            exportPath = file.absolutePath,
                            exportedFilesBytes = bytes
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = "导出失败：${error.message}") }
                }
        }
    }

    fun clearExportedFiles() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { txtExportRepository.clearExportedFiles() }
            result.onSuccess { released ->
                _uiState.update {
                    it.copy(
                        exportedFilesBytes = 0L,
                        exportPath = null,
                        statusMessage = if (released > 0L) {
                            "已清理 ${formatBytes(released)} 的导出文件"
                        } else {
                            "没有需要清理的导出文件"
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(statusMessage = "清理失败：${error.message}") }
            }
        }
    }

    private fun refreshExportedFilesSize() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { txtExportRepository.exportedFilesSize() }
            _uiState.update { it.copy(exportedFilesBytes = bytes) }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    fun saveAndTestSpeechSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingSpeechConfig = true, statusMessage = "正在测试语音配置…") }
            aiService.validateSpeechConfiguration()
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isTestingSpeechConfig = false,
                            statusMessage = "语音配置可用：$message"
                        )
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
                    _uiState.update {
                        it.copy(
                            isTestingSpeechConfig = false,
                            statusMessage = "语音配置测试失败：$detail"
                        )
                    }
                }
        }
    }

    fun saveAndTestAnalysisSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAnalysisConfig = true, statusMessage = "正在测试 AI 配置…") }
            aiService.validateAnalysisConfiguration()
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isTestingAnalysisConfig = false,
                            statusMessage = "AI 配置可用：$message"
                        )
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
                    _uiState.update {
                        it.copy(
                            isTestingAnalysisConfig = false,
                            statusMessage = "AI 配置测试失败：$detail"
                        )
                    }
                }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }

    private fun applyAnalysisServicePreset(serviceType: AnalysisServiceType, settings: AppSettings): AppSettings {
        return when (serviceType) {
            AnalysisServiceType.MINIMAX -> settings.copy(
                analysisBaseUrl = MINIMAX_BASE_URL,
                analysisApiPath = MINIMAX_API_PATH,
                analysisModel = if (settings.analysisModel.startsWith("qwen")) {
                    "MiniMax-M2.5"
                } else {
                    settings.analysisModel.ifBlank { "MiniMax-M2.5" }
                }
            )
            AnalysisServiceType.QWEN -> settings.copy(
                analysisBaseUrl = QWEN_BASE_URL,
                analysisApiPath = QWEN_API_PATH,
                analysisModel = if (settings.analysisModel.startsWith("MiniMax")) {
                    "qwen-plus"
                } else {
                    settings.analysisModel.ifBlank { "qwen-plus" }
                }
            )
        }
    }

    private fun buildTodayText(entries: List<DreamEntryEntity>): String {
        return entries.joinToString("\n\n") { item ->
            val body = when {
                item.content.isNotBlank() -> item.content
                !item.transcript.isNullOrBlank() -> item.transcript
                else -> ""
            }
            "标题：${item.title}\n内容：$body"
        }.trim()
    }

    private fun parseCustomTags(raw: String): List<String> {
        return raw.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            dreamRepository: DreamRepository,
            txtExportRepository: TxtExportRepository,
            aiService: AiService
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        settingsRepository = settingsRepository,
                        dreamRepository = dreamRepository,
                        txtExportRepository = txtExportRepository,
                        aiService = aiService
                    ) as T
                }
            }
        }
    }
}
