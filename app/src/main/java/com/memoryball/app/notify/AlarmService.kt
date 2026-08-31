package com.memoryball.app.notify

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.memoryball.app.R

/**
 * Foreground service that plays the alarm ringtone + vibration for alarm-style reminders.
 * Stays running until the user stops it (via notification action or RingActivity).
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // System restart with no intent: nothing to ring, drop the ghost service.
            startForeground(NOTIF_ID, buildServiceNotification())
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent.action == ACTION_STOP) {
            stopAlarm()
            ReminderNotifier.cancel(this, intent.getLongExtra(EXTRA_ID, -1))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildServiceNotification())

        val reminderId = intent.getLongExtra(EXTRA_ID, -1)
        if (reminderId != -1L) {
            startAlarm()
        }
        return START_NOT_STICKY
    }

    private fun buildServiceNotification(): Notification {
        val stopPi = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, com.memoryball.app.engine.AlarmReceiver::class.java).apply {
                action = com.memoryball.app.engine.AlarmReceiver.ACTION_DISMISS
                putExtra(EXTRA_ID, -1L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotifyHelper.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_memoryball)
            .setLargeIcon(ReminderNotifier.largeIcon(this))
            .setContentTitle("记忆球闹铃")
            .setContentText("正在响铃")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(0, "关闭", stopPi)
            .build()
    }

    private fun startAlarm() {
        stopAlarm()
        try {
            val uri = Settings.System.DEFAULT_ALARM_ALERT_URI
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600), 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 600, 400, 600), 0)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val EXTRA_ID = "reminder_id"
        const val ACTION_STOP = "com.memoryball.app.ACTION_STOP_ALARM"

        fun start(context: Context, reminderId: Long) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(EXTRA_ID, reminderId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopIntent(context: Context, reminderId: Long): Intent =
            Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_ID, reminderId)
            }
    }
}
