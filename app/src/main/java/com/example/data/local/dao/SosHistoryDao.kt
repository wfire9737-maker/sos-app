package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SosHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SosHistoryDao {
    @Query("SELECT * FROM sos_history WHERE uid = :uid ORDER BY date DESC")
    fun getHistoryForUser(uid: String): Flow<List<SosHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SosHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<SosHistoryEntity>)

    @Query("DELETE FROM sos_history WHERE historyId = :historyId")
    suspend fun deleteHistory(historyId: String)
    
    @Query("DELETE FROM sos_history")
    suspend fun clearAll()
}
