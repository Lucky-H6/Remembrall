package com.remembrall.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.remembrall.app.data.model.Place
import com.remembrall.app.data.model.PlaceTrigger
import com.remembrall.app.data.model.Reminder
import com.remembrall.app.data.model.RepeatMode

class Converters {
    @TypeConverter
    fun repeatModeToString(v: RepeatMode): String = v.name

    @TypeConverter
    fun stringToRepeatMode(v: String): RepeatMode = RepeatMode.valueOf(v)

    @TypeConverter
    fun placeTriggerToString(v: PlaceTrigger?): String? = v?.name

    @TypeConverter
    fun stringToPlaceTrigger(v: String?): PlaceTrigger? = v?.let { PlaceTrigger.valueOf(it) }
}

@Database(entities = [Reminder::class, Place::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun placeDao(): PlaceDao
}
