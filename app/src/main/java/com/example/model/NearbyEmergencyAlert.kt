package com.example.model

import com.example.model.MessageDeliveryState

enum class NearbyEmergencyAlertType {
    MANUAL_SOS,
    FALL_DETECTED,
    VOICE_SOS,
    OTHER
}

data class NearbyEmergencyAlert(
    val alertId: String,
    val senderId: String,
    val senderDeviceName: String?,
    val receiverMacAddress: String?, // Helpful for outgoing alerts
    val alertType: NearbyEmergencyAlertType,
    val timestamp: Long,
    val receivedAt: Long,
    val locationAvailable: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val deliveryState: MessageDeliveryState,
    val isOutgoing: Boolean,
    val isDismissed: Boolean = false
)
