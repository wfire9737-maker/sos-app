package com.example.service

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.EmergencyModel
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telecom.TelecomManager

import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.SosHistoryEntity

class EmergencyService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val locationService: LocationService,
    private val notificationService: NotificationService,
    private val databaseService: DatabaseService,
    private val sosHistoryDao: SosHistoryDao? = null,
    private val contactDao: EmergencyContactDao? = null
) {
    var onCallStateChanged: ((Boolean) -> Unit)? = null
    private var isCallActive = false
    @Volatile private var initialSmsSentWithInvalidLocation: Boolean = false
    @Volatile private var followUpLocationSmsSent: Boolean = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private var lastCalledEmergencyId: String? = null
    private var countdownJob: Job? = null

    init {
        registerTelephonyCallStateListener()
    }

    private fun registerTelephonyCallStateListener() {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleTelephonyCallState(state)
                    }
                }
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleTelephonyCallState(state)
                    }
                }
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            Log.w("EmergencyService", "Telephony call state listener registration failed/restricted: ${e.message}")
        }
    }

    private fun handleTelephonyCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                if (!isCallActive) {
                    isCallActive = true
                    Log.d("EmergencyService", "Telephony call state ACTIVE ($state)")
                    onCallStateChanged?.invoke(true)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCallActive) {
                    isCallActive = false
                    Log.d("EmergencyService", "Telephony call state IDLE ($state)")
                    onCallStateChanged?.invoke(false)
                }
            }
        }
    }

    private val _activeEmergency = MutableStateFlow<EmergencyModel?>(null)
    val activeEmergency: StateFlow<EmergencyModel?> = _activeEmergency.asStateFlow()
    
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private val stateTransitionLock = Any()

    fun isEmergencyActive(): Boolean = _activeEmergency.value != null || countdownJob?.isActive == true

    fun isPhysicalSosPendingCancellation(): Boolean {
        synchronized(stateTransitionLock) {
            return countdownJob?.isActive == true &&
                    _activeEmergency.value?.status == "COUNTDOWN" &&
                    _activeEmergency.value?.triggerType == "PHYSICAL_BLE_BUTTON"
        }
    }

    fun isCountdownActive(): Boolean {
        synchronized(stateTransitionLock) {
            return countdownJob?.isActive == true && _activeEmergency.value?.status == "COUNTDOWN"
        }
    }

    suspend fun cancelPendingPhysicalSos(): Boolean {
        synchronized(stateTransitionLock) {
            if (countdownJob?.isActive == true &&
                _activeEmergency.value?.status == "COUNTDOWN" &&
                _activeEmergency.value?.triggerType == "PHYSICAL_BLE_BUTTON"
            ) {
                countdownJob?.cancel()
                _countdown.value = null
                val noteText = "Physical SOS cancelled by second button press within cancellation window"
                val abortedModel = _activeEmergency.value?.copy(
                    status = "CANCELLED",
                    endTimeMs = System.currentTimeMillis(),
                    responderStatus = "CANCELLED BY SECOND PHYSICAL BUTTON PRESS",
                    notes = noteText
                )
                _activeEmergency.value = null
                databaseService.addDeveloperLog("PHYSICAL_SOS_CANCELLED: $noteText", "INFO")
                if (abortedModel != null) {
                    saveEmergencyToCloud(abortedModel)
                }
                closeActiveSession()
                return true
            }
        }
        return false
    }

    fun startEmergency(
        userId: String,
        userName: String,
        userPhone: String,
        triggerType: String,
        deviceId: String = "ESP32-SOS-BAND-81F4",
        customLat: Double? = null,
        customLng: Double? = null,
        customAccuracy: Float? = null,
        customAltitude: Double? = null,
        customSpeed: Float? = null,
        customBearing: Float? = null,
        locationSource: String = "PHONE_GPS",
        skipPhoneCall: Boolean = false,
        skipSms: Boolean = false,
        delaySosSeconds: Int = 0,
        trustedPlaceName: String? = null
    ): EmergencyModel {
        // Reset location SMS state for new emergency
        initialSmsSentWithInvalidLocation = false
        followUpLocationSmsSent = false

        // Prevent duplicate SOS sessions
        _activeEmergency.value?.let {
            Log.w("EmergencyService", "An active emergency session is already running: ${it.emergencyId}")
            return it
        }
        
        if (countdownJob?.isActive == true) {
            Log.w("EmergencyService", "Countdown already running.")
            return EmergencyModel(emergencyId = "PENDING", userId = userId, userName = userName, userPhone = userPhone, startTimeMs = System.currentTimeMillis(), latitude = 0.0, longitude = 0.0, status = "COUNTDOWN", triggerType = triggerType, aiConfidenceScore = 100, contactsNotified = listOf(), responderStatus = "COUNTDOWN", deviceId = deviceId, locationSource = locationSource)
        }

        // Create a unique Emergency ID
        val emergencyId = "EMG-" + UUID.randomUUID().toString().take(8).uppercase()

        // Create pending model for the countdown window without requesting GPS or uploading to cloud
        val initialLat = customLat ?: 0.0
        val initialLng = customLng ?: 0.0

        val resolvedLocationName = if (!trustedPlaceName.isNullOrBlank()) {
            "$trustedPlaceName (Within trusted location)"
        } else {
            "GPS Coordinate Plot"
        }

        val windowDuration = when (triggerType) {
            "PHYSICAL_BLE_BUTTON" -> {
                context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
                    .getInt(
                        com.example.repository.SettingsRepository.KEY_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS,
                        com.example.repository.SettingsRepository.DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS
                    )
            }
            "MANUAL" -> {
                context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
                    .getInt(
                        com.example.repository.SettingsRepository.KEY_IN_APP_SOS_ACTIVATION_DELAY_SECONDS,
                        com.example.repository.SettingsRepository.DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS
                    )
            }
            else -> 5
        }
        val isImmediate = windowDuration == 0 && delaySosSeconds == 0

        val pendingModel = EmergencyModel(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            startTimeMs = System.currentTimeMillis(),
            latitude = initialLat,
            longitude = initialLng,
            accuracy = customAccuracy ?: 0f,
            altitude = customAltitude ?: 0.0,
            speed = customSpeed ?: 0f,
            bearing = customBearing ?: 0f,
            locationName = resolvedLocationName,
            status = if (isImmediate) "ACTIVE" else "COUNTDOWN",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = if (isImmediate) "SOS TRIGGERED - BROADCASTING" else if (delaySosSeconds > 0) "DELAYED (${delaySosSeconds}s)" else "COUNTDOWN ACTIVE",
            deviceId = deviceId,
            locationSource = locationSource
        )
        
        _activeEmergency.value = pendingModel
        saveEmergencyToCloud(pendingModel)
        
        countdownJob = serviceScope.launch {
            if (!isImmediate) {
                if (delaySosSeconds > 0) {
                    Log.d("EmergencyService", "TRUSTED PLACE DELAY: Waiting ${delaySosSeconds}s before starting SOS countdown")
                    databaseService.addDeveloperLog("TRUSTED_PLACE_DELAY: Waiting ${delaySosSeconds}s before SOS dispatch", "INFO")
                    delay(delaySosSeconds * 1000L)
                    Log.d("EmergencyService", "TRUSTED PLACE DELAY: Delay finished, starting countdown")
                }

                if (windowDuration > 0) {
                    Log.d("SOS_ESP32", "SOS COUNTDOWN STARTED (Duration: ${windowDuration}s, Trigger: $triggerType)")
                    for (i in windowDuration downTo 1) {
                        val shouldContinue = synchronized(stateTransitionLock) {
                            isActive && _activeEmergency.value?.status == "COUNTDOWN"
                        }
                        if (!shouldContinue) {
                            Log.d("SOS_ESP32", "SOS COUNTDOWN ABORTED (cancelled during window)")
                            return@launch
                        }
                        Log.d("SOS_ESP32", "SOS COUNTDOWN: $i")
                        _countdown.value = i
                        delay(1000)
                    }
                    Log.d("SOS_ESP32", "SOS COUNTDOWN FINISHED")
                }

                // Atomic state transition to ACTIVE
                val canProceed = synchronized(stateTransitionLock) {
                    if (!isActive || _activeEmergency.value == null || _activeEmergency.value?.status != "COUNTDOWN") {
                        false
                    } else {
                        _countdown.value = null
                        val currentLoc = locationService.currentLocation.value // Fallback
                        var model = pendingModel.copy(
                            latitude = customLat ?: currentLoc.latitude,
                            longitude = customLng ?: currentLoc.longitude,
                            accuracy = customAccuracy ?: currentLoc.accuracy,
                            altitude = customAltitude ?: currentLoc.altitude,
                            speed = customSpeed ?: currentLoc.speed.toFloat(),
                            bearing = customBearing ?: currentLoc.bearing,
                            status = "ACTIVE",
                            responderStatus = "SOS TRIGGERED - BROADCASTING",
                            locationSource = locationSource
                        )
                        _activeEmergency.value = model
                        true
                    }
                }

                if (!canProceed) {
                    Log.d("SOS_ESP32", "SOS WORKFLOW SUPPRESSED (Emergency was cancelled before activation)")
                    return@launch
                }
            } else {
                _countdown.value = null
            }

            Log.d("SOS_ESP32", "STARTING EMERGENCY WORKFLOW")
            
            // Immediate UI update first
            val currentLoc = locationService.currentLocation.value // Fallback
            var model = pendingModel.copy(
                latitude = customLat ?: currentLoc.latitude,
                longitude = customLng ?: currentLoc.longitude,
                accuracy = customAccuracy ?: currentLoc.accuracy,
                altitude = customAltitude ?: currentLoc.altitude,
                speed = customSpeed ?: currentLoc.speed.toFloat(),
                bearing = customBearing ?: currentLoc.bearing,
                status = "ACTIVE",
                responderStatus = "SOS TRIGGERED - BROADCASTING",
                locationSource = locationSource
            )
            _activeEmergency.value = model

            // Elevate to Foreground Service IMMEDIATELY to protect process from background restrictions
            startHighFrequencyLocationUpdates(emergencyId)

            // Independent Action: Call (Do not wait for slow GPS location!)
            if (skipPhoneCall) {
                Log.d("EmergencyService", "CALL_SKIPPED: Automatic emergency phone call skipped due to Trusted Place setting (skipAutomaticPhoneCall=true)")
                databaseService.addDeveloperLog("CALL_SKIPPED: Automatic phone call skipped by Trusted Place setting", "INFO")
            } else {
                launch(Dispatchers.Main) {
                    val primaryContact = databaseService.contacts.value.firstOrNull()
                    val phoneToCall = primaryContact?.phone?.trim() ?: ""

                    if (phoneToCall.isEmpty() || !phoneToCall.matches(Regex("^[+]?[0-9\\s-]{3,15}$"))) {
                        databaseService.addDeveloperLog("CALL_SKIPPED: No valid emergency contact phone number configured", "WARN")
                        Log.w("EmergencyService", "CALL_SKIPPED: No valid emergency contact phone number configured")
                    } else {
                        databaseService.addDeveloperLog("CALL_REQUESTED: $phoneToCall (ID: $emergencyId)", "INFO")

                        if (lastCalledEmergencyId == emergencyId) {
                            Log.w("EmergencyService", "Call already placed for emergency: $emergencyId")
                        } else {
                            lastCalledEmergencyId = emergencyId
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                Log.d("EmergencyService", "CALL_REQUESTED: Attempting background dial to $phoneToCall")
                                try {
                                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                    val uri = Uri.fromParts("tel", phoneToCall, null)
                                    if (telecomManager != null) {
                                        isCallActive = true
                                        onCallStateChanged?.invoke(true)
                                        telecomManager.placeCall(uri, null)
                                        databaseService.addDeveloperLog("CALL_STARTED: tel:$phoneToCall via TelecomManager", "SUCCESS")
                                        Log.d("EmergencyService", "CALL_STARTED: Successfully placed call via TelecomManager.")
                                    } else {
                                        if (isCallActive) {
                                            isCallActive = false
                                            onCallStateChanged?.invoke(false)
                                        }
                                        databaseService.addDeveloperLog("CALL_FAILED: TelecomManager.placeCall: TelecomManager is null", "ERROR")
                                        Log.e("EmergencyService", "CALL_FAILED: TelecomManager is null")
                                    }
                                } catch (e: Exception) {
                                    if (isCallActive) {
                                        isCallActive = false
                                        onCallStateChanged?.invoke(false)
                                    }
                                    databaseService.addDeveloperLog("CALL_FAILED: TelecomManager.placeCall: ${e.message}", "ERROR")
                                    Log.e("EmergencyService", "CALL_FAILED: TelecomManager.placeCall failed: ${e.message}")
                                }
                            } else {
                                databaseService.addDeveloperLog("CALL_PERMISSION_DENIED: CALL_PHONE permission not granted", "ERROR")
                                Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")
                            }
                        }
                    }
                }
            }

            // Independent Action: Acquire high-accuracy location, notify Cloud and SMS
            launch {
                val highAccuracyLoc = locationService.getCurrentLocationOnce(3000)
                if (highAccuracyLoc != null) {
                    model = model.copy(
                        latitude = customLat ?: highAccuracyLoc.latitude,
                        longitude = customLng ?: highAccuracyLoc.longitude,
                        accuracy = customAccuracy ?: highAccuracyLoc.accuracy,
                        altitude = customAltitude ?: highAccuracyLoc.altitude,
                        speed = customSpeed ?: highAccuracyLoc.speed.toFloat(),
                        bearing = customBearing ?: highAccuracyLoc.bearing
                    )
                    _activeEmergency.value = model
                }
                
                // Now execute subsequent network/cloud/SMS tasks concurrently
                launch { saveEmergencyToCloud(model) }
                if (skipSms) {
                    Log.d("EmergencyService", "SMS_SKIPPED: Automatic emergency SMS skipped due to Trusted Place setting (skipAutomaticSms=true)")
                    databaseService.addDeveloperLog("SMS_SKIPPED: Automatic SMS skipped by Trusted Place setting", "INFO")
                } else {
                    initialSmsSentWithInvalidLocation = (model.latitude == 0.0 && model.longitude == 0.0)
                    launch { notifyEmergencyContacts(model) }
                }
                launch {
                    notificationService.addNotification(
                        NotificationItem(
                            id = UUID.randomUUID().toString(),
                            title = "🚨 EMERGENCY SOS ACTIVE",
                            body = "SOS triggered by $userName ($triggerType). Location broadcasting live.",
                            type = NotificationType.EMERGENCY,
                            deviceId = deviceId
                        )
                    )
                }
            }
        }

        return pendingModel
    }

    fun notifyEmergencyContacts(model: EmergencyModel, isUpdate: Boolean = false) {
        val contacts = databaseService.contacts.value
        val smsManager: android.telephony.SmsManager? = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            android.telephony.SmsManager.getDefault()
        }

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = dateFormat.format(java.util.Date())
        val sentPhones = mutableSetOf<String>()

        val eventType = if (model.triggerType.contains("FALL", ignoreCase = true)) {
            "FALL DETECTED"
        } else {
            "SOS TRIGGERED"
        }

        val locationUrl = if (model.latitude != 0.0 || model.longitude != 0.0) {
            "https://maps.google.com/?q=${model.latitude},${model.longitude}"
        } else {
            "Location unavailable"
        }

        contacts.forEach { contact ->
            if (sentPhones.contains(contact.phone)) return@forEach
            sentPhones.add(contact.phone)
            
            val message = if (isUpdate) {
                "LIVE UPDATE!\n${model.userName} is still in an active emergency.\n\nEmergency: $eventType\nLocation: $locationUrl\nTime: $timestamp"
            } else {
                val customTemplate = contact.customSmsTemplate?.trim()
                val customPortion = if (!customTemplate.isNullOrBlank()) {
                    customTemplate
                } else {
                    if (eventType == "FALL DETECTED") {
                        "EMERGENCY!\n${model.userName} may have suffered a fall and needs assistance."
                    } else {
                        "EMERGENCY!\n${model.userName} has triggered an SOS. Please help immediately."
                    }
                }
                "$customPortion\n\nEmergency: $eventType\nLocation: $locationUrl\nTime: $timestamp"
            }
            try {
                // For long SMS, we should use sendMultipartTextMessage
                val parts = smsManager?.divideMessage(message)
                if (parts != null) {
                    smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                } else {
                    smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                }
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to send real SMS to ${contact.phone}: ${e.message}")
            }
            
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "📞 Notified Contact: ${contact.name}",
                    body = if (isUpdate) "Real-time location SMS sent to ${contact.relationship} at ${contact.phone}." else "SMS sent to ${contact.relationship} at ${contact.phone} with emergency coordinates (${model.latitude}, ${model.longitude}).",
                    type = NotificationType.EMERGENCY
                )
            )
        }
    }

    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
        
        val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
            action = "START_LOCATION_SERVICE"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(3500) // 3-5 seconds frequency (3.5s)
                val currentLoc = locationService.currentLocation.value
                val currentModel = _activeEmergency.value
                if (currentModel != null && currentModel.emergencyId == emergencyId) {
                    val updatedModel = currentModel.copy(
                        latitude = currentLoc.latitude,
                        longitude = currentLoc.longitude,
                        responderStatus = "LIVE LOCATION UPDATING..."
                    )
                    _activeEmergency.value = updatedModel
                    saveEmergencyToCloud(updatedModel)

                    if (initialSmsSentWithInvalidLocation && !followUpLocationSmsSent && (updatedModel.latitude != 0.0 || updatedModel.longitude != 0.0)) {
                        followUpLocationSmsSent = true
                        Log.d("EmergencyService", "F-05: Initial SMS had invalid location. Valid location acquired (${updatedModel.latitude}, ${updatedModel.longitude}). Sending follow-up SMS.")
                        databaseService.addDeveloperLog("SMS_FOLLOWUP_SENT: Valid GPS location acquired (${updatedModel.latitude}, ${updatedModel.longitude})", "INFO")
                        launch { notifyEmergencyContacts(updatedModel, isUpdate = true) }
                    }
                }
            }
        }
    }

    private fun saveEmergencyToCloud(model: EmergencyModel) {
        serviceScope.launch {
            try {
                val duration = if (model.endTimeMs != null && model.endTimeMs > model.startTimeMs) {
                    (model.endTimeMs - model.startTimeMs) / 1000
                } else if (model.startTimeMs > 0 && (model.status == "MARKED_SAFE" || model.status == "CANCELLED" || model.status == "RESOLVED")) {
                    (System.currentTimeMillis() - model.startTimeMs) / 1000
                } else {
                    0L
                }

                val severityGrade = if (model.triggerType.contains("FALL", ignoreCase = true)) {
                    "CRITICAL"
                } else if (model.status == "CANCELLED" && model.notes.contains("countdown", ignoreCase = true)) {
                    "WARNING"
                } else {
                    "HIGH"
                }

                val entity = SosHistoryEntity(
                    historyId = model.emergencyId,
                    uid = model.userId,
                    triggerSource = model.triggerType,
                    status = model.status,
                    date = model.startTimeMs,
                    latitude = model.latitude,
                    longitude = model.longitude,
                    googleMapsLink = "https://maps.google.com/?q=${model.latitude},${model.longitude}",
                    durationSeconds = duration,
                    address = model.locationName,
                    severity = severityGrade,
                    contactsNotified = model.contactsNotified.joinToString("; "),
                    deviceUsed = model.deviceId,
                    resolutionNotes = model.notes.ifBlank { model.responderStatus },
                    resolvedBy = model.userName,
                    aiConfidence = model.aiConfidenceScore
                )
                sosHistoryDao?.insertHistory(entity)

                val fs = firestore
                if (fs != null) {
                    val authUid = getAuthenticatedUid()
                    if (authUid != null) {
                        try {
                            val historyMap = mapOf(
                                "historyId" to entity.historyId,
                                "uid" to authUid,
                                "triggerSource" to entity.triggerSource,
                                "status" to entity.status,
                                "date" to entity.date,
                                "latitude" to entity.latitude,
                                "longitude" to entity.longitude,
                                "googleMapsLink" to entity.googleMapsLink,
                                "durationSeconds" to entity.durationSeconds,
                                "address" to entity.address,
                                "severity" to entity.severity,
                                "contactsNotified" to entity.contactsNotified,
                                "deviceUsed" to entity.deviceUsed,
                                "resolutionNotes" to entity.resolutionNotes,
                                "resolvedBy" to entity.resolvedBy,
                                "aiConfidence" to entity.aiConfidence
                            )
                            fs.collection("users").document(authUid)
                                .collection("emergency_history").document(entity.historyId)
                                .set(historyMap, com.google.firebase.firestore.SetOptions.merge())
                                .await()
                        } catch (e: Exception) {
                            Log.e("EmergencyService", "Failed to sync emergency history to user subcollection: ${e.message}")
                        }
                    }

                    try {
                        fs.collection("emergencies").document(model.emergencyId).set(model.toMap()).await()
                    } catch (e: Exception) {
                        Log.e("EmergencyService", "Failed to sync emergency to Firestore: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to save emergency to Room / sync: ${e.message}")
            }
        }
    }

    suspend fun cancelEmergencyWithPin(pin: String, expectedPin: String, notes: String = "Cancelled with PIN"): Boolean {
        if (expectedPin.isNotEmpty() && pin != expectedPin) {
            Log.w("EmergencyService", "PIN mismatch during emergency cancellation attempt.")
            return false
        }

        val wasCountdownCancelled = synchronized(stateTransitionLock) {
            if (countdownJob?.isActive == true && _activeEmergency.value?.status == "COUNTDOWN") {
                val noteText = if (notes.isNotBlank() && notes != "Cancelled with PIN") notes else "False alarm: Aborted during countdown"
                val abortedModel = _activeEmergency.value?.copy(
                    status = "CANCELLED",
                    endTimeMs = System.currentTimeMillis(),
                    responderStatus = "CANCELLED DURING COUNTDOWN",
                    notes = noteText
                )
                countdownJob?.cancel()
                _countdown.value = null
                _activeEmergency.value = null
                databaseService.addDeveloperLog("CALL_CANCELLED: Countdown aborted by user ($noteText)", "INFO")
                if (abortedModel != null) {
                    saveEmergencyToCloud(abortedModel)
                }
                closeActiveSession()
                true
            } else {
                false
            }
        }
        if (wasCountdownCancelled) {
            return true
        }
        
        if (pin != expectedPin) {
            Log.w("EmergencyService", "PIN mismatch during emergency cancellation attempt.")
            return false
        }

        val currentModel = _activeEmergency.value ?: return false
        
        val updatedModel = currentModel.copy(
            status = "CANCELLED",
            endTimeMs = System.currentTimeMillis(),
            responderStatus = "CANCELLED BY USER",
            notes = if (notes.isNotBlank()) notes else "Cancelled with security PIN"
        )

        databaseService.addDeveloperLog("CALL_CANCELLED: Emergency cancelled with PIN", "INFO")
        saveEmergencyToCloud(updatedModel)
        closeActiveSession()
        return true
    }

    fun markSafeAndClose() {
        val currentModel = _activeEmergency.value ?: return
        val updatedModel = currentModel.copy(
            status = "MARKED_SAFE",
            endTimeMs = System.currentTimeMillis(),
            responderStatus = "MARKED SAFE - ALL CLEAR",
            notes = "User marked safe."
        )

        databaseService.addDeveloperLog("CALL_COMPLETED/RETURNED: Marked safe and emergency closed", "INFO")
        saveEmergencyToCloud(updatedModel)
        closeActiveSession()
    }

    suspend fun resolveEmergency(emergencyId: String, resolvedBy: String, notes: String) {
        try {
            sosHistoryDao?.updateResolution(emergencyId, notes, resolvedBy)
        } catch (e: Exception) {
            Log.e("EmergencyService", "Failed to update emergency resolution in Room: ${e.message}")
        }

        val authUid = getAuthenticatedUid()
        if (authUid != null) {
            val fs = firestore ?: return
            try {
                val updateMap = mapOf(
                    "status" to "RESOLVED",
                    "resolutionNotes" to notes,
                    "resolvedBy" to resolvedBy
                )
                fs.collection("users").document(authUid)
                    .collection("emergency_history").document(emergencyId)
                    .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to sync emergency resolution to user subcollection: ${e.message}")
            }
        }
    }

    private fun getAuthenticatedUid(): String? {
        return try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank() || uid.startsWith("demo-", ignoreCase = true) || uid == "user-101" || uid == "anonymous") {
                null
            } else {
                uid
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun closeActiveSession() {
        initialSmsSentWithInvalidLocation = false
        followUpLocationSmsSent = false
        if (isCallActive) {
            isCallActive = false
            onCallStateChanged?.invoke(false)
        }
        trackingJob?.cancel()
        trackingJob = null
        _activeEmergency.value = null
        
        val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
            action = "STOP_LOCATION_SERVICE"
        }
        context.startService(intent)
    }
}
