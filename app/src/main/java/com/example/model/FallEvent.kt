package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fall_events")
data class FallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val eventType: String, // "NORMAL_WALKING", "RUNNING", "SITTING", "STANDING", "SUDDEN_FALL_DETECTED", "FALL_CANCELLED", "FALL_SOS_AUTO_TRIGGER"
    val sensorReadingDetails: String
)
