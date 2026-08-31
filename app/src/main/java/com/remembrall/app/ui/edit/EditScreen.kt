package com.remembrall.app.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remembrall.app.data.model.PlaceTrigger
import com.remembrall.app.data.model.RepeatMode
import com.remembrall.app.ui.common.IconBadge
import com.remembrall.app.ui.common.SectionCard
import com.remembrall.app.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    reminderId: Long,
    onBack: () -> Unit,
    onPickPlace: () -> Unit,
    vm: EditViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val places by vm.places.collectAsState()

    LaunchedEffect(reminderId) { vm.load(reminderId) }
    if (state.saved) {
        LaunchedEffect(Unit) { onBack() }
    }

    var showTriggerDatePicker by remember { mutableStateOf(false) }
    var showTriggerTimePicker by remember { mutableStateOf(false) }
    var showWindowStartDatePicker by remember { mutableStateOf(false) }
    var showWindowEndDatePicker by remember { mutableStateOf(false) }
    var showWindowStartPicker by remember { mutableStateOf(false) }
    var showWindowEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reminderId == -1L) "新建提醒" else "编辑提醒",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- 内容 ----
            SectionCard(title = "提醒内容", accentIcon = Icons.Default.Edit) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { vm.setTitle(it) },
                    label = { Text("提醒内容") },
                    placeholder = { Text("例如：带会议纪要") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { vm.setNote(it) },
                    label = { Text("备注（可选）") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- 提醒方式 ----
            SectionCard(title = "提醒方式", accentIcon = Icons.Default.Tune) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectableTile(
                        selected = state.alarmStyle,
                        icon = Icons.Default.Alarm,
                        title = "闹铃",
                        subtitle = "铃声 + 全屏",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setAlarmStyle(true) }
                    )
                    SelectableTile(
                        selected = !state.alarmStyle,
                        icon = Icons.Default.NotificationsActive,
                        title = "常驻通知",
                        subtitle = "通知栏不消失",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setAlarmStyle(false) }
                    )
                }
            }

            // ---- 重复（时间条件之前；非"仅一次"只有时间概念）----
            SectionCard(title = "重复", accentIcon = Icons.Default.Repeat) {
                SegmentedChips(
                    options = listOf(
                        RepeatMode.ONCE to "仅一次",
                        RepeatMode.DAILY to "每天",
                        RepeatMode.WEEKDAYS to "工作日",
                        RepeatMode.WEEKLY to "按周"
                    ),
                    selected = state.repeatMode,
                    onSelect = { vm.setRepeatMode(it) }
                )
                if (state.repeatMode == RepeatMode.WEEKLY) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Calendar.MONDAY to "一",
                            Calendar.TUESDAY to "二",
                            Calendar.WEDNESDAY to "三",
                            Calendar.THURSDAY to "四",
                            Calendar.FRIDAY to "五",
                            Calendar.SATURDAY to "六",
                            Calendar.SUNDAY to "日"
                        ).forEach { (day, label) ->
                            val bit = TimeUtils.DAY_SUNDAY shl (day - Calendar.SUNDAY)
                            val selected = (state.repeatDaysMask and bit) != 0
                            DayCircle(
                                label = label,
                                selected = selected,
                                onClick = { vm.toggleDay(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ---- 时间条件 ----
            SectionCard(title = "时间条件", accentIcon = Icons.Default.Schedule) {
                SegmentedChips(
                    options = listOf(
                        TimeMode.EXACT to "某个时间",
                        TimeMode.WINDOW to "时间段",
                        TimeMode.NONE to "不限时间"
                    ),
                    selected = state.timeMode,
                    onSelect = { vm.setTimeMode(it) }
                )
                Spacer(Modifier.height(12.dp))
                val repeating = state.repeatMode != RepeatMode.ONCE
                when (state.timeMode) {
                    TimeMode.EXACT -> {
                        if (repeating) {
                            TimeOnlyTile(
                                millis = state.triggerAt,
                                placeholder = "点击选择时间",
                                onClick = { showTriggerTimePicker = true }
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DateTile(
                                    millis = state.triggerAt,
                                    onClick = { showTriggerDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                                TimeOnlyTile(
                                    millis = state.triggerAt,
                                    placeholder = "时间",
                                    onClick = { showTriggerTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    TimeMode.WINDOW -> {
                        Text("开始时间", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        if (repeating) {
                            TimeOnlyTile(
                                millis = state.windowStart,
                                placeholder = "点击选择开始时间",
                                onClick = { showWindowStartPicker = true }
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DateTile(
                                    millis = state.windowStart,
                                    onClick = { showWindowStartDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                                TimeOnlyTile(
                                    millis = state.windowStart,
                                    placeholder = "时间",
                                    onClick = { showWindowStartPicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("结束时间", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        if (repeating) {
                            TimeOnlyTile(
                                millis = state.windowEnd,
                                placeholder = "点击选择结束时间",
                                onClick = { showWindowEndPicker = true }
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DateTile(
                                    millis = state.windowEnd,
                                    onClick = { showWindowEndDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                                TimeOnlyTile(
                                    millis = state.windowEnd,
                                    placeholder = "时间",
                                    onClick = { showWindowEndPicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HintLine("只设开始 = 「该时间之后」；只设结束 = 「该时间之前」")
                    }
                    TimeMode.NONE -> {}
                }
            }

            // ---- 地点条件 ----
            SectionCard(title = "地点条件", accentIcon = Icons.Default.Place) {
                SegmentedChips(
                    options = listOf(false to "不限地点", true to "关联地点"),
                    selected = state.hasPlace,
                    onSelect = { vm.setHasPlace(it) }
                )
                if (state.hasPlace) {
                    Spacer(Modifier.height(12.dp))
                    if (places.isEmpty()) {
                        FilledTonalButton(
                            onClick = onPickPlace,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.AddLocationAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("先去添加一个常用地点（如「家」「公司」）")
                        }
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = places.firstOrNull { it.id == state.placeId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("选择地点") },
                                label = { Text("地点") },
                                leadingIcon = { Icon(Icons.Default.Place, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(expanded, { expanded = false }) {
                                places.forEach { place ->
                                    DropdownMenuItem(
                                        text = { Text(place.name) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Place,
                                                null,
                                                Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            vm.setPlace(place.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("触发方式", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PlaceTrigger.ARRIVE to ("到达时" to Icons.Default.Login),
                            PlaceTrigger.LEAVE to ("离开时" to Icons.Default.Logout),
                            PlaceTrigger.INSIDE to ("处于该地" to Icons.Default.LocationOn),
                            PlaceTrigger.NOT_INSIDE to ("不处于该地" to Icons.Default.LocationOff)
                        ).forEach { (trigger, pair) ->
                            val (label, icon) = pair
                            FilterChip(
                                selected = state.placeTrigger == trigger,
                                onClick = { vm.setPlaceTrigger(trigger) },
                                label = { Text(label) },
                                leadingIcon = {
                                    if (state.placeTrigger == trigger) {
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    } else {
                                        Icon(icon, null, Modifier.size(18.dp))
                                    }
                                },
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                    if (state.placeTrigger == PlaceTrigger.INSIDE ||
                        state.placeTrigger == PlaceTrigger.NOT_INSIDE
                    ) {
                        Spacer(Modifier.height(8.dp))
                        HintLine("结合上方时间条件：在时间范围内满足地点条件时提醒")
                    }
                }
            }

            if (state.error != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(state.error!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { vm.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
            Text("保存提醒", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTriggerDatePicker) {
        DatePickerDialogOnly(
            initial = state.triggerAt ?: System.currentTimeMillis(),
            onConfirm = { vm.setTriggerDate(it); showTriggerDatePicker = false },
            onDismiss = { showTriggerDatePicker = false }
        )
    }
    val nowCal = Calendar.getInstance()
    if (showTriggerTimePicker) {
        TimePickerDialogOnly(
            initialHour = state.triggerAt?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: nowCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = state.triggerAt?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: nowCal.get(Calendar.MINUTE),
            onConfirm = { h, m -> vm.setTriggerTime(h, m); showTriggerTimePicker = false },
            onDismiss = { showTriggerTimePicker = false }
        )
    }
    if (showWindowStartDatePicker) {
        DatePickerDialogOnly(
            initial = state.windowStart ?: System.currentTimeMillis(),
            onConfirm = { vm.setWindowStartDate(it); showWindowStartDatePicker = false },
            onDismiss = { showWindowStartDatePicker = false }
        )
    }
    if (showWindowEndDatePicker) {
        DatePickerDialogOnly(
            initial = state.windowEnd ?: System.currentTimeMillis(),
            onConfirm = { vm.setWindowEndDate(it); showWindowEndDatePicker = false },
            onDismiss = { showWindowEndDatePicker = false }
        )
    }
    if (showWindowStartPicker) {
        TimePickerDialogOnly(
            initialHour = state.windowStart?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: nowCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = state.windowStart?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: nowCal.get(Calendar.MINUTE),
            onConfirm = { h, m -> vm.setWindowStartHourMinute(h, m); showWindowStartPicker = false },
            onDismiss = { showWindowStartPicker = false }
        )
    }
    if (showWindowEndPicker) {
        TimePickerDialogOnly(
            initialHour = state.windowEnd?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: nowCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = state.windowEnd?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: nowCal.get(Calendar.MINUTE),
            onConfirm = { h, m -> vm.setWindowEndHourMinute(h, m); showWindowEndPicker = false },
            onDismiss = { showWindowEndPicker = false }
        )
    }
}

/** Single-select row of chips rendered as a connected segmented control. */
@Composable
private fun <T> SegmentedChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            )
            .padding(3.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Surface(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(9.dp),
                color = if (isSel) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                contentColor = if (isSel)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = if (isSel) 2.dp else 0.dp,
                border = if (isSel)
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                else null
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableTile(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) cs.primaryContainer.copy(alpha = 0.45f) else cs.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) cs.primary else cs.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DateTile(
    millis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (millis != null) cs.surface else cs.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (millis != null) cs.primary.copy(alpha = 0.5f) else cs.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = if (millis != null) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "日期",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                if (millis != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    val dow = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[
                        cal.get(Calendar.DAY_OF_WEEK) - 1
                    ]
                    Text(
                        SimpleDateFormat("M月d日", Locale.CHINA).format(Date(millis)) + " " + dow,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        maxLines = 1
                    )
                } else {
                    Text(
                        "选择",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeOnlyTile(
    millis: Long?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (millis != null) cs.surface else cs.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (millis != null) cs.primary.copy(alpha = 0.5f) else cs.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (millis != null) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "时间",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                if (millis != null) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(millis)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        maxLines = 1
                    )
                } else {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCircle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) cs.primary else cs.surfaceContainerHighest,
            contentColor = if (selected) cs.onPrimary else cs.onSurfaceVariant,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HintLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
