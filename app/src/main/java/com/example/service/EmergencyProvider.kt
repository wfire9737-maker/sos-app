package com.example.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import com.example.model.EmergencyModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class EmergencyProvider(
    private val context: Context,
    val emergencyService: EmergencyService,
    private val authService: AuthService,
    private val locationService: LocationService,
    private val aiService: AIService,
    private val alarmVibratorService: AlarmVibratorService,
    private val deviceService: DeviceService,
    private val voiceSosService: VoiceSosService,
    private val settingsDataStore: com.example.data.SettingsDataStore
) {
    val activeEmergencyState: StateFlow<EmergencyModel?> = emergencyService.activeEmergency
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            combine(
                activeEmergencyState,
                settingsDataStore.sosSoundEnabledFlow,
                settingsDataStore.sosVibrationEnabledFlow
            ) { activeEmergency, soundEnabled, vibrationEnabled ->
                Triple(activeEmergency != null, soundEnabled, vibrationEnabled)
            }
            .distinctUntilChanged()
            .collect { (isActive, soundEnabled, vibrationEnabled) ->
                if (isActive) {
                    if (soundEnabled) alarmVibratorService.startAlarm() else alarmVibratorService.stopAlarm()
                    if (vibrationEnabled) alarmVibratorService.startVibration() else alarmVibratorService.stopVibration()
                } else {
                    alarmVibratorService.cleanUp()
                }
            }
        }

        scope.launch {
            deviceService.bleManager.sosEvents.collect { sosEvent ->
                android.util.Log.d("BleManager", "EMERGENCY: activating from PHYSICAL_BLE_BUTTON (Event #${sosEvent.eventId})")
                val hwGps = sosEvent.hardwareGpsLocation ?: deviceService.bleManager.latestHardwareGpsLocation.value
                val isGpsValid = deviceService.bleManager.hardwareGpsState.value is com.example.ble.HardwareGpsState.ValidLocation && hwGps != null

                if (isGpsValid && hwGps != null) {
                    android.util.Log.d("BleManager", "SOS: using latest NEO-6M location")
                    triggerEmergency(
                        triggerSource = "PHYSICAL_BLE_BUTTON",
                        deviceId = "ESP32-SOS-BAND-81F4",
                        lat = hwGps.latitude,
                        lng = hwGps.longitude,
                        accuracy = 3.0f,
                        locationSource = "ESP32_NEO6M"
                    )
                } else {
                    android.util.Log.d("BleManager", "SOS: NEO-6M location unavailable")
                    triggerEmergency(
                        triggerSource = "PHYSICAL_BLE_BUTTON",
                        deviceId = "ESP32-SOS-BAND-81F4",
                        lat = null,
                        lng = null,
                        accuracy = null,
                        locationSource = "ESP32_NEO6M_UNAVAILABLE"
                    )
                }
            }
        }
        scope.launch {
            deviceService.incomingEsp32SosEvent.collect { triggerType ->
                if (triggerType != null && !triggerType.startsWith("PHYSICAL_BLE_BUTTON")) {
                    android.util.Log.d("Emergency", "EMERGENCY: activating from $triggerType")
                    triggerEmergency(triggerSource = triggerType, deviceId = "ESP32-SOS-BAND-81F4")
                    deviceService.clearIncomingEsp32SosEvent()
                }
            }
        }
        scope.launch {
            voiceSosService.lastRecognizedCommand.collect { command ->
                when (command) {
                    is VoiceCommand.Sos -> {
                        triggerEmergency(triggerSource = "VOICE_SOS", deviceId = "MOBILE-VOICE-RECOGNIZE")
                        voiceSosService.clearLastRecognizedCommand()
                    }
                    is VoiceCommand.CancelSos -> {
                        cancelEmergency("", "", "Voice SOS Cancelled")
                        voiceSosService.clearLastRecognizedCommand()
                    }
                    else -> {}
                }
            }
        }
    }


    
    fun triggerEmergency(
        triggerSource: String,
        deviceId: String = "MOBILE-APP-SOS",
        lat: Double? = null,
        lng: Double? = null,
        accuracy: Float? = null,
        altitude: Double? = null,
        speed: Float? = null,
        bearing: Float? = null,
        locationSource: String = "PHONE_GPS"
    ) {
        scope.launch {
            if (isEmergencyInProgress()) {
                emergencyService.activeEmergency.value?.let { model ->
                    emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                }
                return@launch
            }
            
            val user = (authService.authState.value as? com.example.service.AuthState.Success)?.user
            val userId = user?.uid ?: "user-101"
            val userName = user?.name ?: "Marcus Vance"
            val userPhone = user?.phone ?: "+1-555-0143"

            val model = emergencyService.startEmergency(
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                triggerType = triggerSource,
                deviceId = deviceId,
                customLat = lat ?: locationService.currentLocation.value.latitude,
                customLng = lng ?: locationService.currentLocation.value.longitude,
                customAccuracy = accuracy ?: locationService.currentLocation.value.accuracy,
                customAltitude = altitude,
                customSpeed = speed,
                customBearing = bearing,
                locationSource = locationSource
            )

            // Trigger AI Emergency Analysis
            val analysis = com.example.model.AIAnalysisModel(
                alertId = model.emergencyId,
                confidenceScore = if (triggerSource == "FALL_DETECTED") 98 else 100,
                falseAlarmProbability = 2,
                motionAnalysis = "Automated analysis based on $triggerSource",
                activityRecognition = "SOS DETECTED",
                riskLevel = "CRITICAL",
                suggestedAction = "ALERT ALL PRIMARY FAMILY CONTACTS AND LAUNCH COUNTY DISPATCH CODES",
                timeline = listOf(
                    com.example.model.AITimelineEvent("Now", "Triggered", "SOS Triggered by $triggerSource", "🚨")
                )
            )
            aiService.addAnalysisLog(analysis)
        }
    }

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
