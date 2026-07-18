package com.example.repository

import com.example.data.FallEventDao
import com.example.model.FallEvent
import kotlinx.coroutines.flow.Flow

class FallRepository(private val fallEventDao: FallEventDao) {
    val allEvents: Flow<List<FallEvent>> = fallEventDao.getAllEvents()

    suspend fun insertEvent(event: FallEvent) {
        fallEventDao.insertEvent(event)
    }

    suspend fun clearAllEvents() {
        fallEventDao.clearAllEvents()
    }
}
