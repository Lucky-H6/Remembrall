package com.memoryball.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memoryball.app.data.model.Place
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Place>>

    @Query("SELECT * FROM places ORDER BY createdAt ASC")
    suspend fun getAll(): List<Place>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getById(id: Long): Place?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: Place): Long

    @Delete
    suspend fun delete(place: Place)
}
