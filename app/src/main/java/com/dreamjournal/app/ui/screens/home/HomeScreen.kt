package com.dreamjournal.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.ui.components.QingJiBrandTitle
import com.dreamjournal.app.ui.components.GlassBackground
import com.dreamjournal.app.ui.components.glassCardColor
import com.dreamjournal.app.ui.components.glassSurface
import com.dreamjournal.app.ui.theme.DayAccent
import com.dreamjournal.app.ui.theme.DayContainer
import com.dreamjournal.app.ui.theme.DayContainerDark
import com.dreamjournal.app.ui.theme.NightAccent
import com.dreamjournal.app.ui.theme.NightContainer
import com.dreamjournal.app.ui.theme.NightContainerDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.jsonArray

private enum class EntryFilter { ALL, DAY, NIGHT }

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartRecording: (RecordType) -> Unit,
    onStopRecording: () -> Unit,
    onCreateTextEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var filter by rememberSaveable { mutableStateOf(EntryFilter.ALL) }
    val visibleEntries = remember(uiState.entries, filter) {
        when (filter) {
            EntryFilter.ALL -> uiState.entries
            EntryFilter.DAY -> uiState.entries.filter { RecordType.fromStorage(it.recordType) == RecordType.DAY }
            EntryFilter.NIGHT -> uiState.entries.filter { RecordType.fromStorage(it.recordType) == RecordType.DREAM }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearError()
    }

    GlassBackground {
        Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeCaptureDock(
                isRecording = uiState.isRecording,
                isTranscribing = uiState.isTranscribing,
                recordingSeconds = uiState.recordingSeconds,
                waveformLevels = uiState.waveformLevels,
                activeRecordType = uiState.activeRecordType,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onCreateTextEntry = onCreateTextEntry
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { HomeHeader(uiState.weatherText, uiState.locationText, uiState.isLoadingWeather) }
            item {
                RecentHeader(
                    count = visibleEntries.size,
                    filter = filter,
                    onFilterChange = { filter = it }
                )
            }
            if (visibleEntries.isEmpty()) {
                item { EmptyState(filter) }
            } else {
                items(visibleEntries, key = { it.id }) { entry ->
                    TimelineEntry(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
    }
}

@Composable
private fun HomeCaptureDock(
    isRecording: Boolean,
    isTranscribing: Boolean,
    recordingSeconds: Int,
    waveformLevels: List<Float>,
    activeRecordType: RecordType,
    onStartRecording: (RecordType) -> Unit,
    onStopRecording: () -> Unit,
    onCreateTextEntry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(radius = 0.dp, alpha = 0.74f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (isTranscribing && !isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                )
                Text("正在转成文字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (isRecording) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                            Text(
                                if (activeRecordType == RecordType.DAY) "白天记录" else "夜间记录",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(formatDuration(recordingSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    CompactWaveform(
                        levels = waveformLevels,
                        color = if (activeRecordType == RecordType.DAY) DayAccent else NightAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(onClick = onStopRecording)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                        Text("完成", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomRecordButton(
                    modifier = Modifier.weight(1f),
                    label = "白天",
                    hint = "按住生活",
                    color = DayAccent,
                    onClick = { onStartRecording(RecordType.DAY) }
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable(onClick = onCreateTextEntry),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.headlineMedium)
                }
                BottomRecordButton(
                    modifier = Modifier.weight(1f),
                    label = "夜间",
                    hint = "按住梦与思绪",
                    color = NightAccent,
                    onClick = { onStartRecording(RecordType.DREAM) }
                )
            }
        }
    }
}

@Composable
private fun BottomRecordButton(
    modifier: Modifier,
    label: String,
    hint: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            MicrophoneGlyph(tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(hint, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun CompactWaveform(levels: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.takeLast(12).forEach { level ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((4 + 25 * level.coerceIn(0f, 1f)).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.82f))
            )
        }
    }
}

@Composable
private fun HomeHeader(weatherText: String?, locationText: String?, isLoadingWeather: Boolean) {
    val now = remember { Calendar.getInstance() }
    val dateText = remember {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(now.time)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QingJiBrandTitle(compact = true)
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    !weatherText.isNullOrBlank() -> listOfNotNull(
                        weatherText,
                        locationText?.takeIf(String::isNotBlank)
                    ).joinToString(" · ")
                    isLoadingWeather -> "天气获取中…"
                    else -> "天气暂不可用"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RecordSection(
    isRecording: Boolean,
    isTranscribing: Boolean,
    recordingSeconds: Int,
    waveformLevels: List<Float>,
    activeRecordType: RecordType,
    onStartRecording: (RecordType) -> Unit,
    onStopRecording: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = if (isRecording) "正在留住这一刻" else "现在，想记下什么？",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isRecording) "录音只保存在你的手机里" else "不必整理好，先说下来就好",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isRecording) {
            RecordingPanel(
                recordType = activeRecordType,
                seconds = recordingSeconds,
                levels = waveformLevels,
                onStop = onStopRecording
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecordChoice(
                    modifier = Modifier.weight(1f),
                    recordType = RecordType.DAY,
                    eyebrow = "白天",
                    title = "记录此刻",
                    subtitle = "见闻、想法、待办",
                    onClick = { onStartRecording(RecordType.DAY) }
                )
                RecordChoice(
                    modifier = Modifier.weight(1f),
                    recordType = RecordType.DREAM,
                    eyebrow = "夜间",
                    title = "记下梦境",
                    subtitle = "趁还记得，先说下来",
                    onClick = { onStartRecording(RecordType.DREAM) }
                )
            }
        }

        if (isTranscribing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("正在整理录音", style = MaterialTheme.typography.titleSmall)
                        Text("请稍候", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordChoice(
    modifier: Modifier,
    recordType: RecordType,
    eyebrow: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isDay = recordType == RecordType.DAY
    val accent = if (isDay) DayAccent else NightAccent
    val container = when {
        isDay && dark -> DayContainerDark
        isDay -> DayContainer
        dark -> NightContainerDark
        else -> NightContainer
    }
    Card(
        modifier = modifier
            .height(164.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.12f else 0.64f))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    MicrophoneGlyph(tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun RecordingPanel(
    recordType: RecordType,
    seconds: Int,
    levels: List<Float>,
    onStop: () -> Unit
) {
    val accent = if (recordType == RecordType.DAY) DayAccent else NightAccent
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassCardColor(0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Text(
                        if (recordType == RecordType.DAY) "白天记录" else "夜间梦境",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(formatDuration(seconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            WaveformBar(levels = levels, color = accent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("波形会跟随你的声音变化", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .clickable(onClick = onStop)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                    Text("完成", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WaveformBar(levels: List<Float>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.takeLast(18).forEach { level ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((5 + 33 * level.coerceIn(0f, 1f)).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.82f))
            )
        }
    }
}

@Composable
private fun RecentHeader(
    count: Int,
    filter: EntryFilter,
    onFilterChange: (EntryFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("最近记录", style = MaterialTheme.typography.titleLarge)
            Text("$count 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("全部", filter == EntryFilter.ALL) { onFilterChange(EntryFilter.ALL) }
            FilterPill("白天", filter == EntryFilter.DAY) { onFilterChange(EntryFilter.DAY) }
            FilterPill("夜间", filter == EntryFilter.NIGHT) { onFilterChange(EntryFilter.NIGHT) }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyState(filter: EntryFilter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Text(
            when (filter) {
                EntryFilter.ALL -> "这里会慢慢装下你的生活"
                EntryFilter.DAY -> "还没有白天记录"
                EntryFilter.NIGHT -> "还没有夜间记录"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text("从上面的录音入口开始第一条记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimelineEntry(entry: DreamEntryEntity, onClick: () -> Unit) {
    val type = RecordType.fromStorage(entry.recordType)
    val accent = if (type == RecordType.DAY) DayAccent else NightAccent
    val formattedTime = remember(entry.createdAt) {
        SimpleDateFormat("M月d日 EEE · HH:mm", Locale.CHINESE).format(Date(entry.createdAt))
    }
    val preview = entry.content.ifBlank { entry.transcript.orEmpty() }.ifBlank { "还没有文字内容，点击继续整理" }
    val tags = entry.dreamTag.split(",").map(String::trim)
        .filter { it.isNotBlank() && it !in setOf("白天", "夜间", "普通") }
        .take(2)
    val photoCount = encodedListSize(entry.photoPaths)
    val audioCount = (if (entry.audioPath.isNullOrBlank()) 0 else 1) + encodedListSize(entry.extraAudioPaths)
    val displayTitle = remember(entry.title, type) {
        val isOldDefault = entry.title.matches(Regex("^(白天|夜间)记录 \\d{2}-\\d{2} \\d{2}:\\d{2}$"))
        if (isOldDefault) {
            if (type == RecordType.DAY) "日常记录" else "梦境记录"
        } else entry.title
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .width(1.dp)
                    .height(118.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            displayTitle.ifBlank { "未命名记录" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOfNotNull(
                                formattedTime,
                                entry.weatherText?.takeIf(String::isNotBlank),
                                entry.locationText?.takeIf(String::isNotBlank)
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ChevronGlyph(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
                    Text(if (type == RecordType.DAY) "白天" else "夜间", style = MaterialTheme.typography.labelSmall, color = accent)
                    tags.forEach { MetaLabel(it, MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (audioCount > 0) MediaFact("录音 $audioCount")
                    if (photoCount > 0) MediaFact("照片 $photoCount")
                }
            }
        }
    }
}

@Composable
private fun MediaFact(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun encodedListSize(raw: String?): Int {
    if (raw.isNullOrBlank() || raw == "[]") return 0
    return runCatching { kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonArray.size }.getOrDefault(0)
}

@Composable
private fun MetaLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun formatDuration(totalSeconds: Int): String {
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun MicrophoneGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.05f),
            size = Size(w * 0.32f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.18f)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.27f),
            size = Size(w * 0.64f, h * 0.52f),
            style = Stroke(width = w * 0.10f)
        )
        drawLine(tint, Offset(w * 0.5f, h * 0.76f), Offset(w * 0.5f, h * 0.91f), strokeWidth = w * 0.10f)
        drawLine(tint, Offset(w * 0.31f, h * 0.92f), Offset(w * 0.69f, h * 0.92f), strokeWidth = w * 0.10f)
    }
}

@Composable
private fun ChevronGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawLine(tint, Offset(size.width * 0.35f, size.height * 0.25f), Offset(size.width * 0.62f, size.height * 0.5f), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(size.width * 0.62f, size.height * 0.5f), Offset(size.width * 0.35f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
    }
}
