package com.remembrall.app.data.repo

import android.content.Context
import androidx.room.Room
import com.remembrall.app.data.db.AppDatabase
import com.remembrall.app.data.db.PlaceDao
import com.remembrall.app.data.db.ReminderDao

/** Simple singleton holder for the Room database. */
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "Remembrall.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }

    fun reminderDao(context: Context): ReminderDao = get(context).reminderDao()
    fun placeDao(context: Context): PlaceDao = get(context).placeDao()
}
