package com.example.service

import android.content.Context
import android.util.Log
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
        customBearing: Float? = null
    ): EmergencyModel {
        // Prevent duplicate SOS sessions
        _activeEmergency.value?.let {
            Log.w("EmergencyService", "An active emergency session is already running: ${it.emergencyId}")
            return it
        }
        
        if (countdownJob?.isActive == true) {
            Log.w("EmergencyService", "Countdown already running.")
            return EmergencyModel(emergencyId = "PENDING", userId = userId, userName = userName, userPhone = userPhone, startTimeMs = System.currentTimeMillis(), latitude = 0.0, longitude = 0.0, status = "COUNTDOWN", triggerType = triggerType, aiConfidenceScore = 100, contactsNotified = listOf(), responderStatus = "COUNTDOWN", deviceId = deviceId)
        }

        // Create a unique Emergency ID
        val emergencyId = "EMG-" + UUID.randomUUID().toString().take(8).uppercase()

        // Get current coordinates
        val currentLoc = locationService.currentLocation.value
        val lat = customLat ?: currentLoc.latitude
        val lng = customLng ?: currentLoc.longitude
        val accuracy = customAccuracy ?: currentLoc.accuracy
        val altitude = customAltitude ?: currentLoc.altitude
        val speed = customSpeed ?: currentLoc.speed.toFloat()
        val bearing = customBearing ?: currentLoc.bearing

        val pendingModel = EmergencyModel(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            startTimeMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            accuracy = accuracy,
            altitude = altitude,
            speed = speed,
            bearing = bearing,
            status = "COUNTDOWN",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = "COUNTDOWN ACTIVE",
            deviceId = deviceId
        )
        
        _activeEmergency.value = pendingModel
        
        countdownJob = serviceScope.launch {
            for (i in 5 downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = null
            
            val model = pendingModel.copy(status = "ACTIVE", responderStatus = "SOS TRIGGERED - BROADCASTING")
            _activeEmergency.value = model

            // Record event & timestamp in Firestore
            saveEmergencyToCloud(model)

            // Send push notification
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "🚨 EMERGENCY SOS ACTIVE",
                    body = "SOS triggered by $userName ($triggerType). Location broadcasting live.",
                    type = NotificationType.EMERGENCY,
                    deviceId = deviceId
                )
            )

            // Notify emergency contacts
            notifyEmergencyContacts(model)
            
            // Make an automated real-time phone call to the first contact or 911
            withContext(Dispatchers.Main) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val primaryContact = databaseService.contacts.value.firstOrNull()
                    val phoneToCall = primaryContact?.phone ?: "911"
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneToCall")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(callIntent)
                    } catch (e: Exception) {
                        Log.e("EmergencyService", "Failed to initiate real-time call: ${e.message}")
                    }
                }
            }

            // Start updating location every 3-5 seconds
            startHighFrequencyLocationUpdates(emergencyId)
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
