package com.kaifcodec.p2pchat.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kaifcodec.p2pchat.data.local.dao.MessageDao
import com.kaifcodec.p2pchat.data.local.entities.toChatMessage
import com.kaifcodec.p2pchat.data.local.entities.toEntity
import com.kaifcodec.p2pchat.data.remote.FirebaseSignaling
import com.kaifcodec.p2pchat.models.*
import com.kaifcodec.p2pchat.utils.Logger
import com.kaifcodec.p2pchat.utils.generateUserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val messageDao: MessageDao,
    private val firebaseSignaling: FirebaseSignaling
) {

    // Local database operations
    fun getMessagesByRoom(roomId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByRoom(roomId).map { messages ->
            messages.map { it.toChatMessage() }
        }
    }

    fun getMessagesByRoomLiveData(roomId: String): LiveData<List<ChatMessage>> {
        return messageDao.getMessagesByRoomLiveData(roomId).map { messages ->
            messages.map { it.toChatMessage() }
        }
    }

    suspend fun insertMessage(message: ChatMessage, roomId: String, isLocal: Boolean) {
        try {
            val entity = message.toEntity(roomId, isLocal)
            messageDao.insertMessage(entity)
            Logger.d("Message inserted locally: ${message.id}")
        } catch (e: Exception) {
            Logger.e("Failed to insert message locally", e)
        }
    }

    suspend fun updateMessageDeliveryStatus(messageId: String, status: DeliveryStatus) {
        try {
            messageDao.updateMessageDeliveryStatus(messageId, status.name)
            Logger.d("Message delivery status updated: $messageId -> $status")
        } catch (e: Exception) {
            Logger.e("Failed to update message delivery status", e)
        }
    }

    suspend fun getLastMessage(roomId: String): ChatMessage? {
        return try {
            messageDao.getLastMessage(roomId)?.toChatMessage()
        } catch (e: Exception) {
            Logger.e("Failed to get last message", e)
            null
        }
    }

    suspend fun deleteMessagesByRoom(roomId: String) {
        try {
            messageDao.deleteMessagesByRoom(roomId)
            Logger.d("Messages deleted for room: $roomId")
        } catch (e: Exception) {
            Logger.e("Failed to delete messages for room", e)
        }
    }

    // Firebase signaling operations
    suspend fun createRoom(roomId: String, userId: String): Boolean {
        return firebaseSignaling.createRoom(roomId, userId)
    }

    suspend fun joinRoom(roomId: String, userId: String): Boolean {
        return firebaseSignaling.joinRoom(roomId, userId)
    }

    suspend fun leaveRoom(roomId: String, userId: String): Boolean {
        return firebaseSignaling.leaveRoom(roomId, userId)
    }

    suspend fun sendSignal(roomId: String, signalData: SignalData): Boolean {
        return firebaseSignaling.sendSignal(roomId, signalData)
    }

    fun listenForSignals(roomId: String, userId: String): Flow<SignalData> {
        return firebaseSignaling.listenForSignals(roomId, userId)
    }

    suspend fun isRoomActive(roomId: String): Boolean {
        return firebaseSignaling.isRoomActive(roomId)
    }

    suspend fun cleanupExpiredRooms() {
        firebaseSignaling.cleanupExpiredRooms()
    }

    // Helper methods
    fun generateMessageId(): String = UUID.randomUUID().toString()

    fun generateUserId(): String = generateUserId()

    suspend fun getMessageCount(roomId: String): Int {
        return try {
            messageDao.getMessageCount(roomId)
        } catch (e: Exception) {
            Logger.e("Failed to get message count", e)
            0
        }
    }
}