package com.example.model

data class TrustedPlace(
    val placeId: String = "",
    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 100.0, // meters
    val createdDate: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    // Configurations
    val alwaysSendSos: Boolean = true,
    val reduceNotificationSound: Boolean = false,
    val skipAutomaticPhoneCall: Boolean = false,
    val delaySosSeconds: Int = 0,
    val showConfirmationDialog: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "placeId" to placeId,
            "userId" to userId,
            "name" to name,
            "address" to address,
            "latitude" to latitude,
            "longitude" to longitude,
            "radius" to radius,
            "createdDate" to createdDate,
            "lastUpdated" to lastUpdated,
            "alwaysSendSos" to alwaysSendSos,
            "reduceNotificationSound" to reduceNotificationSound,
            "skipAutomaticPhoneCall" to skipAutomaticPhoneCall,
            "delaySosSeconds" to delaySosSeconds,
            "showConfirmationDialog" to showConfirmationDialog
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): TrustedPlace {
            return TrustedPlace(
                placeId = map["placeId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                address = map["address"] as? String ?: "",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                radius = (map["radius"] as? Number)?.toDouble() ?: 100.0,
                createdDate = (map["createdDate"] as? Number)?.toLong() ?: 0L,
                lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: 0L,
                alwaysSendSos = map["alwaysSendSos"] as? Boolean ?: true,
                reduceNotificationSound = map["reduceNotificationSound"] as? Boolean ?: false,
                skipAutomaticPhoneCall = map["skipAutomaticPhoneCall"] as? Boolean ?: false,
                delaySosSeconds = (map["delaySosSeconds"] as? Number)?.toInt() ?: 0,
                showConfirmationDialog = map["showConfirmationDialog"] as? Boolean ?: false
            )
        }
    }
}
