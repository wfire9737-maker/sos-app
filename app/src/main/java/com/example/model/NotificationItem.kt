package com.example.model

import java.util.UUID

enum class NotificationType {
    EMERGENCY,
    BATTERY_LOW,
    DEVICE_OFFLINE,
    GPS_UNAVAILABLE,
    FIRMWARE_UPDATE,
    SAFE_ARRIVAL,
    EMERGENCY_CANCELLED
}

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType,
    val isRead: Boolean = false,
    val deviceId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
