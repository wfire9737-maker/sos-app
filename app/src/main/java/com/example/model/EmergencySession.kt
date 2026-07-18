package com.example.model

data class EmergencySession(
    val activeAlert: Alert? = null,
    val deviceId: String = "ESP32-SOS-BAND-81F4",
    val batteryLevel: Int = 86,
    val gpsStatus: String = "HIGH ACCURACY (±2.5m)",
    val internetStatus: String = "ESP-WIFI-CELLULAR-BRIDGE (CONNECTED)",
    val emergencyLevel: String = "CRITICAL (LEVEL 3)",
    val aiConfidence: Int = 94,
    val startTimeMs: Long = 0L,
    val responderStatus: String = "DISPATCHING HELPLINE", // DISPATCHING, ACKNOWLEDGED, EN_ROUTE, RESOLVED
    val isMuted: Boolean = false,
    val isAcknowledged: Boolean = false,
    val isMarkedSafe: Boolean = false
) {
    val isActive: Boolean get() = activeAlert != null && !isMarkedSafe
}
