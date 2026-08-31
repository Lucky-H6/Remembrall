package com.remembrall.app.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.remembrall.app.ui.common.SectionCard

/** 权限状态：已授权 / 未授权 / 无法检测（需手动确认）。 */
private enum class PermState { Granted, Missing, Manual }

/** Settings: permissions status + MIUI reliability guidance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // 回到前台时刷新 + 停留在本页期间轻量轮询，确保在系统设置里收回权限后状态及时更新
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            resumeTick++
        }
    }
    // 不加 remember：读取 resumeTick 建立重组依赖，每次重组实时查询系统权限状态
    @Suppress("UNUSED_VARIABLE")
    val tick = resumeTick
    val states: Map<String, PermState> = permissionStates(context)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
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
            SectionCard(title = "系统权限", accentIcon = Icons.Default.Key) {
                SettingRow(
                    icon = Icons.Default.PlayCircle,
                    title = "允许自启动",
                    state = states["autostart"] ?: PermState.Manual,
                    onClick = { openAppDetails(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SettingRow(
                    icon = Icons.Default.BatterySaver,
                    title = "省电策略「无限制」",
                    state = states["battery"] ?: PermState.Missing,
                    onClick = { requestIgnoreBatteryOptimizations(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SettingRow(
                    icon = Icons.Default.GpsFixed,
                    title = "允许后台定位",
                    state = states["bgLocation"] ?: PermState.Missing,
                    onClick = { openAppDetails(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SettingRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "允许通知",
                    state = states["notification"] ?: PermState.Missing,
                    onClick = { openNotificationSettings(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SettingRow(
                    icon = Icons.Default.Layers,
                    title = "悬浮窗权限",
                    state = states["overlay"] ?: PermState.Missing,
                    onClick = { openOverlaySettings(context) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SettingRow(
                        icon = Icons.Default.Alarm,
                        title = "精确闹钟",
                        state = states["exactAlarm"] ?: PermState.Missing,
                        onClick = { openExactAlarmSettings(context) }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SettingRow(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "允许后台弹出界面",
                    state = states["bgPopup"] ?: PermState.Manual,
                    onClick = { openAppDetails(context) }
                )
            }

            SectionCard(title = "关于提醒方式", accentIcon = Icons.Default.Info) {
                ReminderModeRow(
                    icon = Icons.Default.Alarm,
                    title = "闹铃",
                    desc = "系统闹铃音频通道 + 全屏提醒"
                )
                Spacer(Modifier.height(10.dp))
                ReminderModeRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "常驻通知",
                    desc = "通知栏长期停留"
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    state: PermState,
    onClick: () -> Unit
) {
    val green = Color(0xFF1B873F)
    val red = Color(0xFFD32F2F)
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    val spec = when (state) {
        PermState.Granted -> RowSpec(
            iconTint = green, statusColor = green,
            statusIcon = Icons.Filled.CheckCircle, statusDesc = "已授权", badge = true)
        PermState.Missing -> RowSpec(
            iconTint = red, statusColor = red,
            statusIcon = Icons.Filled.Cancel, statusDesc = "未授权", badge = true)
        PermState.Manual -> RowSpec(
            iconTint = MaterialTheme.colorScheme.primary, statusColor = gray,
            statusIcon = Icons.Filled.HelpOutline, statusDesc = "点击进入并确认", badge = false)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (spec.badge) {
            Box {
                Icon(icon, contentDescription = null, tint = spec.iconTint)
                // 主图标右下角叠加状态小图标，一眼可辨
                Icon(
                    spec.statusIcon,
                    contentDescription = spec.statusDesc,
                    tint = spec.statusColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        } else {
            Icon(icon, contentDescription = null, tint = spec.iconTint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(spec.statusDesc, style = MaterialTheme.typography.bodySmall, color = spec.statusColor)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

/** 权限行的视觉规格。 */
private data class RowSpec(
    val iconTint: Color,
    val statusColor: Color,
    val statusIcon: ImageVector,
    val statusDesc: String,
    val badge: Boolean
)

/** 检测各权限当前状态；MIUI 专属开关无法程序读取，标记为 Manual。 */
private fun permissionStates(context: Context): Map<String, PermState> {
    fun granted(vararg perms: String): PermState {
        val ok = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return if (ok) PermState.Granted else PermState.Missing
    }
    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val battery = if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
        PermState.Granted
    } else {
        PermState.Missing
    }
    val bgLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        PermState.Granted
    }
    val notification = if (
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    ) PermState.Granted else PermState.Missing
    val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        Settings.canDrawOverlays(context)
    ) PermState.Granted else PermState.Missing
    val exactAlarm = if (isExactAlarmAllowed(context)) PermState.Granted else PermState.Missing
    return mapOf(
        "autostart" to PermState.Manual,
        "battery" to battery,
        "bgLocation" to bgLocation,
        "notification" to notification,
        "overlay" to overlay,
        "exactAlarm" to exactAlarm,
        "bgPopup" to PermState.Manual
    )
}

@Composable
private fun ReminderModeRow(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openAppDetails(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {
    }
}

private fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
        }
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun openOverlaySettings(context: android.content.Context) {
    // 1) MIUI/HyperOS per-app permission editor: always shows this app's
    //    "显示在其他应用上层" toggle, unlike the global list which often
    //    omits freshly installed apps.
    try {
        context.startActivity(
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return
    } catch (_: Exception) {
    }
    // 2) AOSP overlay permission page scoped to this package.
    try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return
    } catch (_: Exception) {
    }
    // 3) Last resort: app info page.
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {
    }
}

private fun isExactAlarmAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
    return am.canScheduleExactAlarms()
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }
}
