package com.example.model

data class Device(
    val deviceId: String = "",
    val userId: String = "",
    val deviceName: String = "Guardian Band v1",
    val status: String = "DISCONNECTED", // CONNECTED, DISCONNECTED, ALERTing, REBOOTING
    val batteryLevel: Int = 100,
    val macAddress: String = "00:00:00:00:00:00",
    val lastSync: Long = System.currentTimeMillis(),
    val firmwareVersion: String = "v1.2.4-esp32",
    val signalStrength: Int = -67, // dBm
    val deviceHealth: String = "EXCELLENT", // EXCELLENT, GOOD, WARNING, CRITICAL
    
    // NEW MODULE 11 REQUIREMENTS:
    val isCharging: Boolean = false,
    val wifiSignal: Int = -55, // dBm
    val bluetoothStatus: String = "CONNECTED", // CONNECTED, DISCONNECTED, PAIRING
    val gpsStatus: String = "LOCKED", // LOCKED, SEARCHING, OFF
    val deviceTemperature: Float = 36.5f,
    val uptimeSeconds: Long = 3600L,
    val memoryUsagePercent: Int = 42,
    val cpuUsagePercent: Int = 18,
    val healthScore: Int = 98,
    val connectionStatus: String = "ONLINE", // ONLINE, OFFLINE, STANDBY
    
    // GPS & MPU6050 telemetries
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val accelX: Float = 0.05f,
    val accelY: Float = -0.02f,
    val accelZ: Float = 0.98f,
    val gyroX: Float = 0.1f,
    val gyroY: Float = -0.1f,
    val gyroZ: Float = 0.2f
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
            "deviceHealth" to deviceHealth,
            "isCharging" to isCharging,
            "wifiSignal" to wifiSignal,
            "bluetoothStatus" to bluetoothStatus,
            "gpsStatus" to gpsStatus,
            "deviceTemperature" to deviceTemperature,
            "uptimeSeconds" to uptimeSeconds,
            "memoryUsagePercent" to memoryUsagePercent,
            "cpuUsagePercent" to cpuUsagePercent,
            "healthScore" to healthScore,
            "connectionStatus" to connectionStatus,
            "latitude" to latitude,
            "longitude" to longitude,
            "accelX" to accelX,
            "accelY" to accelY,
            "accelZ" to accelZ,
            "gyroX" to gyroX,
            "gyroY" to gyroY,
            "gyroZ" to gyroZ
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
                deviceHealth = map["deviceHealth"] as? String ?: "EXCELLENT",
                isCharging = map["isCharging"] as? Boolean ?: false,
                wifiSignal = (map["wifiSignal"] as? Number)?.toInt() ?: -55,
                bluetoothStatus = map["bluetoothStatus"] as? String ?: "CONNECTED",
                gpsStatus = map["gpsStatus"] as? String ?: "LOCKED",
                deviceTemperature = (map["deviceTemperature"] as? Number)?.toFloat() ?: 36.5f,
                uptimeSeconds = (map["uptimeSeconds"] as? Number)?.toLong() ?: 3600L,
                memoryUsagePercent = (map["memoryUsagePercent"] as? Number)?.toInt() ?: 42,
                cpuUsagePercent = (map["cpuUsagePercent"] as? Number)?.toInt() ?: 18,
                healthScore = (map["healthScore"] as? Number)?.toInt() ?: 98,
                connectionStatus = map["connectionStatus"] as? String ?: "ONLINE",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 37.7749,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: -122.4194,
                accelX = (map["accelX"] as? Number)?.toFloat() ?: 0.05f,
                accelY = (map["accelY"] as? Number)?.toFloat() ?: -0.02f,
                accelZ = (map["accelZ"] as? Number)?.toFloat() ?: 0.98f,
                gyroX = (map["gyroX"] as? Number)?.toFloat() ?: 0.1f,
                gyroY = (map["gyroY"] as? Number)?.toFloat() ?: -0.1f,
                gyroZ = (map["gyroZ"] as? Number)?.toFloat() ?: 0.2f
            )
        }
    }
}
