package com.example.model

data class Device(
    val deviceId: String = "",
    val userId: String = "",
    val deviceName: String = "Guardian Band v1",
    val status: String = "DISCONNECTED", // CONNECTED, DISCONNECTED, ALERTing
    val batteryLevel: Int = 100,
    val macAddress: String = "00:00:00:00:00:00",
    val lastSync: Long = System.currentTimeMillis(),
    val firmwareVersion: String = "v1.2.4-esp32",
    val signalStrength: Int = -67, // dBm
    val deviceHealth: String = "EXCELLENT" // EXCELLENT, GOOD, WARNING, CRITICAL
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "deviceId" to deviceId,
            "userId" to userId,
            "deviceName" to deviceName,
            "status" to status,
            "batteryLevel" to batteryLevel,
            "macAddress" to macAddress,
            "lastSync" to lastSync,
            "firmwareVersion" to firmwareVersion,
            "signalStrength" to signalStrength,
            "deviceHealth" to deviceHealth
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Device {
            return Device(
                deviceId = map["deviceId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                deviceName = map["deviceName"] as? String ?: "Guardian Band v1",
                status = map["status"] as? String ?: "DISCONNECTED",
                batteryLevel = (map["batteryLevel"] as? Number)?.toInt() ?: 100,
                macAddress = map["macAddress"] as? String ?: "00:00:00:00:00:00",
                lastSync = (map["lastSync"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                firmwareVersion = map["firmwareVersion"] as? String ?: "v1.2.4-esp32",
                signalStrength = (map["signalStrength"] as? Number)?.toInt() ?: -67,
                deviceHealth = map["deviceHealth"] as? String ?: "EXCELLENT"
            )
        }
    }
}
