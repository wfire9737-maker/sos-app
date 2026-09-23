package com.example.repository

import android.content.Context
import android.util.Log
import com.example.ble.nearby.NearbyBleManager
import com.example.ble.nearby.NearbyPayload
import com.example.model.MessageDeliveryState
import com.example.model.NearbyChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyChatRepository @Inject constructor(
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
        
    private val _messages = MutableStateFlow<List<NearbyChatMessage>>(emptyList())
    val messages: StateFlow<List<NearbyChatMessage>> = _messages.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val messageLock = Any()
    
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
        
        when (payload.type) {
            NearbyPayload.PayloadType.TEXT -> {
                if (payload.messageId.isBlank() || payload.senderId.isBlank() || payload.payload.isBlank()) return
                
                // Note on limits: The Nearby transport enforces a MAX_PAYLOAD_SIZE of 512 bytes on send.
                // We don't strictly re-verify byte size on receipt (as it already arrived),
                // but any further processing expects valid strings.
                var isDuplicate = false
                synchronized(messageLock) {
                    if (_messages.value.any { it.messageId == payload.messageId }) {
                        isDuplicate = true
                    } else {
                        val newMsg = NearbyChatMessage(
                            messageId = payload.messageId,
                            senderId = payload.senderId,
                            receiverId = currentDeviceId,
                            text = payload.payload.trim(),
                            timestamp = payload.timestamp,
                            isOutgoing = false,
                            deliveryState = MessageDeliveryState.DELIVERED
                        )
                        _messages.value = _messages.value + newMsg
                    }
                }
                
                // Always send ACK back, even for duplicate (in case earlier ACK was lost)
                sendAck(macAddress, payload.messageId)
            }
            NearbyPayload.PayloadType.ACK -> {
                val ackedMessageId = payload.messageId
                synchronized(messageLock) {
                    val currentList = _messages.value.toMutableList()
                    val index = currentList.indexOfFirst { it.messageId == ackedMessageId && it.isOutgoing }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(deliveryState = MessageDeliveryState.DELIVERED)
                        _messages.value = currentList
                    }
                }
            }
            else -> {
                // Ignore EMERGENCY, LOCATION etc for now
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
            payload = "ACK"
        )
        val jsonString = ackPayload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e("NearbyChatRepository", "ACK payload too large, won't send")
            return
        }
        
        nearbyBleManager.sendNearbyPayload(macAddress, jsonString)
    }

    fun sendText(macAddress: String, text: String): Boolean {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return false
        
        val newMsgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val payload = NearbyPayload(
            version = 1,
            type = NearbyPayload.PayloadType.TEXT,
            messageId = newMsgId,
            senderId = currentDeviceId,
            timestamp = timestamp,
            payload = trimmedText
        )
        
        val jsonString = payload.toJsonString()
        if (jsonString.toByteArray(Charsets.UTF_8).size > 512) {
            Log.e("NearbyChatRepository", "Payload too large to send (Max 512 bytes)")
            return false
        }
        
        val newMsg = NearbyChatMessage(
            messageId = newMsgId,
            senderId = currentDeviceId,
            receiverId = macAddress,
            text = trimmedText,
            timestamp = timestamp,
            isOutgoing = true,
            deliveryState = MessageDeliveryState.SENDING
        )
        
        synchronized(messageLock) {
            _messages.value = _messages.value + newMsg
        }
        
        // sendNearbyPayload enforces that the device is connected
        val success = nearbyBleManager.sendNearbyPayload(macAddress, jsonString)
        
        synchronized(messageLock) {
            val currentList = _messages.value.toMutableList()
            val index = currentList.indexOfFirst { it.messageId == newMsgId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(
                    deliveryState = if (success) MessageDeliveryState.SENT else MessageDeliveryState.FAILED
                )
                _messages.value = currentList
            }
        }
        
        return success
    }
    
    fun clearMessages() {
        synchronized(messageLock) {
            _messages.value = emptyList()
        }
    }
}
