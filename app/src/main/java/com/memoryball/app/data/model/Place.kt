package com.memoryball.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-saved favourite place, e.g. "家", "公司".
 * A reminder is considered "at this place" when within [radiusMeters] (default 50 m).
 */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 50f,
    val createdAt: Long = System.currentTimeMillis()
)
