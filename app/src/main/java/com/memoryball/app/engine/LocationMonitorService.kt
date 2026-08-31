package com.memoryball.app.engine

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.memoryball.app.MainActivity
import com.memoryball.app.R
import com.memoryball.app.data.model.PlaceTrigger
import com.memoryball.app.data.model.Reminder
import com.memoryball.app.data.model.RepeatMode
import com.memoryball.app.data.repo.DatabaseProvider
import com.memoryball.app.notify.NotifyHelper
import com.memoryball.app.notify.ReminderNotifier
import com.memoryball.app.util.GeoUtils
import com.memoryball.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import kotlin.coroutines.resume

/**
 * Evaluates place-based / combined (time+place) reminders with on-demand location.
 *
 * Battery strategy:
 *  - No enabled place reminders          -> service stops itself, no notification.
 *  - Time gate not open yet              -> exact wake alarm at gate, service stops.
 *  - Pure place reminders (no time)      -> one fix every [PLACE_ONLY_POLL_MS].
 *  - Time-open place reminders           -> one fix every [POLL_INTERVAL_MS].
 *
 * The monitor notification is dismissible and only posted once per monitoring
 * session; tapping it opens the app.
 */
class LocationMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var loopJob: Job? = null
    private var locationClient: AMapLocationClient? = null
    private var lastNotifText: String? = null
    private val notifManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("正在为你监测地点提醒"))
        lastNotifText = "正在为你监测地点提醒"
        startLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wake-up: (re)evaluate so config edits apply at once.
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (currentCoroutineContext().isActive) {
                val plan = evaluate()
                if (plan == null) {
                    Log.d(TAG, "nothing to monitor, stopping service")
                    MonitorWake.cancel(this@LocationMonitorService)
                    quit()
                    return@launch
                }
                if (plan.needsLocationNow) {
                    MonitorWake.cancel(this@LocationMonitorService)
                    val fix = requestOnceLocation()
                    if (fix != null) processLocation(fix.first, fix.second)
                    delay(if (plan.pollFast) PLACE_ONLY_POLL_MS else POLL_INTERVAL_MS)
                } else {
                    Log.d(TAG, "standby until ${plan.nextWakeAt}, stopping service")
                    MonitorWake.schedule(this@LocationMonitorService, plan.nextWakeAt)
                    quit()
                    return@launch
                }
            }
        }
    }

    private fun quit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /** Decide what this service must do right now; null = nothing to monitor. */
    private suspend fun evaluate(): Plan? {
        return try {
            val db = DatabaseProvider.get(this)
            val reminders = db.reminderDao().getEnabledWithPlace()
            if (reminders.isEmpty()) return null
            val now = System.currentTimeMillis()
            var needsNow = false
            var anyTimeless = false
            var nextWake = Long.MAX_VALUE
            for (r in reminders) {
                if (r.placeId == null || r.placeTrigger == null) continue
                val timeless =
                    r.triggerAt == null && r.windowStart == null && r.windowEnd == null
                if (isInTimeWindow(r, now)) {
                    needsNow = true
                    if (timeless) anyTimeless = true
                } else if (!timeless) {
                    val wake = nextGate(r, now)
                    if (wake != null && wake < nextWake) nextWake = wake
                }
            }
            if (!needsNow && nextWake == Long.MAX_VALUE) return null // all expired
            Plan(
                needsLocationNow = needsNow,
                nextWakeAt = if (nextWake == Long.MAX_VALUE) now + IDLE_RECHECK_MS else nextWake,
                pollFast = anyTimeless
            )
        } catch (e: Exception) {
            Log.e(TAG, "evaluate failed", e)
            Plan(needsLocationNow = true, nextWakeAt = System.currentTimeMillis(), pollFast = false)
        }
    }

    private data class Plan(
        val needsLocationNow: Boolean,
        val nextWakeAt: Long,
        val pollFast: Boolean
    )

    /**
     * True when [now] satisfies the reminder's time constraints.
     * ONCE reminders use absolute bounds; repeating ones use time-of-day +
     * weekday mask (a daily window, wrapping when start > end).
     */
    private fun isInTimeWindow(r: Reminder, now: Long): Boolean {
        if (r.repeatMode == RepeatMode.ONCE) {
            if (r.triggerAt != null && now < r.triggerAt) return false
            if (r.windowStart != null && now < r.windowStart) return false
            if (r.windowEnd != null && now > r.windowEnd) return false
            return true
        }
        val mask = maskOf(r)
        if (!dayMatches(now, mask)) return false
        val m = minutesOfDay(now)
        val s = r.windowStart?.let { minutesOfDay(it) }
            ?: r.triggerAt?.let { minutesOfDay(it) }
        val e = r.windowEnd?.let { minutesOfDay(it) }
        return when {
            s != null && e != null -> if (s <= e) m in s..e else m >= s || m <= e
            s != null -> m >= s
            e != null -> m <= e
            else -> true
        }
    }

    /** Next epoch-millis at which this reminder's time gate opens (repeating-aware). */
    private fun nextGate(r: Reminder, now: Long): Long? {
        if (r.repeatMode == RepeatMode.ONCE) {
            return listOfNotNull(
                r.triggerAt?.takeIf { it > now },
                r.windowStart?.takeIf { it > now }
            ).minOrNull()
        }
        val mask = maskOf(r)
        // Daily window: anchor at the opening minute-of-day. When only an end
        // bound exists the window opens at midnight of the next matching day.
        val anchor = r.windowStart ?: r.triggerAt
        return if (anchor != null) {
            TimeUtils.nextRepeating(anchor, mask, now)
        } else {
            r.windowEnd?.let { TimeUtils.nextRepeating(zeroTimeOf(it), mask, now) }
        }
    }

    private fun maskOf(r: Reminder): Int = when (r.repeatMode) {
        RepeatMode.DAILY -> TimeUtils.EVERYDAY_MASK
        RepeatMode.WEEKDAYS -> TimeUtils.WEEKDAYS_MASK
        RepeatMode.WEEKLY -> r.repeatDaysMask.takeIf { it != 0 } ?: TimeUtils.EVERYDAY_MASK
        RepeatMode.ONCE -> 0
    }

    private fun dayMatches(millis: Long, mask: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return mask and (1 shl cal.get(Calendar.DAY_OF_WEEK)) != 0
    }

    private fun minutesOfDay(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun zeroTimeOf(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** One single-shot location fix, or null on timeout / permission loss. */
    private suspend fun requestOnceLocation(): Pair<Double, Double>? {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            suspendCancellableCoroutine { cont ->
                AMapLocationClient.updatePrivacyShow(this, true, true)
                AMapLocationClient.updatePrivacyAgree(this, true)
                val client = locationClient ?: AMapLocationClient(this).also {
                    locationClient = it
                }
                client.setLocationOption(AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isOnceLocationLatest = true
                    isNeedAddress = false
                    isMockEnable = false
                    httpTimeOut = 20_000
                    isLocationCacheEnable = false
                })
                client.setLocationListener { loc ->
                    client.setLocationListener(null)
                    if (!cont.isActive) return@setLocationListener
                    if (loc != null && loc.errorCode == 0) {
                        cont.resume(Pair(loc.latitude, loc.longitude))
                    } else {
                        cont.resume(null)
                    }
                }
                client.startLocation()
                cont.invokeOnCancellation {
                    runCatching { client.stopLocation() }
                }
            }.also { stopLocationClient() }
        } catch (e: Exception) {
            Log.e(TAG, "location request failed", e)
            null
        }
    }

    private fun stopLocationClient() {
        runCatching { locationClient?.stopLocation() }
        runCatching { locationClient?.onDestroy() }
        locationClient = null
    }

    private suspend fun processLocation(lat: Double, lng: Double) {
        try {
            val db = DatabaseProvider.get(this)
            val reminderDao = db.reminderDao()
            val placeDao = db.placeDao()
            val reminders = reminderDao.getEnabledWithPlace()
            val now = System.currentTimeMillis()
            for (reminder in reminders) {
                val place = reminder.placeId?.let { placeDao.getById(it) } ?: continue
                val trigger = reminder.placeTrigger ?: continue
                val inside = GeoUtils.isInside(lat, lng, place)
                val lastInside = reminder.lastInside
                val inTimeWindow = isInTimeWindow(reminder, now)

                val shouldFire = when (trigger) {
                    PlaceTrigger.ARRIVE -> inTimeWindow && lastInside == false && inside
                    PlaceTrigger.LEAVE -> inTimeWindow && lastInside == true && !inside
                    PlaceTrigger.INSIDE -> inside && inTimeWindow && !alreadyFiredRecently(reminder, now)
                    PlaceTrigger.NOT_INSIDE -> !inside && inTimeWindow && !alreadyFiredRecently(reminder, now)
                }

                if (shouldFire) {
                    Log.d(TAG, "firing reminder ${reminder.id} ($trigger) at $now")
                    ReminderNotifier.notify(this, reminder)
                    reminderDao.markFired(reminder.id, now)
                }
                if (lastInside != inside) {
                    reminderDao.updateInsideState(reminder.id, inside)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "processLocation failed", e)
        }
    }

    /**
     * Avoid refiring: a continuous (INSIDE / NOT_INSIDE) reminder fires at most once
     * per hour while the condition holds.
     */
    private fun alreadyFiredRecently(reminder: Reminder, now: Long): Boolean {
        val reArm = 60 * 60 * 1000L // 1 hour
        return reminder.lastFiredAt > 0 && (now - reminder.lastFiredAt) < reArm
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, NOTIF_ID,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotifyHelper.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_memoryball)
            .setLargeIcon(ReminderNotifier.largeIcon(this))
            .setContentTitle("记忆球位置监测")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
    }

    /** Post only when the text actually changes, so a user-swipe is never undone. */
    private fun updateNotification(text: String) {
        if (text == lastNotifText) return
        lastNotifText = text
        notifManager.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        stopLocationClient()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationMonitor"
        private const val NOTIF_ID = 2001
        private const val POLL_INTERVAL_MS = 60_000L
        private const val PLACE_ONLY_POLL_MS = 30_000L
        private const val IDLE_RECHECK_MS = 10 * 60_000L

        fun startIfNeeded(context: Context) {
            val intent = Intent(context, LocationMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            MonitorWake.cancel(context)
            context.stopService(Intent(context, LocationMonitorService::class.java))
        }
    }
}

/** Schedules/claims the standby wake-up alarm for the monitor service. */
private object MonitorWake {
    private const val REQ = 999000

    fun schedule(context: Context, at: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQ,
            Intent(context, LocationWakeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/** Receives the standby wake-up and restarts the monitor service to poll. */
class LocationWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("LocationMonitor", "wake alarm received")
        LocationMonitorService.startIfNeeded(context)
    }
}
