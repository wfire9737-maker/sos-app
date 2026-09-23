package com.example.ble.nearby

import org.json.JSONObject

/**
 * A versioned JSON payload format for Nearby BLE communication.
 * This structure supports future features such as Chat, Emergency Alerts, and Location Sharing.
 */
data class NearbyPayload(
    val version: Int = 1,
    val type: PayloadType,
    val messageId: String,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: String
) {
    enum class PayloadType {
        TEXT,
        EMERGENCY,
        LOCATION,
        ACK
    }

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("version", version)
        json.put("type", type.name)
        json.put("messageId", messageId)
        json.put("senderId", senderId)
        json.put("timestamp", timestamp)
        json.put("payload", payload)
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonString: String): NearbyPayload? {
            return try {
                val json = JSONObject(jsonString)
                val type = PayloadType.valueOf(json.getString("type"))
                NearbyPayload(
                    version = json.getInt("version"),
                    type = type,
                    messageId = json.getString("messageId"),
                    senderId = json.getString("senderId"),
                    timestamp = json.getLong("timestamp"),
                    payload = json.getString("payload")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
