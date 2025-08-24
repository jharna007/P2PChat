package com.kaifcodec.p2pchat.models

data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long,
    val senderId: String,
    val roomId: String,
    val deliveryState: String,
    val isFromMe: Boolean = false
)
