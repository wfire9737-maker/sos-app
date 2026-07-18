package com.example.model

import java.util.UUID

data class HistoryModel(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val time: String,
    val durationSeconds: Long,
    val responseTimeSeconds: Long,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val aiConfidence: Int,
    val severity: String, // "CRITICAL", "HIGH", "WARNING"
    val contactsNotified: List<String>,
    val deviceUsed: String = "ESP32-SOS-BAND-81F4",
    val triggerType: String = "MANUAL_BUTTON",
    val resolutionNotes: String = "Resolved completely by first responder crew.",
    val resolvedBy: String = "Operator Marcus"
) {
    // Read-only properties for seamless integration & backward-compatibility
    val duration: Long get() = durationSeconds
    val responseTime: Long get() = responseTimeSeconds
    val locationName: String get() = address
    val aiScore: Int get() = aiConfidence
}
