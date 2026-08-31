package com.memoryball.app.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.memoryball.app.R
import com.memoryball.app.data.model.Reminder
import com.memoryball.app.ui.ring.RingActivity

/** Builds and fires the actual reminder notifications (alarm-style or persistent). */
object ReminderNotifier {

    fun largeIcon(context: Context) =
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

    fun notify(context: Context, reminder: Reminder) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap opens the ring screen
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            RingActivity.intent(context, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action stops the alarm sound via broadcast (reliable on MIUI).
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            Intent(context, com.memoryball.app.engine.AlarmReceiver::class.java).apply {
                action = com.memoryball.app.engine.AlarmReceiver.ACTION_DISMISS
                putExtra("reminder_id", reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (reminder.alarmStyle) {
            // Full alarm: start the ringing foreground service AND post a full-screen notification.
            AlarmService.start(context, reminder.id)

            val fullScreen = PendingIntent.getActivity(
                context,
                reminder.id.toInt() + 100000,
                RingActivity.intent(context, reminder.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, NotifyHelper.CHANNEL_ALARM)
                .setSmallIcon(R.drawable.ic_stat_memoryball)
                .setLargeIcon(largeIcon(context))
                .setContentTitle(reminder.title)
                .setContentText(reminder.note.ifBlank { "记忆球提醒" })
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(fullScreen, true)
                .addAction(0, "关闭", dismissIntent)
                .build()
            nm.notify(reminder.id.toInt(), notif)
        } else {
            // Persistent notification: stays in the shade until user dismisses.
            val notif = NotificationCompat.Builder(context, NotifyHelper.CHANNEL_NOTIFY)
                .setSmallIcon(R.drawable.ic_stat_memoryball)
                .setLargeIcon(largeIcon(context))
                .setContentTitle(reminder.title)
                .setContentText(reminder.note.ifBlank { "记忆球提醒" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .addAction(0, "知道了", dismissIntent)
                .build()
            nm.notify(reminder.id.toInt(), notif)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(reminderId.toInt())
    }
}
