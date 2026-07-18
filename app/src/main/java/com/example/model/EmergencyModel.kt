package com.example.model

data class EmergencyModel(
    val emergencyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "GPS Coordinate Plot",
    val status: String = "ACTIVE", // ACTIVE, MARKED_SAFE, CANCELLED, RESOLVED
    val triggerType: String = "MANUAL", // MANUAL, ESP32_BUTTON, FALL_DETECTED
    val aiConfidenceScore: Int = 90,
    val contactsNotified: List<String> = emptyList(),
    val responderStatus: String = "DISPATCHING FIRST RESPONDERS",
    val notes: String = "",
    val deviceId: String = "ESP32-SOS-BAND-81F4"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "emergencyId" to emergencyId,
            "userId" to userId,
            "userName" to userName,
            "userPhone" to userPhone,
            "startTimeMs" to startTimeMs,
            "endTimeMs" to endTimeMs,
            "latitude" to latitude,
            "longitude" to longitude,
            "locationName" to locationName,
            "status" to status,
            "triggerType" to triggerType,
            "aiConfidenceScore" to aiConfidenceScore,
            "contactsNotified" to contactsNotified,
            "responderStatus" to responderStatus,
            "notes" to notes,
            "deviceId" to deviceId
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): EmergencyModel {
            return EmergencyModel(
                emergencyId = map["emergencyId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userPhone = map["userPhone"] as? String ?: "",
                startTimeMs = (map["startTimeMs"] as? Number)?.toLong() ?: 0L,
                endTimeMs = (map["endTimeMs"] as? Number)?.toLong(),
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                locationName = map["locationName"] as? String ?: "GPS Coordinate Plot",
                status = map["status"] as? String ?: "ACTIVE",
                triggerType = map["triggerType"] as? String ?: "MANUAL",
                aiConfidenceScore = (map["aiConfidenceScore"] as? Number)?.toInt() ?: 90,
                contactsNotified = (map["contactsNotified"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                responderStatus = map["responderStatus"] as? String ?: "DISPATCHING FIRST RESPONDERS",
                notes = map["notes"] as? String ?: "",
                deviceId = map["deviceId"] as? String ?: "ESP32-SOS-BAND-81F4"
            )
        }
    }
}
