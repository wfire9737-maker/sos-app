package com.example.model

data class NearbyLocationUpdate(
    val messageId: String,
    val senderId: String,
    val senderDeviceName: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestamp: Long,
    val receivedAt: Long,
    val isOutgoing: Boolean
)
