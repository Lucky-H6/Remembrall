package com.remembrall.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.remembrall.app.data.repo.DatabaseProvider
import com.remembrall.app.notify.AlarmService
import com.remembrall.app.notify.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles exact time alarms and alarm dismissal. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("reminder_id", -1)

        // Dismiss path: notification "关闭" button. Broadcasts are always delivered
        // promptly, unlike getService PendingIntents which MIUI may freeze/delay.
        if (intent.action == ACTION_DISMISS) {
            if (id != -1L) ReminderNotifier.cancel(context, id)
            // Stop the ringing service if it is alive; if it was already killed
            // there is nothing to stop (the MediaPlayer died with the process).
            runCatching { context.startService(AlarmService.stopIntent(context, id)) }
            // Tell RingActivity (if open) to close itself.
            context.sendBroadcast(
                Intent(ACTION_DISMISSED)
                    .setPackage(context.packageName)
                    .putExtra("reminder_id", id)
            )
            return
        }

        if (id == -1L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = DatabaseProvider.reminderDao(context)
                val reminder = dao.getById(id) ?: return@launch
                if (!reminder.enabled) return@launch
                
                // Verify the alarm is actually due (prevent early firing from system batching)
                val now = System.currentTimeMillis()
                val triggerAt = reminder.triggerAt
                if (triggerAt != null) {
                    val delay = triggerAt - now
                    val delaySec = delay / 1000
                    Log.d("AlarmReceiver", "reminder $id fired: expected at $triggerAt, actual at $now, diff=${delaySec}s")
                    
                    // If alarm fired more than 10 seconds early, reschedule instead of notifying
                    if (delay > 10_000) {
                        Log.w("AlarmReceiver", "alarm fired ${delay}ms early, rescheduling for correct time")
                        AlarmScheduler.schedule(context, reminder)
                        return@launch
                    }
                } else {
                    Log.d("AlarmReceiver", "reminder $id fired: no triggerAt (place-based or window-based)")
                }
                
                // Notify user
                ReminderNotifier.notify(context, reminder)
                
                // For one-time reminders: disable after firing and cancel the alarm
                if (reminder.repeatMode == com.remembrall.app.data.model.RepeatMode.ONCE) {
                    dao.setEnabled(id, false)
                    AlarmScheduler.cancel(context, id)
                    Log.d("AlarmReceiver", "one-time reminder $id disabled after firing")
                } else {
                    // Reschedule next occurrence for repeating reminders
                    AlarmScheduler.schedule(context, reminder)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.remembrall.app.ACTION_DISMISS"
        const val ACTION_DISMISSED = "com.remembrall.app.ALARM_DISMISSED"
    }
}
