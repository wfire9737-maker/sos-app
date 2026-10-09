package com.example.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import com.example.model.EmergencyModel
import com.example.model.TrustedPlace
import kotlinx.coroutines.flow.StateFlow

class EmergencyProvider(
    private val context: Context,
    val emergencyService: EmergencyService,
    private val authService: AuthService,
    private val locationService: LocationService,
    private val aiService: AIService,
    private val alarmVibratorService: AlarmVibratorService,
    private val deviceService: DeviceService,
    private val voiceSosService: VoiceSosService,
    private val trustedPlacesService: TrustedPlacesService
) {
    val activeEmergencyState: StateFlow<EmergencyModel?> = emergencyService.activeEmergency
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        emergencyService.onCallStateChanged = { isCallActive ->
            if (isCallActive) {
                android.util.Log.d("EmergencyProvider", "Emergency call ACTIVE -> stopping siren & pausing Voice SOS")
                alarmVibratorService.stopAlarm()
                voiceSosService.pauseForCall()
            } else {
                android.util.Log.d("EmergencyProvider", "Emergency call ENDED -> resuming Voice SOS if continuous mode was enabled")
                voiceSosService.resumeFromCall()
            }
        }
        scope.launch {
            deviceService.bleManager.sosEvents.collect { sosEvent ->
                android.util.Log.d("BleManager", "EMERGENCY: Physical SOS event received (Event #${sosEvent.eventId})")

                // 1. Check if physical SOS is currently in its cancellation window
                if (emergencyService.isPhysicalSosPendingCancellation()) {
                    android.util.Log.d("BleManager", "PHYSICAL SOS: Second press detected within cancellation window -> CANCEL SOS")
                    deviceService.addCommLog("⏹️ Second Physical SOS button press detected within cancellation window. SOS CANCELLED.")
                    val cancelled = emergencyService.cancelPendingPhysicalSos()
                    if (cancelled) {
                        alarmVibratorService.stopAlarm()
                        alarmVibratorService.stopVibration()
                        deviceService.resetEsp32()
                    }
                    return@collect
                }

                // 2. If an emergency is already active (past countdown window)
                if (emergencyService.isEmergencyActive() && !emergencyService.isCountdownActive()) {
                    android.util.Log.d("BleManager", "PHYSICAL SOS: Emergency is already active. Updating contacts.")
                    deviceService.addCommLog("🚨 Physical SOS pressed while emergency active. Updating emergency contacts.")
                    val currentModel = emergencyService.activeEmergency.value
                    if (currentModel != null) {
                        emergencyService.notifyEmergencyContacts(currentModel, isUpdate = true)
                    }
                    return@collect
                }

                // 3. First physical SOS button press: proceed with emergency trigger
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
                    is VoiceCommand.CancelSos -> {
                        // Voice SOS cancellation is routed through GuardianViewModel to enforce PIN security
                        voiceSosService.clearLastRecognizedCommand()
                    }
                    else -> {}
                }
            }
        }
    }


    
    fun getMatchedTrustedPlace(lat: Double, lng: Double): TrustedPlace? {
        val results = FloatArray(1)
        for (place in trustedPlacesService.trustedPlaces.value) {
            if (!place.isEnabled) continue
            android.location.Location.distanceBetween(lat, lng, place.latitude, place.longitude, results)
            if (results[0] <= place.radius) return place
        }
        return null
    }

    fun shouldPlaySosAlarm(lat: Double? = null, lng: Double? = null): Boolean {
        val isSoundEnabled = context.getSharedPreferences(
            "smart_sos_settings",
            Context.MODE_PRIVATE
        ).getBoolean("sos_sound_enabled", true)

        if (!isSoundEnabled) return false

        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        if (matchedPlace != null && matchedPlace.reduceNotificationSound) {
            android.util.Log.d("EmergencyProvider", "SOS Siren suppressed by Trusted Place: ${matchedPlace.name} (reduceNotificationSound=true)")
            return false
        }

        return true
    }

    fun shouldSkipPhoneCall(lat: Double? = null, lng: Double? = null): Boolean {
        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        if (matchedPlace != null && matchedPlace.skipAutomaticPhoneCall) {
            android.util.Log.d("EmergencyProvider", "Automatic phone call skipped by Trusted Place: ${matchedPlace.name} (skipAutomaticPhoneCall=true)")
            return true
        }

        return false
    }

    fun shouldSkipSms(lat: Double? = null, lng: Double? = null): Boolean {
        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        if (matchedPlace != null && matchedPlace.skipAutomaticSms) {
            android.util.Log.d("EmergencyProvider", "Automatic SMS skipped by Trusted Place: ${matchedPlace.name} (skipAutomaticSms=true)")
            return true
        }

        return false
    }

    fun shouldSendSos(lat: Double? = null, lng: Double? = null): Boolean {
        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        if (matchedPlace != null && !matchedPlace.alwaysSendSos) {
            android.util.Log.d("EmergencyProvider", "Automatic SOS dispatch prevented by Trusted Place: ${matchedPlace.name} (alwaysSendSos=false)")
            return false
        }

        return true
    }

    fun getDelaySosSeconds(lat: Double? = null, lng: Double? = null): Int {
        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        if (matchedPlace != null && matchedPlace.delaySosSeconds > 0) {
            android.util.Log.d("EmergencyProvider", "Trusted Place delay configured: ${matchedPlace.name} (${matchedPlace.delaySosSeconds}s)")
            return matchedPlace.delaySosSeconds
        }

        return 0
    }

    fun shouldShowConfirmationDialog(lat: Double? = null, lng: Double? = null): Boolean {
        val currentLat = lat ?: locationService.currentLocation.value.latitude
        val currentLng = lng ?: locationService.currentLocation.value.longitude

        val matchedPlace = getMatchedTrustedPlace(currentLat, currentLng)
        // If alwaysSendSos is false, the SOS will be suppressed anyway, so do not show confirmation dialog
        if (matchedPlace != null && !matchedPlace.alwaysSendSos) {
            return false
        }
        return matchedPlace != null && matchedPlace.showConfirmationDialog
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
            val effectiveLat = lat ?: locationService.currentLocation.value.latitude
            val effectiveLng = lng ?: locationService.currentLocation.value.longitude

            if (!shouldSendSos(effectiveLat, effectiveLng)) {
                val matchedPlace = getMatchedTrustedPlace(effectiveLat, effectiveLng)
                android.util.Log.d("EmergencyProvider", "SOS dispatch cancelled: Trusted Place ${matchedPlace?.name} has alwaysSendSos=false")
                return@launch
            }

            val isSoundAllowed = shouldPlaySosAlarm(effectiveLat, effectiveLng)
            val skipCall = shouldSkipPhoneCall(effectiveLat, effectiveLng)
            val skipSms = shouldSkipSms(effectiveLat, effectiveLng)
            val delaySeconds = getDelaySosSeconds(effectiveLat, effectiveLng)

            val isVibrationEnabled = context.getSharedPreferences(
                "smart_sos_settings",
                Context.MODE_PRIVATE
            ).getBoolean("sos_vibration_enabled", true)

            if (isEmergencyInProgress()) {
                val model = emergencyService.activeEmergency.value
                if (model != null && model.status != "COUNTDOWN") {
                    emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                }
                if (isSoundAllowed) {
                    alarmVibratorService.startAlarm()
                }
                if (isVibrationEnabled) {
                    alarmVibratorService.startVibration()
                }
                return@launch
            }
            
            val user = (authService.authState.value as? com.example.service.AuthState.Success)?.user
            val userId = user?.uid ?: "user-101"
            val userName = user?.name ?: "Marcus Vance"
            val userPhone = user?.phone ?: "+1-555-0143"

            // Trigger alarm conditionally based on sos_sound_enabled and Trusted Place settings
            if (isSoundAllowed) {
                alarmVibratorService.startAlarm()
            }
            if (isVibrationEnabled) {
                alarmVibratorService.startVibration()
            }

            val matchedPlace = getMatchedTrustedPlace(effectiveLat, effectiveLng)

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
                locationSource = locationSource,
                skipPhoneCall = skipCall,
                skipSms = skipSms,
                delaySosSeconds = delaySeconds,
                trustedPlaceName = matchedPlace?.name
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

    fun triggerFallEmergency() {
        val hwGps = deviceService.bleManager.latestHardwareGpsLocation.value
        val isGpsValid = deviceService.bleManager.hardwareGpsState.value is com.example.ble.HardwareGpsState.ValidLocation && hwGps != null

        if (isGpsValid && hwGps != null) {
            triggerEmergency(
                triggerSource = "FALL_DETECTED",
                deviceId = "ESP32-SOS-BAND-81F4",
                lat = hwGps.latitude,
                lng = hwGps.longitude,
                accuracy = 3.0f,
                locationSource = "ESP32_NEO6M"
            )
        } else {
            triggerEmergency(
                triggerSource = "FALL_DETECTED",
                deviceId = "ESP32-SOS-BAND-81F4",
                lat = null,
                lng = null,
                accuracy = null,
                locationSource = "PHONE_GPS"
            )
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
    ): EmergencyModel? {
        val effectiveLat = lat ?: locationService.currentLocation.value.latitude
        val effectiveLng = lng ?: locationService.currentLocation.value.longitude

        if (!shouldSendSos(effectiveLat, effectiveLng)) {
            val matchedPlace = getMatchedTrustedPlace(effectiveLat, effectiveLng)
            android.util.Log.d("EmergencyProvider", "Automatic SOS dispatch prevented by Trusted Place: ${matchedPlace?.name} (alwaysSendSos=false)")
            return null
        }

        val skipCall = shouldSkipPhoneCall(effectiveLat, effectiveLng)
        val skipSms = shouldSkipSms(effectiveLat, effectiveLng)
        val delaySeconds = getDelaySosSeconds(effectiveLat, effectiveLng)

        val matchedPlace = getMatchedTrustedPlace(effectiveLat, effectiveLng)

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
            customBearing = bearing,
            skipPhoneCall = skipCall,
            skipSms = skipSms,
            delaySosSeconds = delaySeconds,
            trustedPlaceName = matchedPlace?.name
        )
    }

    suspend fun cancelEmergency(pin: String, expectedPin: String, reason: String): Boolean {
        val success = emergencyService.cancelEmergencyWithPin(pin, expectedPin, reason)
        if (success) {
            alarmVibratorService.stopAlarm()
            alarmVibratorService.stopVibration()
        }
        return success
    }

    fun markEmergencySafe() {
        emergencyService.markSafeAndClose()
        alarmVibratorService.stopAlarm()
        alarmVibratorService.stopVibration()
    }
}
