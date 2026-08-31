package com.memoryball.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reschedule all reminders after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmScheduler.rescheduleAll(context)
                    LocationMonitorService.startIfNeeded(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "reschedule failed", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
