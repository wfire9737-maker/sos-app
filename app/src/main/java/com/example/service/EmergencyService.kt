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
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private var lastCalledEmergencyId: String? = null
    private var countdownJob: Job? = null

    private val _activeEmergency = MutableStateFlow<EmergencyModel?>(null)
    val activeEmergency: StateFlow<EmergencyModel?> = _activeEmergency.asStateFlow()
    
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    fun isEmergencyActive(): Boolean = _activeEmergency.value != null || countdownJob?.isActive == true

    suspend fun startEmergency(
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
        locationSource: String = "PHONE_GPS"
    ): EmergencyModel {
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
            status = "COUNTDOWN",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = "COUNTDOWN ACTIVE",
            deviceId = deviceId,
            locationSource = locationSource
        )
        
        _activeEmergency.value = pendingModel
        
        countdownJob = serviceScope.launch {
            Log.d("SOS_ESP32", "SOS COUNTDOWN STARTED")
            for (i in 5 downTo 1) {
                Log.d("SOS_ESP32", "SOS COUNTDOWN: $i")
                _countdown.value = i
                delay(1000)
            }
            Log.d("SOS_ESP32", "SOS COUNTDOWN FINISHED")
            Log.d("SOS_ESP32", "STARTING EMERGENCY WORKFLOW")
            _countdown.value = null
            
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
            launch(Dispatchers.Main) {
                val primaryContact = databaseService.contacts.value.firstOrNull()
                val phoneToCall = primaryContact?.phone ?: "911"
                databaseService.addDeveloperLog("CALL_REQUESTED: $phoneToCall (ID: $emergencyId)", "INFO")

                if (lastCalledEmergencyId == emergencyId) {
                    Log.w("EmergencyService", "Call already placed for emergency: $emergencyId")
                } else {
                    lastCalledEmergencyId = emergencyId
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        Log.d("EmergencyService", "CALL_REQUESTED: Attempting background dial to $phoneToCall")
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$phoneToCall")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(callIntent)
                            databaseService.addDeveloperLog("CALL_STARTED: tel:$phoneToCall", "SUCCESS")
                            Log.d("EmergencyService", "CALL_STARTED: Successfully launched dialer activity.")
                        } catch (e: Exception) {
                            databaseService.addDeveloperLog("CALL_FAILED: ${e.message}", "ERROR")
                            Log.e("EmergencyService", "CALL_FAILED: Failed to start background call activity: ${e.message}")
                        }
                    } else {
                        databaseService.addDeveloperLog("CALL_PERMISSION_DENIED: CALL_PHONE permission not granted", "ERROR")
                        Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")
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
                launch { notifyEmergencyContacts(model) }
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
        val timestamp = dateFormat.format(java.util.Date(model.startTimeMs))
        val sentPhones = mutableSetOf<String>()

        contacts.forEach { contact ->
            if (sentPhones.contains(contact.phone)) return@forEach
            sentPhones.add(contact.phone)
            
            val message = if (isUpdate) {
                "LIVE UPDATE!\n${model.userName} is still in an active emergency.\n\nLocation:\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\n\nTime: $timestamp"
            } else {
                "EMERGENCY!\n${model.userName} has triggered an SOS.\n\nLocation:\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\n\nPlease contact immediately.\n\nTime: $timestamp"
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
                }
            }
        }
    }

    private fun saveEmergencyToCloud(model: EmergencyModel) {
        serviceScope.launch {
            try {
                val entity = SosHistoryEntity(
                    historyId = model.emergencyId,
                    uid = model.userId,
                    triggerSource = model.triggerType,
                    status = model.status,
                    date = model.startTimeMs,
                    latitude = model.latitude,
                    longitude = model.longitude,
                    googleMapsLink = "https://maps.google.com/?q=${model.latitude},${model.longitude}"
                )
                sosHistoryDao?.insertHistory(entity)
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to save emergency to Room: ${e.message}")
            }
            
            val fs = firestore ?: return@launch
            try {
                fs.collection("emergencies").document(model.emergencyId).set(model.toMap()).await()
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to sync emergency to Firestore: ${e.message}")
            }
        }
    }

    suspend fun cancelEmergencyWithPin(pin: String, expectedPin: String, notes: String = "Cancelled with PIN"): Boolean {
        if (countdownJob?.isActive == true) {
            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            databaseService.addDeveloperLog("CALL_CANCELLED: Countdown aborted by user", "INFO")
            closeActiveSession()
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
            notes = notes
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
            notes = "Completed safety verification cycle."
        )

        databaseService.addDeveloperLog("CALL_COMPLETED/RETURNED: Marked safe and emergency closed", "INFO")
        saveEmergencyToCloud(updatedModel)
        closeActiveSession()
    }

    private fun closeActiveSession() {
        trackingJob?.cancel()
        trackingJob = null
        _activeEmergency.value = null
        
        val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
            action = "STOP_LOCATION_SERVICE"
        }
        context.startService(intent)
    }
}
