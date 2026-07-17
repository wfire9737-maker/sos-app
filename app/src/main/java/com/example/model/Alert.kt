package com.example.model

data class Alert(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, RESOLVED
    val triggerType: String = "MANUAL", // MANUAL, ESP32_BUTTON, FALL_DETECTED
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedAt: Long = 0L,
    val resolvedBy: String = "",
    val notes: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userPhone" to userPhone,
            "latitude" to latitude,
            "longitude" to longitude,
            "status" to status,
            "triggerType" to triggerType,
            "timestamp" to timestamp,
            "resolvedAt" to resolvedAt,
            "resolvedBy" to resolvedBy,
            "notes" to notes
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Alert {
            return Alert(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userPhone = map["userPhone"] as? String ?: "",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                status = map["status"] as? String ?: "ACTIVE",
                triggerType = map["triggerType"] as? String ?: "MANUAL",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                resolvedAt = (map["resolvedAt"] as? Number)?.toLong() ?: 0L,
                resolvedBy = map["resolvedBy"] as? String ?: "",
                notes = map["notes"] as? String ?: ""
            )
        }
    }
}
