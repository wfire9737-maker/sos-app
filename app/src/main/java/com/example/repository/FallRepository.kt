package com.example.repository

import android.util.Log
import com.example.data.FallEventDao
import com.example.model.FallEvent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class FallRepository(
    private val fallEventDao: FallEventDao,
    private val firestore: FirebaseFirestore? = null
) {
    val allEvents: Flow<List<FallEvent>> = fallEventDao.getAllEvents()

    suspend fun insertEvent(event: FallEvent) {
        fallEventDao.insertEvent(event)
        
        val fs = firestore ?: return
        try {
            val eventMap = mapOf(
                "id" to event.id,
                "timestampMs" to event.timestampMs,
                "eventType" to event.eventType,
                "sensorReadingDetails" to event.sensorReadingDetails
            )
            fs.collection("fall_events").document("fall-${event.timestampMs}").set(eventMap)
        } catch (e: Exception) {
            Log.e("FallRepository", "Failed to sync fall event to Firestore: ${e.message}")
        }
    }

    suspend fun clearAllEvents() {
        fallEventDao.clearAllEvents()
    }
}

