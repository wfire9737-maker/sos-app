package com.example.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val medicalInfo: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val role: String = "User", // e.g. User, Responder, Dispatcher
    val createdAt: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val bloodType: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "medicalInfo" to medicalInfo,
            "emergencyContactName" to emergencyContactName,
            "emergencyContactPhone" to emergencyContactPhone,
            "role" to role,
            "createdAt" to createdAt,
            "photoUri" to photoUri,
            "bloodType" to bloodType,
            "allergies" to allergies,
            "conditions" to conditions,
            "medications" to medications
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                uid = map["uid"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                medicalInfo = map["medicalInfo"] as? String ?: "",
                emergencyContactName = map["emergencyContactName"] as? String ?: "",
                emergencyContactPhone = map["emergencyContactPhone"] as? String ?: "",
                role = map["role"] as? String ?: "User",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                photoUri = map["photoUri"] as? String,
                bloodType = map["bloodType"] as? String ?: "",
                allergies = map["allergies"] as? String ?: "",
                conditions = map["conditions"] as? String ?: "",
                medications = map["medications"] as? String ?: ""
            )
        }
    }
}
