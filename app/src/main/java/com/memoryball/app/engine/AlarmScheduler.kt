package com.memoryball.app.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.memoryball.app.MainActivity
import com.memoryball.app.data.model.Reminder
import com.memoryball.app.data.model.RepeatMode
import com.memoryball.app.data.repo.DatabaseProvider
import com.memoryball.app.util.TimeUtils

/**
 * Schedules exact wake-up alarms for time-based reminders using AlarmManager.
 * Pure place-based reminders are handled by [LocationMonitorService] instead.
 */
object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val EXTRA_REMINDER_ID = "reminder_id"

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.memoryball.app.ACTION_TIME_ALARM"
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Compute when this reminder should next fire (time condition), or null if not time-based/scheduled. */
    fun nextTriggerMillis(reminder: Reminder, now: Long = System.currentTimeMillis()): Long? {
        if (!reminder.enabled) return null
        if (!reminder.hasTimeCondition) return null

        return when {
            reminder.triggerAt != null && reminder.repeatMode == RepeatMode.ONCE -> {
                // For one-time reminders, only fire if strictly in the future
                if (reminder.triggerAt > now) reminder.triggerAt else null
            }
            reminder.triggerAt != null -> {
                val mask = when (reminder.repeatMode) {
                    RepeatMode.DAILY -> TimeUtils.EVERYDAY_MASK
                    RepeatMode.WEEKDAYS -> TimeUtils.WEEKDAYS_MASK
                    RepeatMode.WEEKLY -> reminder.repeatDaysMask
                    RepeatMode.ONCE -> 0
                }
                val next = TimeUtils.nextRepeating(reminder.triggerAt, mask, now)
                if (next > now) next else null
            }
            else -> null
        }
    }

    fun schedule(context: Context, reminder: Reminder) {
        val at = nextTriggerMillis(reminder)
        if (at == null) {
            cancel(context, reminder.id)
            return
        }
        val am = alarmManager(context)
        val pi = pendingIntent(context, reminder.id)
        val show = PendingIntent.getActivity(
            context,
            reminder.id.toInt() + 300_000,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // setAlarmClock is treated as a user-visible alarm on MIUI/HyperOS
            // and is not batched/folded like setExactAndAllowWhileIdle.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
            Log.d(TAG, "scheduled reminder ${reminder.id} at $at (${java.util.Date(at)})")
        } catch (e: SecurityException) {
            Log.e(TAG, "exact alarm not allowed", e)
            am.set(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        alarmManager(context).cancel(pendingIntent(context, reminderId))
    }

    /** Reschedule every enabled time-based reminder (after boot / update). */
    suspend fun rescheduleAll(context: Context) {
        val dao = DatabaseProvider.reminderDao(context)
        dao.getEnabled().forEach { schedule(context, it) }
    }
}
