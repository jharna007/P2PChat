package com.kaifcodec.p2pchat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "messages",
    indices = [Index(value = ["roomId", "timestamp"])]
)
data class Message(
    @PrimaryKey
    val id: String,
    val content: String,
    val timestamp: Long,
    val senderId: String,
    val roomId: String,
    val deliveryState: String,
    val isFromMe: Boolean = false
)
