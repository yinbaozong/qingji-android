package com.dreamjournal.app.ui.screens.settings

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dreamjournal.app.domain.settings.AnalysisProviderType
import com.dreamjournal.app.domain.settings.AnalysisServiceType
import com.dreamjournal.app.domain.model.ExportFormat
import com.dreamjournal.app.domain.settings.SpeechProviderType
import com.dreamjournal.app.domain.settings.ThemeMode
import com.dreamjournal.app.domain.settings.preset
import com.dreamjournal.app.ui.components.QingJiBrandTitle
import com.dreamjournal.app.ui.components.GlassBackground
import com.dreamjournal.app.ui.components.glassCardColor
import com.dreamjournal.app.ui.components.glassSurface
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.io.File

private const val ExportCalendarAnchorPage = 1200

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
fun SettingsScreen(
    uiState: SettingsUiState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSpeechProviderType: (SpeechProviderType) -> Unit,
    onSetAnalysisProviderType: (AnalysisProviderType) -> Unit,
    onSetAnalysisServiceType: (AnalysisServiceType) -> Unit,
    onSetSpeechBaseUrl: (String) -> Unit,
    onSetSpeechApiPath: (String) -> Unit,
    onSetSpeechModel: (String) -> Unit,
    onSetBaiduApiKey: (String) -> Unit,
    onSetBaiduSecretKey: (String) -> Unit,
    onSetBaiduAppId: (String) -> Unit,
    onSetAliyunSpeechApiKey: (String) -> Unit,
    onSetAnalysisModel: (String) -> Unit,
    onSetAnalysisBaseUrl: (String) -> Unit,
    onSetAnalysisApiPath: (String) -> Unit,
    onSetSpeechApiKey: (String) -> Unit,
    onSetAnalysisApiKey: (String) -> Unit,
    onSetAnalysisPromptTemplate: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onRemoveCustomTag: (String) -> Unit,
    onSetExportStartDate: (String) -> Unit,
    onSetExportEndDate: (String) -> Unit,
    onSetExportFormat: (ExportFormat) -> Unit,
    onToggleAdvancedSettings: () -> Unit,
    onSaveSpeechSettings: () -> Unit,
    onSaveAnalysisSettings: () -> Unit,
    onGenerateTodaySummary: () -> Unit,
    onExportSelectedRange: () -> Unit,
    onExportAll: () -> Unit,
    onExportSingle: (Long) -> Unit,
    onClearExportedFiles: () -> Unit
) {
    val settings = uiState.settings
    val context = LocalContext.current
    val isDarkMode = settings.themeMode == ThemeMode.DARK
    val customTags = remember(settings.customTags) { settings.customTags.split("\n").map { it.trim() }.filter { it.isNotBlank() } }
    var newTagInput by rememberSaveable { mutableStateOf("") }
    GlassBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("我的", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("管理记录、服务与本地数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard {
                QingJiBrandTitle(title = "瞬记", subtitle = "白天与夜晚，都值得被记住")
                PreferenceRow("暗夜模式", if (isDarkMode) "已开启" else "已关闭") { Switch(checked = isDarkMode, onCheckedChange = { onSetThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT) }) }
            }
            JournalOverviewCard(uiState)
            SectionCard {
                SectionHeader("AI 今日回顾", if (uiState.isGeneratingDailySummary) "生成中…" else "生成", onGenerateTodaySummary, !uiState.isGeneratingDailySummary)
                Text(uiState.dailySummary ?: "让 AI 帮你回顾和整理今天的记录。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard {
                Text("标签管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("管理常用标签，编辑记录时可直接选择。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (customTags.isNotEmpty()) { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { customTags.forEach { tag -> AssistChip(onClick = { onRemoveCustomTag(tag) }, label = { Text("× $tag") }) } } }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newTagInput, onValueChange = { newTagInput = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("新标签") }, shape = RoundedCornerShape(12.dp))
                    Button(onClick = { onAddCustomTag(newTagInput); newTagInput = "" }, enabled = newTagInput.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("添加") }
                }
            }
            ExportCenterCard(
                uiState,
                onSetExportStartDate,
                onSetExportEndDate,
                onSetExportFormat,
                onExportSelectedRange,
                onExportAll,
                onExportSingle,
                onClearExportedFiles,
                onShareExport = { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val mimeType = when (file.extension.lowercase()) {
                            "zip" -> "application/zip"
                            "md" -> "text/markdown"
                            else -> "text/plain"
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri(file.name, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享导出文件"))
                    }
                }
            )
            SectionCard {
                SectionHeader("服务配置", if (uiState.showAdvancedSettings) "收起" else "展开", onToggleAdvancedSettings)
                ServiceStatusRow("语音转文字", settings.speechProviderType.displayName())
                ServiceStatusRow(
                    "AI 分析",
                    if (settings.analysisProviderType == AnalysisProviderType.MOCK) "未启用" else settings.analysisServiceType.preset().displayName
                )
                AnimatedVisibility(visible = uiState.showAdvancedSettings) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SpeechConfigSection(settings, onSetSpeechProviderType, onSetAliyunSpeechApiKey, onSetBaiduApiKey, onSetBaiduSecretKey, onSetBaiduAppId, onSetSpeechBaseUrl, onSetSpeechApiPath, onSetSpeechModel, onSetSpeechApiKey, onSaveSpeechSettings, uiState.isTestingSpeechConfig)
                        AnalysisConfigSection(settings, onSetAnalysisProviderType, onSetAnalysisServiceType, onSetAnalysisModel, onSetAnalysisBaseUrl, onSetAnalysisApiPath, onSetAnalysisApiKey, onSetAnalysisPromptTemplate, onSaveAnalysisSettings, uiState.isTestingAnalysisConfig)
                    }
                }
            }
            AnimatedVisibility(visible = !uiState.statusMessage.isNullOrBlank() || !uiState.exportPath.isNullOrBlank()) {
                SectionCard { uiState.statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }; uiState.exportPath?.let { Text("导出路径：$it", style = MaterialTheme.typography.bodySmall) } }
            }
            Text("瞬记 v${uiState.versionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
}

@Composable
private fun SpeechConfigSection(settings: com.dreamjournal.app.domain.settings.AppSettings, onSetSpeechProviderType: (SpeechProviderType) -> Unit, onSetAliyunSpeechApiKey: (String) -> Unit, onSetBaiduApiKey: (String) -> Unit, onSetBaiduSecretKey: (String) -> Unit, onSetBaiduAppId: (String) -> Unit, onSetSpeechBaseUrl: (String) -> Unit, onSetSpeechApiPath: (String) -> Unit, onSetSpeechModel: (String) -> Unit, onSetSpeechApiKey: (String) -> Unit, onSaveSpeechSettings: () -> Unit, isTesting: Boolean) {
    SubSectionCard("语音转文字") {
        Text("录完后自动转成可编辑文字。推荐继续使用阿里云。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ServicePicker(
            label = "识别服务",
            selected = settings.speechProviderType,
            selectedLabel = settings.speechProviderType.displayName(),
            options = listOf(
                SpeechProviderType.ALIYUN_QWEN_ASR to "阿里云百炼（推荐）",
                SpeechProviderType.SYSTEM to "手机系统识别",
                SpeechProviderType.BAIDU_ASR to "百度短语音",
                SpeechProviderType.OPENAI_COMPATIBLE to "其他兼容服务"
            ) + if (settings.speechProviderType == SpeechProviderType.MOCK) listOf(SpeechProviderType.MOCK to "演示模式") else emptyList(),
            onSelected = onSetSpeechProviderType
        )
        when (settings.speechProviderType) {
            SpeechProviderType.ALIYUN_QWEN_ASR -> {
                OutlinedTextField(value = settings.aliyunSpeechApiKey, onValueChange = onSetAliyunSpeechApiKey, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("阿里云 API Key") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                Text("模型已自动选择 qwen3-asr-flash，无需填写地址。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpeechProviderType.BAIDU_ASR -> {
                Text("百度适合 60 秒内短录音，需要三项控制台凭证。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecretField("百度 API Key", settings.baiduApiKey, onSetBaiduApiKey)
                SecretField("百度 Secret Key", settings.baiduSecretKey, onSetBaiduSecretKey)
                OutlinedTextField(value = settings.baiduAppId, onValueChange = onSetBaiduAppId, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("百度 AppID") }, shape = RoundedCornerShape(14.dp))
            }
            SpeechProviderType.OPENAI_COMPATIBLE -> {
                Text("高级选项，适合已经知道接口地址的服务。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = settings.speechBaseUrl, onValueChange = onSetSpeechBaseUrl, modifier = Modifier.fillMaxWidth(), label = { Text("Base URL") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = settings.speechApiPath, onValueChange = onSetSpeechApiPath, modifier = Modifier.fillMaxWidth(), label = { Text("API Path") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = settings.speechModel, onValueChange = onSetSpeechModel, modifier = Modifier.fillMaxWidth(), label = { Text("模型") }, shape = RoundedCornerShape(14.dp))
                SecretField("API Key", settings.speechApiKey, onSetSpeechApiKey)
            }
            SpeechProviderType.SYSTEM -> { Text("不需要 API Key，直接调用手机内置语音识别。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            SpeechProviderType.MOCK -> { Text("演示模式。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Button(onClick = onSaveSpeechSettings, modifier = Modifier.fillMaxWidth(), enabled = !isTesting, shape = RoundedCornerShape(14.dp)) { Text(if (isTesting) "正在测试…" else "保存并测试") }
    }
}

@Composable
private fun AnalysisConfigSection(settings: com.dreamjournal.app.domain.settings.AppSettings, onSetAnalysisProviderType: (AnalysisProviderType) -> Unit, onSetAnalysisServiceType: (AnalysisServiceType) -> Unit, onSetAnalysisModel: (String) -> Unit, onSetAnalysisBaseUrl: (String) -> Unit, onSetAnalysisApiPath: (String) -> Unit, onSetAnalysisApiKey: (String) -> Unit, onSetAnalysisPromptTemplate: (String) -> Unit, onSaveAnalysisSettings: () -> Unit, isTesting: Boolean) {
    var showPrompt by rememberSaveable { mutableStateOf(false) }
    val preset = settings.analysisServiceType.preset()
    SubSectionCard("AI 分析") {
        PreferenceRow("启用 AI 分析", "服务配置只保存在本机") {
            Switch(checked = settings.analysisProviderType == AnalysisProviderType.OPENAI_COMPATIBLE, onCheckedChange = { onSetAnalysisProviderType(if (it) AnalysisProviderType.OPENAI_COMPATIBLE else AnalysisProviderType.MOCK) })
        }
        if (settings.analysisProviderType == AnalysisProviderType.OPENAI_COMPATIBLE) {
            ServicePicker(
                label = "模型服务",
                selected = settings.analysisServiceType,
                selectedLabel = preset.displayName,
                options = AnalysisServiceType.entries.map { it to it.preset().displayName },
                onSelected = onSetAnalysisServiceType
            )
            if (settings.analysisServiceType == AnalysisServiceType.CUSTOM) {
                OutlinedTextField(value = settings.analysisBaseUrl, onValueChange = onSetAnalysisBaseUrl, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Base URL") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = settings.analysisApiPath, onValueChange = onSetAnalysisApiPath, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("API Path") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = settings.analysisModel, onValueChange = onSetAnalysisModel, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("模型名称") }, shape = RoundedCornerShape(14.dp))
            } else {
                ServicePicker(label = "模型", selected = settings.analysisModel, selectedLabel = settings.analysisModel, options = preset.models.map { it to it }, onSelected = onSetAnalysisModel)
            }
            SecretField("${preset.displayName} API Key", settings.analysisApiKey, onSetAnalysisApiKey)
            TextButton(onClick = { showPrompt = !showPrompt }) { Text(if (showPrompt) "收起分析提示词" else "自定义分析提示词") }
            AnimatedVisibility(visible = showPrompt) {
                OutlinedTextField(value = settings.analysisPromptTemplate, onValueChange = onSetAnalysisPromptTemplate, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("分析提示词（可选）") }, shape = RoundedCornerShape(14.dp))
            }
            Button(onClick = onSaveAnalysisSettings, modifier = Modifier.fillMaxWidth(), enabled = !isTesting, shape = RoundedCornerShape(14.dp)) { Text(if (isTesting) "正在测试…" else "保存并测试") }
        } else { Text("AI 分析已关闭，记录与导出功能不受影响。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun <T> ServicePicker(label: String, selected: T, selectedLabel: String, options: List<Pair<T, String>>, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)).clickable { expanded = true }.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Text("选择", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { expanded = false; onSelected(value) }
                )
            }
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
}

@Composable
private fun ServiceStatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun SpeechProviderType.displayName(): String = when (this) {
    SpeechProviderType.ALIYUN_QWEN_ASR -> "阿里云百炼"
    SpeechProviderType.BAIDU_ASR -> "百度短语音"
    SpeechProviderType.SYSTEM -> "手机系统识别"
    SpeechProviderType.OPENAI_COMPATIBLE -> "其他兼容服务"
    SpeechProviderType.MOCK -> "演示模式"
}

@Composable @OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
private fun ExportCenterCard(uiState: SettingsUiState, onSetExportStartDate: (String) -> Unit, onSetExportEndDate: (String) -> Unit, onSetExportFormat: (ExportFormat) -> Unit, onExportSelectedRange: () -> Unit, onExportAll: () -> Unit, onExportSingle: (Long) -> Unit, onClearExportedFiles: () -> Unit, onShareExport: (String) -> Unit) {
    var showRangePicker by rememberSaveable { mutableStateOf(false) }
    var exportAllDates by rememberSaveable { mutableStateOf(true) }
    var singleMenuExpanded by remember { mutableStateOf(false) }
    var selectedSingleId by remember(uiState.entriesForExport) { mutableStateOf(uiState.entriesForExport.firstOrNull()?.id) }
    SectionCard {
        Text("数据导出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("把文字、照片、录音和待办一起带走。含媒体时会自动打包为 ZIP。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("导出格式", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.exportFormat == ExportFormat.MARKDOWN,
                onClick = { onSetExportFormat(ExportFormat.MARKDOWN) },
                label = { Text("Markdown（推荐）") }
            )
            FilterChip(
                selected = uiState.exportFormat == ExportFormat.TXT,
                onClick = { onSetExportFormat(ExportFormat.TXT) },
                label = { Text("纯文字 TXT") }
            )
        }
        if (uiState.exportFormat == ExportFormat.MARKDOWN) {
            Text(
                "自动生成目录和记录跳转；图片嵌入正文，录音使用相对链接，移动整个文件夹后仍可打开。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SubSectionCard("单条导出") {
            if (uiState.entriesForExport.isEmpty()) {
                Text("暂无可导出的记录。", style = MaterialTheme.typography.bodySmall)
            } else {
                Box {
                    Button(onClick = { singleMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(uiState.entriesForExport.firstOrNull { it.id == selectedSingleId }?.let { "${it.dreamDate} · ${it.title}" } ?: "选择一条记录")
                    }
                    DropdownMenu(expanded = singleMenuExpanded, onDismissRequest = { singleMenuExpanded = false }) {
                        uiState.entriesForExport.take(60).forEach { entry ->
                            DropdownMenuItem(
                                text = { Text("${entry.dreamDate} · ${entry.title}") },
                                onClick = { selectedSingleId = entry.id; singleMenuExpanded = false }
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { selectedSingleId?.let(onExportSingle) },
                    enabled = selectedSingleId != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("导出这条记录") }
            }
        }

        SubSectionCard("批量导出") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { exportAllDates = !exportAllDates },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = exportAllDates, onCheckedChange = { exportAllDates = it })
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("全部日期", style = MaterialTheme.typography.titleSmall)
                    Text("汇总当前所有记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AnimatedVisibility(visible = !exportAllDates) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { showRangePicker = true }, label = { Text("开始 ${uiState.exportStartDate.ifBlank { "请选择" }}") })
                        AssistChip(onClick = { showRangePicker = true }, label = { Text("结束 ${uiState.exportEndDate.ifBlank { "请选择" }}") })
                    }
                    TextButton(onClick = { showRangePicker = !showRangePicker }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showRangePicker) "收起日期选择" else "在日历中选择起止日期")
                    }
                    AnimatedVisibility(visible = showRangePicker) {
                        ExportRangePicker(
                            uiState.exportStartDate,
                            uiState.exportEndDate,
                            uiState.entriesForExport.map { it.dreamDate }.toSet(),
                            onSetExportStartDate,
                            onSetExportEndDate,
                            { showRangePicker = false }
                        )
                    }
                }
            }

            Button(
                onClick = if (exportAllDates) onExportAll else onExportSelectedRange,
                enabled = exportAllDates || (uiState.exportStartDate.isNotBlank() && uiState.exportEndDate.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (exportAllDates) "导出全部记录" else "导出所选日期")
            }
        }
        uiState.exportPath?.takeIf(String::isNotBlank)?.let { path ->
            Button(
                onClick = { onShareExport(path) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("分享刚刚导出的文件") }
            Text(
                "可发送到微信、飞书或已安装的 NAS 应用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("已生成的导出文件", style = MaterialTheme.typography.titleSmall)
                Text(
                    formatExportBytes(uiState.exportedFilesBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onClearExportedFiles,
                enabled = uiState.exportedFilesBytes > 0L
            ) { Text("清理") }
        }
    }
}

private fun formatExportBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable @OptIn(ExperimentalFoundationApi::class)
private fun ExportRangePicker(selectedStart: String, selectedEnd: String, markedDates: Set<String>, onSetExportStartDate: (String) -> Unit, onSetExportEndDate: (String) -> Unit, onConfirm: () -> Unit) {
    val baseMonth = remember { YearMonth.now() }
    val initialMonth = remember(selectedStart, selectedEnd) { when { selectedEnd.isNotBlank() -> YearMonth.from(LocalDate.parse(selectedEnd)); selectedStart.isNotBlank() -> YearMonth.from(LocalDate.parse(selectedStart)); else -> YearMonth.now() } }
    val pagerState = rememberPagerState(initialPage = ExportCalendarAnchorPage, pageCount = { 2400 })
    val scope = rememberCoroutineScope()
    val currentMonth = baseMonth.minusMonths((pagerState.currentPage - ExportCalendarAnchorPage).toLong())
    var showYearDialog by remember { mutableStateOf(false) }
    LaunchedEffect(initialMonth) { val tp = ExportCalendarAnchorPage - ChronoUnit.MONTHS.between(baseMonth.atDay(1), initialMonth.atDay(1)).toInt(); if (tp in 0 until pagerState.pageCount) pagerState.scrollToPage(tp) }
    if (showYearDialog) { val years = remember(currentMonth.year) { ((currentMonth.year - 15)..(currentMonth.year + 5)).toList().reversed() }; AlertDialog(onDismissRequest = { showYearDialog = false }, confirmButton = {}, title = { Text("选择年份") }, text = { LazyColumn(modifier = Modifier.height(320.dp)) { items(years) { y -> TextButton(onClick = { showYearDialog = false; val tm = YearMonth.of(y, currentMonth.monthValue); val tp2 = ExportCalendarAnchorPage - ChronoUnit.MONTHS.between(baseMonth.atDay(1), tm.atDay(1)).toInt(); scope.launch { pagerState.animateScrollToPage(tp2.coerceIn(0, pagerState.pageCount - 1)) } }, modifier = Modifier.fillMaxWidth()) { Text(y.toString()) } } } }) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { showYearDialog = true }) { Text("${currentMonth.year} 年") }; Text("${currentMonth.monthValue} 月", style = MaterialTheme.typography.titleMedium) }; TextButton(onClick = { onSetExportStartDate(""); onSetExportEndDate("") }) { Text("清空") } }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { t -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(t, style = MaterialTheme.typography.bodySmall) } } }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(280.dp)) { page ->
            val month = baseMonth.minusMonths((page - ExportCalendarAnchorPage).toLong())
            val fd = month.atDay(1); val dim = month.lengthOfMonth(); val lb = (fd.dayOfWeek.value + 6) % 7
            val cells: List<LocalDate?> = List(lb) { null } + (1..dim).map { month.atDay(it) }
            val rows = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { date ->
                            val dt = date?.toString()
                            val iS = dt == selectedStart; val iE = dt == selectedEnd
                            val iR = dt != null && selectedStart.isNotBlank() && selectedEnd.isNotBlank() && dt in selectedStart..selectedEnd
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(when {
                                        iS || iE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        iR -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    })
                                    .clickable(enabled = date != null) {
                                        dt?.let { d ->
                                            if (selectedStart.isBlank() || selectedEnd.isNotBlank()) { onSetExportStartDate(d); onSetExportEndDate("") }
                                            else if (d < selectedStart) { onSetExportStartDate(d); onSetExportEndDate("") }
                                            else onSetExportEndDate(d)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (date != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = if (iS || iE) FontWeight.SemiBold else FontWeight.Normal)
                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (dt in markedDates) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0f)))
                                    }
                                }
                            }
                        }
                        repeat(7 - row.size) { Box(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), enabled = selectedStart.isNotBlank() && selectedEnd.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("选好日期") }
    }
}

@Composable private fun SectionCard(content: @Composable () -> Unit) { Card(modifier = Modifier.fillMaxWidth().glassSurface(radius = 22.dp, alpha = 0.66f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } } }
@Composable private fun SubSectionCard(title: String, content: @Composable () -> Unit) { Card(modifier = Modifier.fillMaxWidth().glassSurface(radius = 18.dp, alpha = 0.56f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium); content() } } }
@Composable private fun SectionHeader(title: String, actionText: String, onAction: () -> Unit, enabled: Boolean = true) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); TextButton(onClick = onAction, enabled = enabled) { Text(actionText) } } }
@Composable private fun PreferenceRow(title: String, subtitle: String, trailing: @Composable () -> Unit) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; trailing() } }
@Composable private fun StatCard(modifier: Modifier, title: String, value: String) { Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) } } }

@Composable
private fun JournalOverviewCard(uiState: SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("记录总览", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${uiState.totalCount}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text("条生活片段", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("今天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${uiState.todayDreamCount} 条", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OverviewMetric("白天", uiState.dayRecordCount)
                OverviewMetric("夜间", uiState.dreamRecordCount)
                OverviewMetric("含录音", uiState.withAudioCount)
            }
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
