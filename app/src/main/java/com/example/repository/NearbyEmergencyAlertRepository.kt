package com.example.repository

import android.content.Context
import android.util.Log
import com.example.ble.nearby.NearbyBleManager
import com.example.ble.nearby.NearbyPayload
import com.example.model.MessageDeliveryState
import com.example.model.NearbyEmergencyAlert
import com.example.model.NearbyEmergencyAlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyEmergencyAlertRepository @Inject constructor(
    private val context: Context,
    private val nearbyBleManager: NearbyBleManager
) {
    private val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)

    val currentDeviceId: String
        get() {
            var id = prefs.getString("nearby_device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("nearby_device_id", id).apply()
            }
            return id
        }

    private val _alerts = MutableStateFlow<List<NearbyEmergencyAlert>>(emptyList())
    
    // Expose all received alerts
    val receivedAlerts: StateFlow<List<NearbyEmergencyAlert>> = _alerts.asStateFlow()

    // Expose active alerts that haven't expired or been dismissed
    val activeEmergencyAlerts = _alerts.map { list ->
        list.filter { !it.isDismissed && (System.currentTimeMillis() - it.receivedAt < EXPIRATION_TIME_MS) && !it.isOutgoing }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val alertLock = Any()

    companion object {
        private const val TAG = "NearbyEmergencyRepo"
        private const val EXPIRATION_TIME_MS = 2 * 60 * 60 * 1000L // 2 hours
    }

    init {
        scope.launch {
            nearbyBleManager.incomingPayloads.collect { (macAddress, payloadString) ->
                handleIncomingPayload(macAddress, payloadString)
            }
        }
        
        // Listen to connection drops and fail sending alerts
        scope.launch {
            nearbyBleManager.nearbyDevices.collect { devices ->
                synchronized(alertLock) {
                    var changed = false
                    val currentList = _alerts.value.toMutableList()
                    for (i in currentList.indices) {
                        val alert = currentList[i]
                        if (alert.isOutgoing && alert.deliveryState == MessageDeliveryState.SENDING) {
                            val mac = alert.receiverMacAddress
                            val device = if (mac != null) devices[mac] else null
                            if (device == null || device.connectionState != com.example.ble.nearby.NearbyConnectionState.CONNECTED) {
                                currentList[i] = alert.copy(deliveryState = MessageDeliveryState.FAILED)
                                changed = true
                            }
                        }
                    }
                    if (changed) {
                        _alerts.value = currentList
                    }
                }
            }
        }
    }

    private fun handleIncomingPayload(macAddress: String, payloadString: String) {
        val payload = NearbyPayload.fromJsonString(payloadString) ?: return
        if (payload.version != 1) return

        when (payload.type) {
            NearbyPayload.PayloadType.EMERGENCY -> {
                if (payload.messageId.isBlank() || payload.senderId.isBlank() || payload.payload.isBlank()) return
                
                try {
                    val emergencyJson = JSONObject(payload.payload)
                    val alertTypeStr = emergencyJson.optString("alertType", NearbyEmergencyAlertType.OTHER.name)
                    val alertType = try { NearbyEmergencyAlertType.valueOf(alertTypeStr) } catch (e: Exception) { NearbyEmergencyAlertType.OTHER }
                    val locationAvailable = emergencyJson.optBoolean("locationAvailable", false)
                    val latitude = if (emergencyJson.has("latitude") && !emergencyJson.isNull("latitude")) emergencyJson.getDouble("latitude") else null
                    val longitude = if (emergencyJson.has("longitude") && !emergencyJson.isNull("longitude")) emergencyJson.getDouble("longitude") else null
                    val accuracy = if (emergencyJson.has("accuracy") && !emergencyJson.isNull("accuracy")) emergencyJson.getDouble("accuracy").toFloat() else null
                    
                    val senderDeviceName = nearbyBleManager.nearbyDevices.value[macAddress]?.deviceName

                    var isDuplicate = false
                    synchronized(alertLock) {
                        if (_alerts.value.any { it.alertId == payload.messageId }) {
                            isDuplicate = true
                        } else {
                            val newAlert = NearbyEmergencyAlert(
                                alertId = payload.messageId,
                                senderId = payload.senderId,
                                senderDeviceName = senderDeviceName,
                                receiverMacAddress = null,
                                alertType = alertType,
                                timestamp = payload.timestamp,
                                receivedAt = System.currentTimeMillis(),
                                locationAvailable = locationAvailable,
                                latitude = latitude,
                                longitude = longitude,
                                accuracy = accuracy,
                                deliveryState = MessageDeliveryState.DELIVERED,
                                isOutgoing = false
                            )
                            _alerts.value = _alerts.value + newAlert
                        }
                    }

                    // Always send ACK back, even for duplicate (in case earlier ACK was lost)
                    sendAck(macAddress, payload.messageId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse emergency payload content", e)
                }
            }
            NearbyPayload.PayloadType.ACK -> {
                val ackedMessageId = payload.messageId
                synchronized(alertLock) {
                    val currentList = _alerts.value.toMutableList()
                    val index = currentList.indexOfFirst { it.alertId == ackedMessageId && it.isOutgoing }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(deliveryState = MessageDeliveryState.DELIVERED)
                        _alerts.value = currentList
                    }
                }
            }
            else -> {
                // Ignore TEXT and LOCATION
            }
        }
    }

    private fun sendAck(macAddress: String, messageIdToAck: String) {
        val ackPayload = NearbyPayload(
            version = 1,
            type = NearbyPayload.PayloadType.ACK,
            messageId = messageIdToAck,
            senderId = currentDeviceId,
            timestamp = System.currentTimeMillis(),
            payload = "EMERGENCY_RECEIVED"
        )
        val jsonString = ackPayload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e(TAG, "ACK payload too large, won't send")
            return
        }
        nearbyBleManager.sendNearbyPayload(macAddress, jsonString)
    }

    fun sendEmergencyAlert(
        macAddress: String,
        alertType: NearbyEmergencyAlertType,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null
    ): Boolean {
        // Check if actually connected
        val device = nearbyBleManager.nearbyDevices.value[macAddress]
        if (device?.connectionState != com.example.ble.nearby.NearbyConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send emergency alert. Device not connected.")
            return false
        }

        val newAlertId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val emergencyContent = JSONObject().apply {
            put("alertType", alertType.name)
            put("locationAvailable", latitude != null && longitude != null)
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
            if (accuracy != null) put("accuracy", accuracy)
        }

        val payload = NearbyPayload(
            version = 1,
            type = NearbyPayload.PayloadType.EMERGENCY,
            messageId = newAlertId,
            senderId = currentDeviceId,
            timestamp = timestamp,
            payload = emergencyContent.toString()
        )

        val jsonString = payload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e(TAG, "Emergency payload too large to send (Max 512 bytes)")
            return false
        }

        val newAlert = NearbyEmergencyAlert(
            alertId = newAlertId,
            senderId = currentDeviceId,
            senderDeviceName = null, // We are the sender
            receiverMacAddress = macAddress,
            alertType = alertType,
            timestamp = timestamp,
            receivedAt = timestamp,
            locationAvailable = latitude != null && longitude != null,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            deliveryState = MessageDeliveryState.SENDING,
            isOutgoing = true
        )

        synchronized(alertLock) {
            _alerts.value = _alerts.value + newAlert
        }

        val success = nearbyBleManager.sendNearbyPayload(macAddress, jsonString)

        synchronized(alertLock) {
            val currentList = _alerts.value.toMutableList()
            val index = currentList.indexOfFirst { it.alertId == newAlertId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(
                    deliveryState = if (success) MessageDeliveryState.SENT else MessageDeliveryState.FAILED
                )
                _alerts.value = currentList
            }
        }

        return success
    }

    fun dismissAlert(alertId: String) {
        synchronized(alertLock) {
            val currentList = _alerts.value.toMutableList()
            val index = currentList.indexOfFirst { it.alertId == alertId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(isDismissed = true)
                _alerts.value = currentList
            }
        }
    }
}
