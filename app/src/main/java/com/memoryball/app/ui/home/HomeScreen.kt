package com.memoryball.app.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memoryball.app.data.model.PlaceTrigger
import com.memoryball.app.data.model.RepeatMode
import com.memoryball.app.data.model.Reminder
import com.memoryball.app.ui.common.ConfirmDialog
import com.memoryball.app.ui.common.EmptyState
import com.memoryball.app.ui.common.IconBadge
import com.memoryball.app.ui.common.MetaPill
import com.memoryball.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onPlaces: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val reminders by vm.reminders.collectAsState()
    val places by vm.places.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "记忆球",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "时间 · 地点，时空闹钟",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = onPlaces,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("管理地点", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewReminder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建提醒", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        val context = LocalContext.current
        var resumeTick by remember { mutableIntStateOf(0) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) resumeTick++
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val missing = remember(resumeTick) { missingSetupItems(context) }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (missing.isNotEmpty()) {
                SetupGuideCard(
                    missing = missing,
                    onClick = onSettings,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            if (reminders.isEmpty()) {
                EmptyState(
                    title = "还没有提醒",
                    description = "点击右下角的「新建提醒」，\n让记忆球在合适的时间、合适的地点提醒你",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "全部提醒",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${reminders.count { it.enabled }} / ${reminders.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        placeName = places.firstOrNull { it.id == reminder.placeId }?.name,
                        onClick = { onEditReminder(reminder.id) },
                        onToggle = { enabled -> vm.setEnabled(reminder, enabled) },
                        onDelete = { vm.delete(reminder) }
                    )
                }
                }
            }
        }
    }
}

/** Items the user still needs to grant/enable for reminders to fire reliably. */
private fun missingSetupItems(context: Context): List<String> {
    val out = mutableListOf<String>()
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        out.add("通知权限")
    }
    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        out.add("位置权限")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        out.add("后台定位")
    }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        alarmManager != null && !alarmManager.canScheduleExactAlarms()
    ) {
        out.add("精确闹钟")
    }
    if (!Settings.canDrawOverlays(context)) {
        out.add("悬浮窗")
    }
    return out
}

@Composable
private fun SetupGuideCard(
    missing: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "完成设置，提醒更可靠",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "尚未开启：${missing.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "去设置",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    placeName: String?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dimmed = !reminder.enabled

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconBadge(
                icon = if (reminder.alarmStyle) Icons.Default.Alarm else Icons.Default.NotificationsActive,
                containerColor = if (reminder.alarmStyle)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (reminder.alarmStyle)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (dimmed) 0.5f else 1f)
            ) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reminder.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        reminder.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (dimmed) {
                        MetaPill(
                            icon = Icons.Default.Pause,
                            text = "已暂停",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    conditionPills(reminder, placeName)
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(x = 6.dp)
            ) {
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle,
                    thumbContent = {
                        if (reminder.enabled) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        }
                    }
                )
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除提醒？",
            text = "确定要删除「${reminder.title}」吗？此操作不可撤销。",
            icon = Icons.Default.DeleteForever,
            confirmLabel = "删除",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun conditionPills(r: Reminder, placeName: String?) {
    val cs = MaterialTheme.colorScheme
    // Time
    when {
        r.triggerAt != null -> MetaPill(
            icon = Icons.Default.Schedule,
            text = TimeUtils.formatDateTime(r.triggerAt),
            containerColor = cs.surfaceContainerHigh,
            contentColor = cs.onSurfaceVariant
        )
        r.windowStart != null || r.windowEnd != null -> {
            val s = r.windowStart?.let { TimeUtils.formatDateTime(it) } ?: "…"
            val e = r.windowEnd?.let { TimeUtils.formatDateTime(it) } ?: "…"
            MetaPill(
                icon = Icons.Default.Schedule,
                text = "$s ~ $e",
                containerColor = cs.surfaceContainerHigh,
                contentColor = cs.onSurfaceVariant
            )
        }
    }
    // Repeat
    val repeat = when (r.repeatMode) {
        RepeatMode.DAILY -> "每天"
        RepeatMode.WEEKDAYS -> "工作日"
        RepeatMode.WEEKLY -> "每周" + weeklyDayLabels(r.repeatDaysMask)
        else -> null
    }
    if (repeat != null) {
        MetaPill(
            icon = Icons.Default.Repeat,
            text = repeat,
            containerColor = cs.surfaceContainerHigh,
            contentColor = cs.onSurfaceVariant
        )
    }
    // Place
    if (r.placeId != null && r.placeTrigger != null) {
        val p = placeName ?: "地点"
        val (label, icon) = when (r.placeTrigger) {
            PlaceTrigger.ARRIVE -> "到达${p}" to Icons.Default.Login
            PlaceTrigger.LEAVE -> "离开${p}" to Icons.Default.Logout
            PlaceTrigger.INSIDE -> "在${p}时" to Icons.Default.LocationOn
            PlaceTrigger.NOT_INSIDE -> "不在${p}时" to Icons.Default.LocationOff
        }
        MetaPill(
            icon = icon,
            text = label,
            containerColor = cs.tertiaryContainer.copy(alpha = 0.6f),
            contentColor = cs.onTertiaryContainer
        )
    }
}

private fun weeklyDayLabels(mask: Int): String {
    val days = listOf(
        TimeUtils.DAY_MON to "一",
        TimeUtils.DAY_TUE to "二",
        TimeUtils.DAY_WED to "三",
        TimeUtils.DAY_THU to "四",
        TimeUtils.DAY_FRI to "五",
        TimeUtils.DAY_SAT to "六",
        TimeUtils.DAY_SUN to "日"
    )
    val picked = days.filter { (mask and it.first) != 0 }.map { it.second }
    return if (picked.isEmpty() || picked.size == 7) "" else "·" + picked.joinToString("")
}
