package com.memoryball.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryball.app.data.model.Place
import com.memoryball.app.data.model.Reminder
import com.memoryball.app.data.repo.DatabaseProvider
import com.memoryball.app.data.repo.ReminderRepository
import com.memoryball.app.engine.AlarmScheduler
import com.memoryball.app.engine.LocationMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReminderRepository(app)

    val reminders: StateFlow<List<Reminder>> =
        repo.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val places: StateFlow<List<Place>> =
        repo.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            repo.setEnabled(reminder, enabled)
            val updated = reminder.copy(enabled = enabled)
            if (enabled) {
                AlarmScheduler.schedule(getApplication(), updated)
            } else {
                AlarmScheduler.cancel(getApplication(), reminder.id)
            }
            if (reminder.hasPlaceCondition) {
                LocationMonitorService.startIfNeeded(getApplication())
            }
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), reminder.id)
            repo.delete(reminder)
            if (reminder.hasPlaceCondition) {
                // Wake the monitor so it re-evaluates (and can stop itself) immediately.
                LocationMonitorService.startIfNeeded(getApplication())
            }
        }
    }
}
