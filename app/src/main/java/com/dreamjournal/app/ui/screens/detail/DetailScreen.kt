package com.dreamjournal.app.ui.screens.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dreamjournal.app.data.local.AiMessageEntity
import com.dreamjournal.app.domain.model.ContentBlockType
import com.dreamjournal.app.domain.model.EntryContentBlock
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.model.TodoItem
import com.dreamjournal.app.ui.components.GlassBackground
import com.dreamjournal.app.ui.components.glassCardColor
import com.dreamjournal.app.ui.components.glassSurface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onUpdateTextBlock: (Long, String) -> Unit,
    onFocusTextBlock: (Long) -> Unit,
    onSetImageWidth: (Long, Int) -> Unit,
    onAddPhotos: (List<Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onAddTodo: () -> Unit,
    onUpdateTodoText: (Long, String) -> Unit,
    onToggleTodo: (Long) -> Unit,
    onRemoveTodo: (Long) -> Unit,
    onSaveDraft: () -> Unit,
    onRunAnalysis: () -> Unit,
    onChatInputChange: (String) -> Unit,
    onSendChat: () -> Unit,
    onDeleteEntry: () -> Unit,
    onToggleAudio: (String) -> Unit,
    onTranscribeAudio: (String) -> Unit,
    onStartAdditionalRecording: () -> Unit,
    onStopAdditionalRecording: () -> Unit,
    onClearError: () -> Unit,
    onClearStatus: () -> Unit
) {
    val entry = uiState.entry
    val recordType = RecordType.fromStorage(entry?.recordType)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) onAddPhotos(uris) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onAddPhotos(listOf(uri))
        pendingCameraUri = null
    }
    val additionalRecordingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartAdditionalRecording()
        else scope.launch { snackbarHostState.showSnackbar("需要麦克风权限才能追加录音") }
    }

    fun startAdditionalRecordingWithPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartAdditionalRecording() else additionalRecordingPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearStatus()
    }

    GlassBackground {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("记录详情", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (entry != null) {
                            val formattedTime = remember(entry.createdAt) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
                            }
                            Text(formattedTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    TextButton(onClick = onSaveDraft) { Text("保存") }
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("删除记录") }, onClick = { moreMenuExpanded = false; onDeleteEntry() })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.errorMessage.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearError) { Text("关闭") }
                    }
                }
            }

            OutlinedTextField(value = uiState.draftTitle, onValueChange = onTitleChange, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("输入标题…") }, shape = RoundedCornerShape(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(recordType.titleLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("·", color = MaterialTheme.colorScheme.outline)
                Text(
                    listOfNotNull(
                        entry.weatherText?.takeIf(String::isNotBlank),
                        entry.locationText?.takeIf(String::isNotBlank)
                    ).joinToString(" · ").ifBlank { "未记录天气和位置" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InlineContentEditor(
                blocks = uiState.draftBlocks,
                onTextChange = onUpdateTextBlock,
                onFocusText = onFocusTextBlock,
                onSetImageWidth = onSetImageWidth,
                onRemoveImage = onRemovePhoto
            )

            AttachmentToolbar(
                isAnalyzing = uiState.isAnalyzing,
                isRecording = uiState.isRecordingMore,
                onTakePhoto = {
                    runCatching { createCameraUri(context) }
                        .onSuccess { uri -> pendingCameraUri = uri; cameraLauncher.launch(uri) }
                        .onFailure { error -> scope.launch { snackbarHostState.showSnackbar("无法打开相机：${error.message}") } }
                },
                onPickPhotos = { photoPickerLauncher.launch("image/*") },
                onRecordAudio = ::startAdditionalRecordingWithPermission,
                onAddTodo = onAddTodo,
                onAnalyze = onRunAnalysis
            )

            if (uiState.isRecordingMore) {
                AdditionalRecordingPanel(
                    seconds = uiState.additionalRecordingSeconds,
                    levels = uiState.additionalWaveformLevels,
                    onStop = onStopAdditionalRecording
                )
            }

            if (uiState.audioPaths.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("录音片段", style = MaterialTheme.typography.titleSmall)
                            Text("${uiState.audioPaths.size} 段", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        uiState.audioPaths.forEachIndexed { index, path ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) { Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                                Text("第 ${index + 1} 段录音", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { onToggleAudio(path) }) {
                                    Text(if (uiState.playingAudioPath == path) "停止" else "播放")
                                }
                                TextButton(onClick = { onTranscribeAudio(path) }, enabled = !uiState.isTranscribingAudio) { Text("转文字") }
                            }
                        }
                    }
                }
            }

            if (uiState.draftTodos.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("待办事项", style = MaterialTheme.typography.titleSmall)
                        uiState.draftTodos.forEach { todo ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = todo.isDone, onCheckedChange = { onToggleTodo(todo.id) })
                                OutlinedTextField(value = todo.text, onValueChange = { onUpdateTodoText(todo.id, it) }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("待办") }, shape = RoundedCornerShape(8.dp))
                                TextButton(onClick = { onRemoveTodo(todo.id) }) { Text("删除") }
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI 分析", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(entry.aiSummary?.takeIf { it.isNotBlank() } ?: "点击上方「AI 分析」让 AI 帮你整理这条记录。", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("AI 对话", style = MaterialTheme.typography.titleSmall)
            Column(
                modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    Text("和 AI 继续聊聊这条记录…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                uiState.messages.forEach { msg ->
                    val isUser = msg.role != "assistant"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isUser) "我" else "AI", style = MaterialTheme.typography.labelSmall, color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = uiState.chatInput, onValueChange = onChatInputChange, modifier = Modifier.weight(1f), placeholder = { Text("继续问 AI…") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Button(onClick = onSendChat, enabled = !uiState.isSendingChat, shape = RoundedCornerShape(12.dp)) { Text(if (uiState.isSendingChat) "…" else "发送") }
            }

            TagSelector(
                availableTags = uiState.availableTags,
                selectedTags = uiState.draftTags,
                onToggle = onTagToggle
            )
        }
    }
}
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TagSelector(
    availableTags: List<String>,
    selectedTags: List<String>,
    onToggle: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val topicTags = listOf("日常", "工作", "灵感", "旅行", "家人", "健康")
    val feelingTags = listOf("开心", "平静", "疲惫", "低落", "噩梦", "混乱")
    val customTags = availableTags.filterNot { it in topicTags || it in feelingTags }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glassCardColor(0.58f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("标签", style = MaterialTheme.typography.titleSmall)
                Text(
                    selectedTags.joinToString(" · ").ifBlank { "尚未添加标签" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TagGroup("主题", topicTags.filter(availableTags::contains), selectedTags, onToggle)
                TagGroup("感受", feelingTags.filter(availableTags::contains), selectedTags, onToggle)
                if (customTags.isNotEmpty()) TagGroup("自定义", customTags, selectedTags, onToggle)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TagGroup(
    label: String,
    tags: List<String>,
    selectedTags: List<String>,
    onToggle: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp).padding(top = 10.dp)
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag),
                    onClick = { onToggle(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

@Composable
private fun InlineContentEditor(
    blocks: List<EntryContentBlock>,
    onTextChange: (Long, String) -> Unit,
    onFocusText: (Long) -> Unit,
    onSetImageWidth: (Long, Int) -> Unit,
    onRemoveImage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(glassCardColor(0.62f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block ->
            when (block.type) {
                ContentBlockType.TEXT -> {
                    BasicTextField(
                        value = block.text,
                        onValueChange = { onTextChange(block.id, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp)
                            .onFocusChanged { if (it.isFocused) onFocusText(block.id) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (block.text.isBlank()) {
                                    Text("写下这一刻…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        }
                    )
                }

                ContentBlockType.IMAGE -> {
                    InlineImageBlock(
                        block = block,
                        onSetWidth = { onSetImageWidth(block.id, it) },
                        onRemove = { onRemoveImage(block.path) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineImageBlock(
    block: EntryContentBlock,
    onSetWidth: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val bitmap = remember(block.path) { runCatching { BitmapFactory.decodeFile(block.path) }.getOrNull() }
    val fraction = block.widthPercent.coerceIn(50, 100) / 100f
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (bitmap != null) {
            val ratio = (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)).coerceIn(0.75f, 1.9f)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "正文图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height((220 / ratio).coerceIn(130f, 280f).dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(fraction).height(140.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) { Text("图片无法预览", style = MaterialTheme.typography.bodySmall) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(50 to "小", 75 to "中", 100 to "满宽").forEach { (size, label) ->
                TextButton(onClick = { onSetWidth(size) }) {
                    Text(label, color = if (block.widthPercent == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onRemove) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AttachmentToolbar(
    isAnalyzing: Boolean,
    isRecording: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhotos: () -> Unit,
    onRecordAudio: () -> Unit,
    onAddTodo: () -> Unit,
    onAnalyze: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .glassSurface(radius = 16.dp, alpha = 0.68f)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onTakePhoto) { Text("拍照") }
        TextButton(onClick = onPickPhotos) { Text("相册") }
        TextButton(onClick = onRecordAudio, enabled = !isRecording) { Text(if (isRecording) "录音中…" else "追加录音") }
        TextButton(onClick = onAddTodo) { Text("待办") }
        TextButton(onClick = onAnalyze, enabled = !isAnalyzing) { Text(if (isAnalyzing) "分析中…" else "AI 分析") }
    }
}

@Composable
private fun AdditionalRecordingPanel(
    seconds: Int,
    levels: List<Float>,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("正在追加录音", style = MaterialTheme.typography.titleSmall)
                    Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onStop, shape = RoundedCornerShape(12.dp)) { Text("完成") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                levels.takeLast(12).forEach { level ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((5 + 25 * level.coerceIn(0f, 1f)).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                    )
                }
            }
            Text("完成后会先保存录音，再尝试把文字追加到正文。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
