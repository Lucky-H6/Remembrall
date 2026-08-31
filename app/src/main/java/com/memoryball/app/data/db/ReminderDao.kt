package com.memoryball.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memoryball.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY COALESCE(triggerAt, windowStart, createdAt) ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND placeId IS NOT NULL")
    suspend fun getEnabledWithPlace(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("UPDATE reminders SET lastFiredAt = :firedAt WHERE id = :id")
    suspend fun markFired(id: Long, firedAt: Long)

    @Query("UPDATE reminders SET lastInside = :inside WHERE id = :id")
    suspend fun updateInsideState(id: Long, inside: Boolean?)

    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
