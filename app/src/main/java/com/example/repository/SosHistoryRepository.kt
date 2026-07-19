package com.example.repository

import com.example.model.EmergencyHistoryItem
import kotlinx.coroutines.flow.Flow

interface SosHistoryRepository {
    fun getHistoryForUser(uid: String): Flow<List<EmergencyHistoryItem>>
    suspend fun addHistoryItem(item: EmergencyHistoryItem): Result<Unit>
    suspend fun syncHistoryWithRemote(uid: String)
}
