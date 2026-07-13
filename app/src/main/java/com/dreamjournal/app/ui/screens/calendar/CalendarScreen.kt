package com.dreamjournal.app.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.ui.components.GlassBackground
import com.dreamjournal.app.ui.components.glassCardColor
import com.dreamjournal.app.ui.theme.DayAccent
import com.dreamjournal.app.ui.theme.NightAccent
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val weekTitles = listOf("一", "二", "三", "四", "五", "六", "日")
private const val CalendarAnchorPage = 1200

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CalendarScreen(
    uiState: CalendarUiState,
    onDateSelected: (String) -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    var showYearDialog by remember { mutableStateOf(false) }
    val baseMonth = remember { YearMonth.now() }
    val scope = rememberCoroutineScope()
    val selectedMonth = remember(uiState.selectedDate) {
        YearMonth.from(LocalDate.parse(uiState.selectedDate))
    }
    val pagerState = rememberPagerState(initialPage = CalendarAnchorPage, pageCount = { 2400 })
    val currentMonth = baseMonth.minusMonths((pagerState.currentPage - CalendarAnchorPage).toLong())

    LaunchedEffect(uiState.selectedDate) {
        val targetPage = CalendarAnchorPage -
            ChronoUnit.MONTHS.between(baseMonth.atDay(1), selectedMonth.atDay(1)).toInt()
        if (targetPage in 0 until pagerState.pageCount) pagerState.scrollToPage(targetPage)
    }

    if (showYearDialog) {
        YearPickerDialog(
            currentYear = currentMonth.year,
            onDismiss = { showYearDialog = false },
            onYearSelected = { year ->
                showYearDialog = false
                val targetMonth = YearMonth.of(year, currentMonth.monthValue)
                val targetPage = CalendarAnchorPage -
                    ChronoUnit.MONTHS.between(baseMonth.atDay(1), targetMonth.atDay(1)).toInt()
                scope.launch { pagerState.animateScrollToPage(targetPage.coerceIn(0, pagerState.pageCount - 1)) }
            }
        )
    }

    GlassBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CalendarHeader(onToday = { onDateSelected(LocalDate.now().toString()) })
            }
            item {
                CalendarPanel(
                    currentMonth = currentMonth,
                    selectedDate = uiState.selectedDate,
                    markedDates = uiState.markedDates,
                    pagerState = pagerState,
                    baseMonth = baseMonth,
                    onYearClick = { showYearDialog = true },
                    onDateSelected = onDateSelected
                )
            }
            item {
                SelectedDateHeader(date = uiState.selectedDate, count = uiState.entries.size)
            }
            if (uiState.entries.isEmpty()) {
                item { EmptyDayCard() }
            } else {
                items(uiState.entries, key = { it.id }) { entry ->
                    CalendarEntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
    }
}

@Composable
private fun CalendarHeader(onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("日历", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("白天与夜间，都在这里留下痕迹", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onToday) { Text("回到今天") }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CalendarPanel(
    currentMonth: YearMonth,
    selectedDate: String,
    markedDates: Map<String, DayMarkers>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    baseMonth: YearMonth,
    onYearClick: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onYearClick)
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                    Text("${currentMonth.monthValue} 月", style = MaterialTheme.typography.headlineSmall)
                }
                Text("左右滑动切换", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                weekTitles.forEach { title ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(272.dp)
            ) { page ->
                val month = baseMonth.minusMonths((page - CalendarAnchorPage).toLong())
                MonthPage(
                    month = month,
                    selectedDate = selectedDate,
                    markedDates = markedDates,
                    onDateSelected = onDateSelected
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(DayAccent)
                Text("白天记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.width(16.dp))
                LegendDot(NightAccent)
                Text("夜间记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 5.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MonthPage(
    month: YearMonth,
    selectedDate: String,
    markedDates: Map<String, DayMarkers>,
    onDateSelected: (String) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value + 6) % 7
    val cells: List<LocalDate?> = List(leadingBlanks) { null } +
        (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        cells.chunked(7).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { date ->
                    CalendarDayCell(
                        modifier = Modifier.weight(1f),
                        date = date,
                        isSelected = date?.toString() == selectedDate,
                        isToday = date == LocalDate.now(),
                        markers = date?.toString()?.let { markedDates[it] } ?: DayMarkers(),
                        onClick = { date?.let { onDateSelected(it.toString()) } }
                    )
                }
                repeat(7 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier,
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    markers: DayMarkers,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(43.dp)
            .clickable(enabled = date != null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (date != null) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            MarkerDot(markers.hasDayRecord, DayAccent)
            MarkerDot(markers.hasDreamRecord, NightAccent)
        }
    }
}

@Composable
private fun MarkerDot(visible: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(if (visible) color else Color.Transparent)
    )
}

@Composable
private fun SelectedDateHeader(date: String, count: Int) {
    val label = remember(date) {
        runCatching {
            LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE))
        }.getOrDefault(date)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
        Text("$count 条记录", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyDayCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )
        Text("这一天还没有记录", style = MaterialTheme.typography.titleMedium)
        Text("回到记录页，留下一段声音或文字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarEntryCard(entry: DreamEntryEntity, onClick: () -> Unit) {
    val type = RecordType.fromStorage(entry.recordType)
    val accent = if (type == RecordType.DAY) DayAccent else NightAccent
    val time = remember(entry.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
    }
    val preview = entry.content.ifBlank { entry.transcript.orEmpty() }.ifBlank { "暂无文字内容" }
    val tags = entry.dreamTag.split(",").map(String::trim)
        .filter { it.isNotBlank() && it !in setOf("普通", "白天", "夜间") }
        .take(2)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = glassCardColor(0.70f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(124.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        entry.title.ifBlank { "未命名记录" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            time,
                            entry.weatherText?.takeIf(String::isNotBlank),
                            entry.locationText?.takeIf(String::isNotBlank)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CalendarChevron(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    preview,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
                    Text(if (type == RecordType.DAY) "白天" else "夜间", style = MaterialTheme.typography.labelSmall, color = accent)
                    tags.forEach { CalendarMeta(it, MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun CalendarMeta(text: String, color: Color) {
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

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val years = remember(currentYear) { ((currentYear - 30)..(currentYear + 5)).toList().reversed() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("选择年份") },
        text = {
            LazyColumn(modifier = Modifier.height(340.dp)) {
                items(years) { year ->
                    TextButton(onClick = { onYearSelected(year) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            year.toString(),
                            fontWeight = if (year == currentYear) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CalendarChevron(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawLine(tint, Offset(size.width * 0.35f, size.height * 0.25f), Offset(size.width * 0.62f, size.height * 0.5f), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(size.width * 0.62f, size.height * 0.5f), Offset(size.width * 0.35f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
    }
}
