package com.example.repository

import android.content.Context
import android.util.Log
import com.example.ble.nearby.NearbyBleManager
import com.example.ble.nearby.NearbyPayload
import com.example.model.NearbyLocationUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyLocationRepository @Inject constructor(
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

    private val _latestLocations = MutableStateFlow<Map<String, NearbyLocationUpdate>>(emptyMap())
    val latestLocations: StateFlow<Map<String, NearbyLocationUpdate>> = _latestLocations.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationLock = Any()
    
    // To protect against duplicate processing (storing recent incoming message IDs)
    private val processedMessageIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "NearbyLocationRepo"
    }

    init {
        scope.launch {
            nearbyBleManager.incomingPayloads.collect { (macAddress, payloadString) ->
                handleIncomingPayload(macAddress, payloadString)
            }
        }
    }

    private fun handleIncomingPayload(macAddress: String, payloadString: String) {
        val payload = NearbyPayload.fromJsonString(payloadString) ?: return
        if (payload.version != 1) return

        if (payload.type == NearbyPayload.PayloadType.LOCATION) {
            if (payload.messageId.isBlank() || payload.senderId.isBlank() || payload.payload.isBlank()) return

            try {
                val locationJson = JSONObject(payload.payload)
                
                if (!locationJson.has("latitude") || !locationJson.has("longitude")) {
                    Log.w(TAG, "Location payload missing lat/lng")
                    return
                }

                val latitude = locationJson.getDouble("latitude")
                val longitude = locationJson.getDouble("longitude")
                
                // Validate coordinates
                if (latitude.isNaN() || latitude.isInfinite() || latitude < -90.0 || latitude > 90.0) return
                if (longitude.isNaN() || longitude.isInfinite() || longitude < -180.0 || longitude > 180.0) return

                val accuracy = if (locationJson.has("accuracy") && !locationJson.isNull("accuracy")) {
                    val acc = locationJson.getDouble("accuracy").toFloat()
                    if (acc.isNaN() || acc.isInfinite() || acc < 0f) null else acc
                } else null

                val senderDeviceName = nearbyBleManager.nearbyDevices.value[macAddress]?.deviceName

                synchronized(locationLock) {
                    if (processedMessageIds.contains(payload.messageId)) {
                        // Already processed this message, just send ACK (in case previous ACK dropped)
                        sendAck(macAddress, payload.messageId)
                        return
                    }

                    processedMessageIds.add(payload.messageId)
                    // Keep memory bounded
                    if (processedMessageIds.size > 1000) {
                        val iterator = processedMessageIds.iterator()
                        for (i in 0 until 100) {
                            if (iterator.hasNext()) {
                                iterator.next()
                                iterator.remove()
                            }
                        }
                    }

                    val update = NearbyLocationUpdate(
                        messageId = payload.messageId,
                        senderId = payload.senderId,
                        senderDeviceName = senderDeviceName,
                        latitude = latitude,
                        longitude = longitude,
                        accuracyMeters = accuracy,
                        timestamp = payload.timestamp,
                        receivedAt = System.currentTimeMillis(),
                        isOutgoing = false
                    )

                    // Retain only latest by senderId
                    val newMap = _latestLocations.value.toMutableMap()
                    
                    val existing = newMap[payload.senderId]
                    if (existing == null || payload.timestamp >= existing.timestamp) {
                        newMap[payload.senderId] = update
                        _latestLocations.value = newMap
                    }
                }

                sendAck(macAddress, payload.messageId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse location payload content", e)
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
            payload = "LOCATION_RECEIVED"
        )
        val jsonString = ackPayload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e(TAG, "ACK payload too large, won't send")
            return
        }
        nearbyBleManager.sendNearbyPayload(macAddress, jsonString)
    }

    fun sendLocation(
        macAddress: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?
    ): Boolean {
        // Validate
        if (latitude.isNaN() || latitude.isInfinite() || latitude < -90.0 || latitude > 90.0) return false
        if (longitude.isNaN() || longitude.isInfinite() || longitude < -180.0 || longitude > 180.0) return false
        
        val validAccuracy = if (accuracyMeters != null) {
            if (accuracyMeters.isNaN() || accuracyMeters.isInfinite() || accuracyMeters < 0f) null else accuracyMeters
        } else null

        // Check connection
        val device = nearbyBleManager.nearbyDevices.value[macAddress]
        if (device?.connectionState != com.example.ble.nearby.NearbyConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send location. Device not connected.")
            return false
        }

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val locationContent = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            if (validAccuracy != null) {
                put("accuracy", validAccuracy)
            }
        }

        val payload = NearbyPayload(
            version = 1,
            type = NearbyPayload.PayloadType.LOCATION,
            messageId = messageId,
            senderId = currentDeviceId,
            timestamp = timestamp,
            payload = locationContent.toString()
        )

        val jsonString = payload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e(TAG, "Location payload too large to send (Max 512 bytes)")
            return false
        }

        // Send payload
        val success = nearbyBleManager.sendNearbyPayload(macAddress, jsonString)
        
        if (success) {
            val update = NearbyLocationUpdate(
                messageId = messageId,
                senderId = currentDeviceId,
                senderDeviceName = null, // it's us
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = validAccuracy,
                timestamp = timestamp,
                receivedAt = timestamp,
                isOutgoing = true
            )
            
            synchronized(locationLock) {
                val newMap = _latestLocations.value.toMutableMap()
                newMap[currentDeviceId] = update
                _latestLocations.value = newMap
            }
        }

        return success
    }
}
