package com.remembrall.app.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remembrall.app.data.model.Place
import com.remembrall.app.data.model.PlaceTrigger
import com.remembrall.app.data.model.Reminder
import com.remembrall.app.data.model.RepeatMode
import com.remembrall.app.data.repo.DatabaseProvider
import com.remembrall.app.data.repo.ReminderRepository
import com.remembrall.app.engine.AlarmScheduler
import com.remembrall.app.engine.LocationMonitorService
import com.remembrall.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeMode { NONE, EXACT, WINDOW }

data class EditState(
    val title: String = "",
    val note: String = "",
    val alarmStyle: Boolean = true,
    val timeMode: TimeMode = TimeMode.EXACT,
    val triggerAt: Long? = null,
    val windowStart: Long? = null,
    val windowEnd: Long? = null,
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val repeatDaysMask: Int = 0,
    val hasPlace: Boolean = false,
    val placeId: Long? = null,
    val placeTrigger: PlaceTrigger? = null,
    val saved: Boolean = false,
    val error: String? = null
)

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReminderRepository(app)
    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    val places: StateFlow<List<Place>> =
        repo.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var editingId: Long = -1L
    private var originalCreatedAt: Long = 0L

    fun load(id: Long) {
        if (id == -1L) return
        editingId = id
        viewModelScope.launch {
            val r = repo.getById(id) ?: return@launch
            originalCreatedAt = r.createdAt
            _state.value = EditState(
                title = r.title,
                note = r.note,
                alarmStyle = r.alarmStyle,
                timeMode = when {
                    r.triggerAt != null -> TimeMode.EXACT
                    r.windowStart != null || r.windowEnd != null -> TimeMode.WINDOW
                    else -> TimeMode.NONE
                },
                triggerAt = r.triggerAt,
                windowStart = r.windowStart,
                windowEnd = r.windowEnd,
                repeatMode = r.repeatMode,
                repeatDaysMask = r.repeatDaysMask,
                hasPlace = r.placeId != null,
                placeId = r.placeId,
                placeTrigger = r.placeTrigger
            )
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setAlarmStyle(v: Boolean) = _state.update { it.copy(alarmStyle = v) }
    fun setTimeMode(v: TimeMode) = _state.update { it.copy(timeMode = v) }
    fun setTriggerAt(v: Long) = _state.update { it.copy(triggerAt = v) }
    fun setRepeatMode(v: RepeatMode) = _state.update { it.copy(repeatMode = v) }

    // ---- date / time part setters (separate pickers) ----

    fun setTriggerDate(v: Long) =
        _state.update { s ->
            val (h, m) = defaultTimeFor(v)
            s.copy(triggerAt = keepTimeOf(s.triggerAt, v, h, m))
        }
    fun setTriggerTime(hour: Int, minute: Int) =
        _state.update { it.copy(triggerAt = keepDateOf(it.triggerAt, hour, minute)) }

    fun setWindowStartDate(v: Long) =
        _state.update { s ->
            val (h, m) = defaultTimeFor(v)
            s.windowStart?.let { w -> s.copy(windowStart = keepTimeOf(w, v, h, m)) }
                ?: s.copy(windowStart = atMidnightPlus(v, h, m))
        }
    fun setWindowStartHourMinute(hour: Int, minute: Int) =
        _state.update { it.copy(windowStart = keepDateOf(it.windowStart, hour, minute)) }

    fun setWindowEndDate(v: Long) =
        _state.update { s ->
            val (h, m) = defaultTimeFor(v, 18)
            s.windowEnd?.let { w -> s.copy(windowEnd = keepTimeOf(w, v, h, m)) }
                ?: s.copy(windowEnd = atMidnightPlus(v, h, m))
        }
    fun setWindowEndHourMinute(hour: Int, minute: Int) =
        _state.update { it.copy(windowEnd = keepDateOf(it.windowEnd, hour, minute)) }

    /** Default time for a freshly picked date: today -> now, otherwise [nonTodayHour]:00. */
    private fun defaultTimeFor(dateMillis: Long, nonTodayHour: Int = 9): Pair<Int, Int> {
        val sel = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val now = Calendar.getInstance()
        val isToday = sel.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            sel.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        return if (isToday) {
            now.get(Calendar.HOUR_OF_DAY) to now.get(Calendar.MINUTE)
        } else {
            nonTodayHour to 0
        }
    }

    /** Result millis = calendar date of [dateMillis] + time of [current] (fallback h:m). */
    private fun keepTimeOf(current: Long?, dateMillis: Long, fallbackHour: Int, fallbackMinute: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        if (current != null) {
            val src = Calendar.getInstance().apply { timeInMillis = current }
            cal.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, src.get(Calendar.MINUTE))
        } else {
            cal.set(Calendar.HOUR_OF_DAY, fallbackHour)
            cal.set(Calendar.MINUTE, fallbackMinute)
        }
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Result millis = calendar date of [current] (fallback: today) + given time. */
    private fun keepDateOf(current: Long?, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            if (current != null) timeInMillis = current
        }
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun atMidnightPlus(dateMillis: Long, hour: Int, minute: Int): Long =
        keepTimeOf(null, dateMillis, hour, minute)

    /** Repeating reminders carry no date meaning: re-anchor millis to today. */
    private fun anchorToToday(millis: Long?): Long? {
        if (millis == null) return null
        val src = Calendar.getInstance().apply { timeInMillis = millis }
        return keepDateOf(System.currentTimeMillis(), src.get(Calendar.HOUR_OF_DAY), src.get(Calendar.MINUTE))
    }

    fun toggleDay(calendarDay: Int) {
        val bit = TimeUtils.DAY_SUNDAY shl (calendarDay - java.util.Calendar.SUNDAY)
        _state.update { it.copy(repeatDaysMask = it.repeatDaysMask xor bit) }
    }

    fun setHasPlace(v: Boolean) = _state.update {
        it.copy(
            hasPlace = v,
            placeTrigger = if (v) (it.placeTrigger ?: PlaceTrigger.ARRIVE) else null
        )
    }

    fun setPlace(id: Long) = _state.update { it.copy(placeId = id) }
    fun setPlaceTrigger(t: PlaceTrigger) = _state.update { it.copy(placeTrigger = t) }

    // Window pickers open a dialog from the UI layer.
    fun pickWindowStart() { /* handled via UI dialog calling setWindowStart */ }
    fun pickWindowEnd() { /* handled via UI dialog calling setWindowEnd */ }
    fun setWindowStart(v: Long?) = _state.update { it.copy(windowStart = v) }
    fun setWindowEnd(v: Long?) = _state.update { it.copy(windowEnd = v) }

    fun save() {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "请填写提醒内容") }
            return
        }
        if (s.hasPlace && (s.placeId == null || s.placeTrigger == null)) {
            _state.update { it.copy(error = "请选择地点和触发方式") }
            return
        }
        if (s.timeMode == TimeMode.EXACT && s.triggerAt == null) {
            _state.update { it.copy(error = "请选择提醒时间") }
            return
        }
        if (s.timeMode == TimeMode.WINDOW && s.windowStart == null && s.windowEnd == null) {
            _state.update { it.copy(error = "请选择开始或结束时间") }
            return
        }

        val now = System.currentTimeMillis()
        // Repeating reminders: strip date meaning, anchor on today.
        val repeating = s.repeatMode != RepeatMode.ONCE
        val rawTrigger = if (s.timeMode == TimeMode.EXACT) s.triggerAt else null
        val rawStart = if (s.timeMode == TimeMode.WINDOW) s.windowStart else null
        val rawEnd = if (s.timeMode == TimeMode.WINDOW) s.windowEnd else null
        val triggerAt = if (repeating) anchorToToday(rawTrigger) else rawTrigger
        val windowStart = if (repeating) anchorToToday(rawStart) else rawStart
        val windowEnd = if (repeating) anchorToToday(rawEnd) else rawEnd

        if (s.timeMode == TimeMode.EXACT && !repeating && triggerAt != null && triggerAt <= now) {
            _state.update { it.copy(error = "提醒时间已过，请选择未来的时间") }
            return
        }
        val placeId = if (s.hasPlace) s.placeId else null
        val placeTrigger = if (s.hasPlace) s.placeTrigger else null

        val reminder = Reminder(
            id = if (editingId == -1L) 0 else editingId,
            title = s.title.trim(),
            note = s.note.trim(),
            enabled = true,
            triggerAt = triggerAt,
            windowStart = windowStart,
            windowEnd = windowEnd,
            repeatMode = s.repeatMode,
            repeatDaysMask = s.repeatDaysMask,
            placeId = placeId,
            placeTrigger = placeTrigger,
            alarmStyle = s.alarmStyle,
            createdAt = if (editingId == -1L) System.currentTimeMillis() else originalCreatedAt
        )

        viewModelScope.launch {
            val id = repo.save(reminder)
            val saved = reminder.copy(id = if (editingId == -1L) id else editingId)
            // Schedule time alarm if applicable
            if (saved.hasTimeCondition && saved.triggerAt != null) {
                AlarmScheduler.schedule(getApplication(), saved)
            } else {
                AlarmScheduler.cancel(getApplication(), saved.id)
            }
            // Ensure the location monitor runs if any reminder has a place condition.
            if (saved.hasPlaceCondition) {
                LocationMonitorService.startIfNeeded(getApplication())
            }
            _state.update { it.copy(saved = true, error = null) }
        }
    }
}
