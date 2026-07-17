package com.example.model

import org.json.JSONObject

data class EmergencyContact(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val priority: Int = 1, // 1 = High/Primary, 2 = Medium/Secondary, 3 = Low
    val notes: String = "",
    val avatarEmoji: String = "👤"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "phone" to phone,
            "relationship" to relationship,
            "priority" to priority,
            "notes" to notes,
            "avatarEmoji" to avatarEmoji
        )
    }

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("userId", userId)
        obj.put("name", name)
        obj.put("phone", phone)
        obj.put("relationship", relationship)
        obj.put("priority", priority)
        obj.put("notes", notes)
        obj.put("avatarEmoji", avatarEmoji)
        return obj
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): EmergencyContact {
            return EmergencyContact(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                relationship = map["relationship"] as? String ?: "",
                priority = (map["priority"] as? Number)?.toInt() ?: 1,
                notes = map["notes"] as? String ?: "",
                avatarEmoji = map["avatarEmoji"] as? String ?: "👤"
            )
        }

        fun fromJsonObject(obj: JSONObject): EmergencyContact {
            return EmergencyContact(
                id = obj.optString("id"),
                userId = obj.optString("userId"),
                name = obj.optString("name"),
                phone = obj.optString("phone"),
                relationship = obj.optString("relationship"),
                priority = obj.optInt("priority", 1),
                notes = obj.optString("notes"),
                avatarEmoji = obj.optString("avatarEmoji", "👤")
            )
        }
    }
}
