package com.example.ble

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real NEO-6M GPS location received from ESP32 via BLE characteristic (beb54805-36e1-4688-b7f5-ea07361b26a8).
 */
data class HardwareGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val receivedAt: Long = System.currentTimeMillis(),
    val rawPayload: String = "",
    val locationSource: String = "ESP32_NEO6M"
) {
    /**
     * Checks if this GPS coordinate fix was received within the given freshness threshold (default 2 minutes).
     */
    fun isFresh(maxAgeMs: Long = 120_000): Boolean = (System.currentTimeMillis() - receivedAt) <= maxAgeMs

    /**
     * Age in seconds since the packet was received from ESP32.
     */
    fun getAgeInSeconds(): Long = ((System.currentTimeMillis() - receivedAt) / 1000L).coerceAtLeast(0L)

    /**
     * Human-readable freshness description.
     */
    fun getFreshnessDescription(): String {
        val ageSec = getAgeInSeconds()
        return when {
            ageSec < 15 -> "Live Fix ($ageSec s ago)"
            ageSec < 60 -> "Fresh Fix ($ageSec s ago)"
            ageSec < 300 -> "Recent Fix (${ageSec / 60}m ago)"
            else -> "Last Known (${ageSec / 60}m ago)"
        }
    }

    fun formattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(receivedAt))
    }
}

/**
 * State of the hardware NEO-6M GPS module on the ESP32.
 */
sealed class HardwareGpsState {
    data object Unavailable : HardwareGpsState()
    data object WaitingForFix : HardwareGpsState()
    data class ValidLocation(val location: HardwareGpsLocation) : HardwareGpsState()
    data class Error(val message: String) : HardwareGpsState()
}
