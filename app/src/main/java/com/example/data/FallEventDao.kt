package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.FallEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface FallEventDao {
    @Query("SELECT * FROM fall_events ORDER BY timestampMs DESC")
    fun getAllEvents(): Flow<List<FallEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FallEvent)

    @Query("DELETE FROM fall_events")
    suspend fun clearAllEvents()
}
