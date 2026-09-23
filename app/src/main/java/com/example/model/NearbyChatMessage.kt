package com.example.model

enum class MessageDeliveryState {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

data class NearbyChatMessage(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    var deliveryState: MessageDeliveryState
)
