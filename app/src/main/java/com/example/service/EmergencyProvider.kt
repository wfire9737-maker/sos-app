package com.example.service

import android.content.Context
import com.example.model.EmergencyModel
import kotlinx.coroutines.flow.StateFlow

class EmergencyProvider(
    private val context: Context,
    val emergencyService: EmergencyService
) {
    val activeEmergencyState: StateFlow<EmergencyModel?> = emergencyService.activeEmergency

    fun isEmergencyInProgress(): Boolean {
        return emergencyService.isEmergencyActive()
    }

    suspend fun initiateEmergency(
        userId: String,
        userName: String,
        userPhone: String,
        triggerSource: String,
        deviceId: String = "ESP32-SOS-BAND-81F4",
        lat: Double? = null,
        lng: Double? = null,
        accuracy: Float? = null,
        altitude: Double? = null,
        speed: Float? = null,
        bearing: Float? = null
    ): EmergencyModel {
        return emergencyService.startEmergency(
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            triggerType = triggerSource,
            deviceId = deviceId,
            customLat = lat,
            customLng = lng,
            customAccuracy = accuracy,
            customAltitude = altitude,
            customSpeed = speed,
            customBearing = bearing
        )
    }

    suspend fun cancelEmergency(pin: String, expectedPin: String, reason: String): Boolean {
        return emergencyService.cancelEmergencyWithPin(pin, expectedPin, reason)
    }

    fun markEmergencySafe() {
        emergencyService.markSafeAndClose()
    }
}
