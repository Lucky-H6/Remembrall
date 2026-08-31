package com.remembrall.app.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Self-drawn month calendar date picker.
 * Replaces the Material3 DatePicker, whose internal headline/title slots
 * left large blank areas and clipped CJK headlines on some devices.
 */
@Composable
fun DatePickerDialogOnly(
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initialDate = remember(initial) { Instant.ofEpochMilli(initial).atZone(zone).toLocalDate() }
    var selected by remember { mutableStateOf(initialDate) }
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = LocalDate.now(zone)
    val headlineFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
    val monthFmt = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(330.dp)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "选择日期",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = selected.format(headlineFmt),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))

                // ---- Month navigation ----
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上一月",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = month.format(monthFmt),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下一月",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ---- Weekday header (Monday first) ----
                Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                        Text(
                            text = wd,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ---- Day grid, 6 weeks ----
                val daysInMonth = month.lengthOfMonth()
                val offset = month.atDay(1).dayOfWeek.value - 1
                Column {
                    repeat(6) { weekIndex ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { dayIndex ->
                                val cell = weekIndex * 7 + dayIndex - offset + 1
                                val inMonth = cell in 1..daysInMonth
                                val cellDate = if (inMonth) month.atDay(cell) else null
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        cellDate == null -> {
                                            // blank cell outside the month
                                        }
                                        cellDate == selected -> Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cell.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                        else -> Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .then(
                                                    if (cellDate == today) {
                                                        Modifier.border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outline,
                                                            CircleShape
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clip(CircleShape)
                                                .clickable { selected = cellDate },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cell.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        onConfirm(selected.atStartOfDay(zone).toInstant().toEpochMilli())
                    }) { Text("确定") }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialogOnly(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("时", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    NumberWheel(
                        value = hour,
                        values = (0..23).toList(),
                        onValueChange = { hour = it }
                    )
                    Text(
                        ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    NumberWheel(
                        value = minute,
                        values = (0..59).toList(),
                        onValueChange = { minute = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("分", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(hour, minute) }) { Text("确定") }
                }
            }
        }
    }
}

/**
 * A wheel-style number picker: swipe up/down to change the value.
 * The centered row is the selected value; adjacent rows are dimmed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    value: Int,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Int = 48,
    visibleCount: Int = 5,
    label: (Int) -> String = { "%02d".format(it) }
) {
    val startIndex = (values.indexOf(value).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val padCount = visibleCount / 2

    // With vertical contentPadding of padCount*itemHeight, the item whose index
    // equals firstVisibleItemIndex sits exactly in the centered row.
    LaunchedEffect(listState, values) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx -> values.getOrNull(idx)?.let { onValueChange(it) } }
    }

    Box(
        modifier = modifier
            .width(72.dp)
            .height((itemHeight * visibleCount).dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(itemHeight.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                    RoundedCornerShape(12.dp)
                )
        )
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = (itemHeight * padCount).dp)
        ) {
            itemsIndexed(values) { index, v ->
                val centered = index == listState.firstVisibleItemIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(v),
                        fontSize = if (centered) 24.sp else 17.sp,
                        fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                        color = if (centered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
