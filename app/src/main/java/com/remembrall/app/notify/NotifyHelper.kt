package com.remembrall.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.provider.Settings

object NotifyHelper {
    const val CHANNEL_ALARM = "alarm_channel"
    const val CHANNEL_NOTIFY = "notify_channel"
    const val CHANNEL_MONITOR = "monitor_channel"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Alarm channel: highest importance + alarm sound + vibration + heads-up.
        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            "闹铃提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "记忆球闹铃式提醒"
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        // Plain (but persistent) notification channel.
        val notify = NotificationChannel(
            CHANNEL_NOTIFY,
            "常驻提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "记忆球常驻通知提醒"
            enableVibration(true)
        }

        // Low-priority channel for the location monitor foreground service.
        val monitor = NotificationChannel(
            CHANNEL_MONITOR,
            "位置监测",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "用于地点提醒的后台位置监测"
            setShowBadge(false)
        }

        nm.createNotificationChannels(listOf(alarm, notify, monitor))
    }
}
