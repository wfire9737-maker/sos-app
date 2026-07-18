package com.example.model

import java.util.UUID

data class EmergencyHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val time: String,
    val durationSeconds: Long,
    val responseTimeSeconds: Long,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String, // "CRITICAL", "HIGH", "WARNING"
    val deviceUsed: String,
    val contactsNotified: List<String>,
    val aiScore: Int,
    val triggerType: String, // "FALL_DETECTED", "MANUAL_BUTTON"
    val resolutionNotes: String = "Resolved completely by first responder crew.",
    val resolvedBy: String = "Operator Marcus"
)
