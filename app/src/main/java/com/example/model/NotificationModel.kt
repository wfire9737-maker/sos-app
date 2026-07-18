package com.example.model

import java.util.UUID

enum class NotificationCategory {
    EMERGENCY_ALERTS,
    BATTERY_ALERTS,
    DEVICE_DISCONNECTED,
    GPS_UNAVAILABLE,
    EMERGENCY_RESOLVED,
    SAFE_ARRIVAL,
    FIRMWARE_UPDATES
}

data class NotificationModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: NotificationCategory,
    val isRead: Boolean = false,
    val deviceId: String? = null,
    val severity: String = "INFO" // "CRITICAL", "WARNING", "INFO"
) {
    // Read-only properties for backward/forward compatibility or mapping helper
    val isEmergency: Boolean get() = category == NotificationCategory.EMERGENCY_ALERTS
}
