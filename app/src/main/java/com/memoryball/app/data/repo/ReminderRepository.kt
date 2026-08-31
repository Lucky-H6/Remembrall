package com.memoryball.app.data.repo

import android.content.Context
import com.memoryball.app.data.model.Place
import com.memoryball.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val context: Context) {
    private val reminderDao = DatabaseProvider.reminderDao(context)
    private val placeDao = DatabaseProvider.placeDao(context)

    val reminders: Flow<List<Reminder>> = reminderDao.observeAll()
    val places: Flow<List<Place>> = placeDao.observeAll()

    suspend fun getReminder(id: Long): Reminder? = reminderDao.getById(id)
    suspend fun getPlace(id: Long): Place? = placeDao.getById(id)

    // Aliases used by ViewModels
    suspend fun getById(id: Long): Reminder? = reminderDao.getById(id)
    suspend fun getPlaceById(id: Long): Place? = placeDao.getById(id)

    suspend fun saveReminder(reminder: Reminder): Long = reminderDao.upsert(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)
    suspend fun setReminderEnabled(id: Long, enabled: Boolean) = reminderDao.setEnabled(id, enabled)

    // Aliases used by ViewModels
    suspend fun save(reminder: Reminder): Long = reminderDao.upsert(reminder)
    suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
    suspend fun setEnabled(reminder: Reminder, enabled: Boolean) = reminderDao.setEnabled(reminder.id, enabled)

    suspend fun savePlace(place: Place): Long = placeDao.upsert(place)
    suspend fun deletePlace(place: Place) = placeDao.delete(place)
}
