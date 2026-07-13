package com.dreamjournal.app.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dreamjournal.app.data.local.AiMessageEntity
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.model.EntryContentBlock
import com.dreamjournal.app.domain.settings.AppSettings
import com.dreamjournal.app.domain.settings.ThemeMode
import com.dreamjournal.app.domain.model.TodoItem
import com.dreamjournal.app.ui.screens.calendar.CalendarScreen
import com.dreamjournal.app.ui.screens.calendar.CalendarUiState
import com.dreamjournal.app.ui.screens.calendar.DayMarkers
import com.dreamjournal.app.ui.screens.detail.DetailScreen
import com.dreamjournal.app.ui.screens.detail.DetailUiState
import com.dreamjournal.app.ui.screens.home.HomeScreen
import com.dreamjournal.app.ui.screens.home.HomeUiState
import com.dreamjournal.app.ui.screens.settings.SettingsScreen
import com.dreamjournal.app.ui.screens.settings.SettingsUiState
import com.dreamjournal.app.ui.components.QingJiBrandTitle
import com.dreamjournal.app.ui.components.QingJiLogo
import com.dreamjournal.app.ui.theme.QingJiTheme

// ── Static Preview Data (no runtime calls) ────────────────────────────

private const val TS_TODAY = 1752278400000L        // 2025-07-12 fixed
private const val TS_YESTERDAY = 1752192000000L     // 2025-07-11 fixed
private const val TS_TWO_DAYS = 1752105600000L      // 2025-07-10 fixed

private const val DATE_TODAY = "2025-07-12"
private const val DATE_YESTERDAY = "2025-07-11"
private const val DATE_TWO_DAYS = "2025-07-10"

private val previewEntries = listOf(
    DreamEntryEntity(
        id = 1L,
        createdAt = TS_TODAY,
        updatedAt = TS_TODAY,
        dreamDate = DATE_TODAY,
        recordType = RecordType.DAY.name,
        title = "下午的会议记录",
        dreamTag = "工作,重要",
        content = "今天和产品团队讨论了新版本的设计方案，主要确定了首页改版的方向。",
        photoPaths = "",
        todoItems = "",
        audioPath = "sample_audio.m4a",
        transcript = "今天和产品团队讨论了新版本的设计方案。",
        aiSummary = "一次关于产品设计方案的工作会议记录。",
        aiKeywords = "会议,设计,方案",
        aiEmotion = "积极",
        aiSuggestions = "可以整理出具体的行动项。"
    ),
    DreamEntryEntity(
        id = 2L,
        createdAt = TS_YESTERDAY,
        updatedAt = TS_YESTERDAY,
        dreamDate = DATE_YESTERDAY,
        recordType = RecordType.DREAM.name,
        title = "夜间的想法",
        dreamTag = "灵感",
        content = "突然想到一个很好的点子，关于如何改进用户体验。",
        photoPaths = "",
        todoItems = "",
        audioPath = null,
        transcript = null,
        aiSummary = null,
        aiKeywords = null,
        aiEmotion = null,
        aiSuggestions = null
    ),
    DreamEntryEntity(
        id = 3L,
        createdAt = TS_TWO_DAYS,
        updatedAt = TS_TWO_DAYS,
        dreamDate = DATE_TWO_DAYS,
        recordType = RecordType.DAY.name,
        title = "周末读书笔记",
        dreamTag = "阅读",
        content = "读完了《深度工作》的前三章，有很多启发。",
        photoPaths = "",
        todoItems = "",
        audioPath = "sample_audio2.m4a",
        transcript = "读完了深度工作的前三章。",
        aiSummary = "阅读笔记，关于深度工作的学习心得。",
        aiKeywords = "阅读,学习,深度工作",
        aiEmotion = "平静",
        aiSuggestions = "继续记录后续章节的阅读感受。"
    )
)

private val previewMessages = listOf(
    AiMessageEntity(id = 1L, entryId = 1L, role = "assistant", message = "这是一次关于产品设计方案的工作会议记录。主要讨论了首页改版方向。", createdAt = TS_TODAY - 100_000L),
    AiMessageEntity(id = 2L, entryId = 1L, role = "user", message = "能帮我整理一下会议要点吗？", createdAt = TS_TODAY - 200_000L),
    AiMessageEntity(id = 3L, entryId = 1L, role = "assistant", message = "当然可以。会议主要确定了三个方向：1. 首页简化 2. 录音功能优化 3. 日历视图改进。", createdAt = TS_TODAY - 300_000L)
)

private val noop = {}
private val noopStr: (String) -> Unit = {}
private val noopLong: (Long) -> Unit = {}

@Preview(name = "品牌图标", showBackground = true, widthDp = 320, heightDp = 180)
@Composable
private fun PreviewBrandIdentity() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        Surface {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)
            ) {
                QingJiLogo(size = 72.dp)
                QingJiBrandTitle(title = "瞬记", subtitle = "白天与夜晚，都值得被记住")
            }
        }
    }
}

// ── 1. 首页 ──────────────────────────────────────────────────────

@Preview(name = "首页", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewHomeScreen() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        HomeScreen(
            uiState = HomeUiState(entries = previewEntries),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}

// ── 2. 首页空状态 ────────────────────────────────────────────────

@Preview(name = "首页空状态", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewHomeScreenEmpty() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        HomeScreen(
            uiState = HomeUiState(entries = emptyList()),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}

// ── 3. 首页录音中 ────────────────────────────────────────────────

@Preview(name = "首页录音中", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewHomeScreenRecording() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        HomeScreen(
            uiState = HomeUiState(
                entries = previewEntries,
                isRecording = true,
                recordingSeconds = 42,
                waveformLevels = listOf(0.3f, 0.6f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f, 0.9f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f),
                activeRecordType = RecordType.DAY
            ),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}

// ── 4. 首页转写中 ────────────────────────────────────────────────

@Preview(name = "首页转写中", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewHomeScreenTranscribing() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        HomeScreen(
            uiState = HomeUiState(
                entries = previewEntries,
                isTranscribing = true,
                activeRecordType = RecordType.DAY
            ),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}

// ── 5. 详情页 ──────────────────────────────────────────────────────

@Preview(name = "详情页", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewDetailScreen() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        DetailScreen(
            uiState = DetailUiState(
                entry = previewEntries[0],
                availableTags = listOf("工作", "重要", "灵感", "日常", "阅读"),
                draftTitle = "下午的会议记录",
                draftTags = listOf("工作", "重要"),
                draftContent = "今天和产品团队讨论了新版本的设计方案，主要确定了首页改版的方向。",
                draftBlocks = listOf(EntryContentBlock.text(value = "今天和产品团队讨论了新版本的设计方案，主要确定了首页改版的方向。")),
                draftTodos = listOf(TodoItem(id = 1L, text = "整理会议纪要", isDone = true), TodoItem(id = 2L, text = "发送方案文档", isDone = false)),
                audioPaths = listOf("sample_audio.m4a", "sample_follow_up.amr"),
                messages = previewMessages,
                chatInput = ""
            ),
            onBack = noop,
            onTitleChange = noopStr,
            onTagToggle = noopStr,
            onUpdateTextBlock = { _, _ -> },
            onFocusTextBlock = noopLong,
            onSetImageWidth = { _, _ -> },
            onAddPhotos = {},
            onRemovePhoto = noopStr,
            onAddTodo = noop,
            onUpdateTodoText = { _, _ -> },
            onToggleTodo = noopLong,
            onRemoveTodo = noopLong,
            onSaveDraft = noop,
            onRunAnalysis = noop,
            onChatInputChange = noopStr,
            onSendChat = noop,
            onDeleteEntry = noop,
            onToggleAudio = noopStr,
            onTranscribeAudio = noopStr,
            onStartAdditionalRecording = noop,
            onStopAdditionalRecording = noop,
            onClearError = noop,
            onClearStatus = noop
        )
    }
}

@Preview(name = "详情追加录音", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewDetailRecordingMore() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        DetailScreen(
            uiState = DetailUiState(
                entry = previewEntries[0],
                availableTags = listOf("工作", "重要", "灵感", "日常"),
                draftTitle = "下午的会议记录",
                draftTags = listOf("工作"),
                draftContent = "第一次录音后，又想起了一个值得补充的细节。",
                draftBlocks = listOf(EntryContentBlock.text(value = "第一次录音后，又想起了一个值得补充的细节。")),
                audioPaths = listOf("sample_audio.m4a"),
                isRecordingMore = true,
                additionalRecordingSeconds = 28,
                additionalWaveformLevels = listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.7f, 0.9f, 0.3f, 0.6f, 0.8f, 0.4f, 0.7f, 0.5f)
            ),
            onBack = noop,
            onTitleChange = noopStr,
            onTagToggle = noopStr,
            onUpdateTextBlock = { _, _ -> },
            onFocusTextBlock = noopLong,
            onSetImageWidth = { _, _ -> },
            onAddPhotos = {},
            onRemovePhoto = noopStr,
            onAddTodo = noop,
            onUpdateTodoText = { _, _ -> },
            onToggleTodo = noopLong,
            onRemoveTodo = noopLong,
            onSaveDraft = noop,
            onRunAnalysis = noop,
            onChatInputChange = noopStr,
            onSendChat = noop,
            onDeleteEntry = noop,
            onToggleAudio = noopStr,
            onTranscribeAudio = noopStr,
            onStartAdditionalRecording = noop,
            onStopAdditionalRecording = noop,
            onClearError = noop,
            onClearStatus = noop
        )
    }
}

// ── 6. 日历页 ──────────────────────────────────────────────────────

@Preview(name = "日历页", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewCalendarScreen() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        CalendarScreen(
            uiState = CalendarUiState(
                selectedDate = DATE_TODAY,
                entries = previewEntries.filter { it.dreamDate == DATE_TODAY },
                markedDates = mapOf(
                    DATE_TODAY to DayMarkers(hasDayRecord = true, hasDreamRecord = false),
                    DATE_YESTERDAY to DayMarkers(hasDayRecord = false, hasDreamRecord = true),
                    DATE_TWO_DAYS to DayMarkers(hasDayRecord = true, hasDreamRecord = false)
                )
            ),
            onDateSelected = noopStr,
            onOpenEntry = noopLong
        )
    }
}

// ── 7. 我的页面 ───────────────────────────────────────────────────

@Preview(name = "我的页面", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewSettingsScreen() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = AppSettings(),
                totalCount = 42,
                todayDreamCount = 1,
                dayRecordCount = 28,
                dreamRecordCount = 14,
                withAudioCount = 35,
                entriesForExport = previewEntries,
                dailySummary = "今天的记录主要围绕工作会议展开，整体情绪积极向上。"
            ),
            onSetThemeMode = {},
            onSetSpeechProviderType = {},
            onSetAnalysisProviderType = {},
            onSetAnalysisServiceType = {},
            onSetSpeechBaseUrl = noopStr,
            onSetSpeechApiPath = noopStr,
            onSetSpeechModel = noopStr,
            onSetBaiduSpeechUrl = noopStr,
            onSetBaiduTokenUrl = noopStr,
            onSetBaiduApiKey = noopStr,
            onSetBaiduSecretKey = noopStr,
            onSetBaiduAppId = noopStr,
            onSetBaiduDevPid = noopStr,
            onSetAliyunSpeechApiKey = noopStr,
            onSetAliyunSpeechModel = noopStr,
            onSetAnalysisModel = noopStr,
            onSetSpeechApiKey = noopStr,
            onSetAnalysisApiKey = noopStr,
            onSetAnalysisPromptTemplate = noopStr,
            onAddCustomTag = noopStr,
            onRemoveCustomTag = noopStr,
            onSetExportStartDate = noopStr,
            onSetExportEndDate = noopStr,
            onSetExportFormat = {},
            onToggleAdvancedSettings = noop,
            onUseMiniMaxDefaults = noop,
            onUseQwenDefaults = noop,
            onUseAliyunSpeechDefaults = noop,
            onSaveSpeechSettings = noop,
            onSaveAnalysisSettings = noop,
            onGenerateTodaySummary = noop,
            onExportSelectedRange = noop,
            onExportAll = noop,
            onExportSingle = noopLong,
            onClearExportedFiles = noop
        )
    }
}

// ── 8. 明亮模式 ────────────────────────────────────────────────

@Preview(name = "明亮模式", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewLightMode() {
    QingJiTheme(themeMode = ThemeMode.LIGHT) {
        HomeScreen(
            uiState = HomeUiState(entries = previewEntries),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}

// ── 9. 暗夜模式 ──────────────────────────────────────────────────

@Preview(name = "暗夜模式", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewDarkMode() {
    QingJiTheme(themeMode = ThemeMode.DARK) {
        HomeScreen(
            uiState = HomeUiState(entries = previewEntries),
            onStartRecording = {},
            onStopRecording = {},
            onCreateTextEntry = noop,
            onOpenEntry = noopLong,
            onClearError = noop
        )
    }
}
