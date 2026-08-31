package com.memoryball.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the place condition is evaluated. */
enum class PlaceTrigger { ARRIVE, LEAVE, INSIDE, NOT_INSIDE }

/** Repeat pattern for a reminder. */
enum class RepeatMode { ONCE, DAILY, WEEKDAYS, WEEKLY }

/**
 * A reminder.
 *
 * Time condition (all epoch-millis, nullable):
 *  - [triggerAt]     exact one-shot time; also the anchor for repeating alarms.
 *  - [windowStart]   only remind at/after this time (nullable = no lower bound).
 *  - [windowEnd]     only remind at/before this time (nullable = no upper bound).
 *
 * Place condition (nullable = no place constraint):
 *  - [placeId]       references Place.
 *  - [placeTrigger]  how the place constrains firing.
 *
 * Behaviour:
 *  - Time-only  -> AlarmManager fires at triggerAt.
 *  - Place-only -> location monitor evaluates ARRIVE/LEAVE transitions.
 *  - Combined   -> evaluated continuously: while within the time window AND the
 *                  INSIDE / NOT_INSIDE place condition holds, the reminder fires.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val enabled: Boolean = true,

    // time condition
    val triggerAt: Long? = null,
    val windowStart: Long? = null,
    val windowEnd: Long? = null,

    // repeat
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    /** Bitmask of days (Calendar.DAY_OF_WEEK bits) for WEEKLY / WEEKDAYS. */
    val repeatDaysMask: Int = 0,

    // place condition
    val placeId: Long? = null,
    val placeTrigger: PlaceTrigger? = null,

    // alarm style: true = full alarm (ring + full screen), false = plain notification
    val alarmStyle: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    /** Set when an ARRIVE/LEAVE one-shot has already fired, to avoid repeats. */
    val lastFiredAt: Long = 0,
    /** Cached inside/outside state for transition detection. */
    val lastInside: Boolean? = null
) {
    val hasTimeCondition: Boolean get() = triggerAt != null || windowStart != null || windowEnd != null
    val hasPlaceCondition: Boolean get() = placeId != null && placeTrigger != null
}
