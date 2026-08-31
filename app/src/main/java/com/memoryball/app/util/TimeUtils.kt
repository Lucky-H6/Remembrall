package com.memoryball.app.util

import java.util.Calendar

/** Compute the next epoch-millis at/after [fromMillis] matching [repeatDaysMask] at hour:minute. */
object TimeUtils {

    const val DAY_MON = 1 shl Calendar.MONDAY
    const val DAY_TUE = 1 shl Calendar.TUESDAY
    const val DAY_WED = 1 shl Calendar.WEDNESDAY
    const val DAY_THU = 1 shl Calendar.THURSDAY
    const val DAY_FRI = 1 shl Calendar.FRIDAY
    const val DAY_SAT = 1 shl Calendar.SATURDAY
    const val DAY_SUN = 1 shl Calendar.SUNDAY
    const val WEEKDAYS_MASK = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI
    const val EVERYDAY_MASK = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI or DAY_SAT or DAY_SUN

    /** Alias used by edit UI: bit for [Calendar.SUNDAY]. */
    const val DAY_SUNDAY = DAY_SUN

    private val dateTimeFmt = java.text.SimpleDateFormat("MM月dd日 HH:mm", java.util.Locale.CHINA)

    /** Format [millis] as "MM月dd日 HH:mm". */
    fun formatDateTime(millis: Long): String =
        dateTimeFmt.format(java.util.Date(millis))

    fun dayLabel(mask: Int): String {
        if (mask == 0) return "仅一次"
        if (mask == EVERYDAY_MASK) return "每天"
        if (mask == WEEKDAYS_MASK) return "工作日"
        val names = linkedMapOf(
            Calendar.MONDAY to "一", Calendar.TUESDAY to "二", Calendar.WEDNESDAY to "三",
            Calendar.THURSDAY to "四", Calendar.FRIDAY to "五", Calendar.SATURDAY to "六",
            Calendar.SUNDAY to "日"
        )
        val parts = names.filterKeys { mask and (1 shl it) != 0 }.values
        return "周" + parts.joinToString("、")
    }

    /** Next trigger for a repeating reminder anchored at [anchorMillis], matching [mask]. */
    fun nextRepeating(anchorMillis: Long, mask: Int, fromMillis: Long): Long {
        if (mask == 0) return anchorMillis
        val anchor = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        val hour = anchor.get(Calendar.HOUR_OF_DAY)
        val minute = anchor.get(Calendar.MINUTE)
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (cal.timeInMillis <= fromMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        // advance until a matching day
        var guard = 0
        while (mask and (1 shl cal.get(Calendar.DAY_OF_WEEK)) == 0 && guard < 8) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        return cal.timeInMillis
    }
}
