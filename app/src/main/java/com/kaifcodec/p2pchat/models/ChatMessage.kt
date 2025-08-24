package com.kaifcodec.p2pchat.models

data class ChatMessage(
    val id: String = "",
    val content: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENDING
)

enum class MessageType {
    TEXT, IMAGE, FILE
}

enum class DeliveryStatus {
    SENDING, SENT, DELIVERED, FAILED
}