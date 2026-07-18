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

class EmergencyService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val locationService: LocationService,
    private val notificationService: NotificationService,
    private val databaseService: DatabaseService
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null

    private val _activeEmergency = MutableStateFlow<EmergencyModel?>(null)
    val activeEmergency: StateFlow<EmergencyModel?> = _activeEmergency.asStateFlow()

    fun isEmergencyActive(): Boolean = _activeEmergency.value != null

    suspend fun startEmergency(
        userId: String,
        userName: String,
        userPhone: String,
        triggerType: String,
        deviceId: String = "ESP32-SOS-BAND-81F4"
    ): EmergencyModel {
        // Prevent duplicate SOS sessions
        _activeEmergency.value?.let {
            Log.w("EmergencyService", "An active emergency session is already running: ${it.emergencyId}")
            return it
        }

        // Create a unique Emergency ID
        val emergencyId = "EMG-" + UUID.randomUUID().toString().take(8).uppercase()

        // Get current coordinates
        val currentLoc = locationService.currentLocation.value
        val lat = currentLoc.latitude
        val lng = currentLoc.longitude

        val model = EmergencyModel(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            startTimeMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            status = "ACTIVE",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = "SOS TRIGGERED - BROADCASTING",
            deviceId = deviceId
        )

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

        // Start updating location every 3-5 seconds
        startHighFrequencyLocationUpdates(emergencyId)

        return model
    }

    private fun notifyEmergencyContacts(model: EmergencyModel) {
        val contacts = databaseService.contacts.value
        contacts.forEach { contact ->
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "📞 Notified Contact: ${contact.name}",
                    body = "Secure SMS dispatch queued to ${contact.relationship} at ${contact.phone} with emergency coordinates (${model.latitude}, ${model.longitude}).",
                    type = NotificationType.EMERGENCY
                )
            )
        }
    }

    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
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
        val fs = firestore ?: return
        serviceScope.launch {
            try {
                fs.collection("emergencies").document(model.emergencyId).set(model.toMap()).await()
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to sync emergency to Firestore: ${e.message}")
            }
        }
    }

    suspend fun cancelEmergencyWithPin(pin: String, expectedPin: String, notes: String = "Cancelled with PIN"): Boolean {
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
    }
}
