package com.kaifcodec.p2pchat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaifcodec.p2pchat.models.DeliveryStatus
import com.kaifcodec.p2pchat.models.MessageType

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val roomId: String,
    val timestamp: Long,
    val messageType: String, // Store as string for Room compatibility
    val deliveryStatus: String, // Store as string for Room compatibility
    val isLocalMessage: Boolean
) {
    fun toMessageType(): MessageType = MessageType.valueOf(messageType)
    fun toDeliveryStatus(): DeliveryStatus = DeliveryStatus.valueOf(deliveryStatus)
}

fun Message.toChatMessage() = com.kaifcodec.p2pchat.models.ChatMessage(
    id = id,
    content = content,
    senderId = senderId,
    senderName = senderName,
    timestamp = timestamp,
    messageType = toMessageType(),
    deliveryStatus = toDeliveryStatus()
)

fun com.kaifcodec.p2pchat.models.ChatMessage.toEntity(roomId: String, isLocal: Boolean) = Message(
    id = id,
    content = content,
    senderId = senderId,
    senderName = senderName,
    roomId = roomId,
    timestamp = timestamp,
    messageType = messageType.name,
    deliveryStatus = deliveryStatus.name,
    isLocalMessage = isLocal
)