package com.kaifcodec.p2pchat.data.repository

import com.kaifcodec.p2pchat.data.local.dao.MessageDao
import com.kaifcodec.p2pchat.data.remote.FirebaseSignaling
import com.kaifcodec.p2pchat.models.ChatMessage
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import com.kaifcodec.p2pchat.utils.toChatMessage
import com.kaifcodec.p2pchat.utils.toMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class ChatRepository(
    private val messageDao: MessageDao,
    private val firebaseSignaling: FirebaseSignaling
) {

    suspend fun insertMessage(message: ChatMessage) {
        try {
            messageDao.insertMessage(message.toMessage())
            Logger.d("Message inserted locally: ${message.id}")
        } catch (e: Exception) {
            Logger.e("Failed to insert message", e)
        }
    }

    suspend fun updateMessageDeliveryState(messageId: String, deliveryState: String) {
        try {
            messageDao.updateMessageDeliveryState(messageId, deliveryState)
            Logger.d("Message delivery state updated: $messageId -> $deliveryState")
        } catch (e: Exception) {
            Logger.e("Failed to update message delivery state", e)
        }
    }

    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForRoom(roomId).map { messages ->
            messages.map { it.toChatMessage() }
        }
    }

    suspend fun getRecentRooms(): List<String> {
        return try {
            messageDao.getRecentRooms()
        } catch (e: Exception) {
            Logger.e("Failed to get recent rooms", e)
            emptyList()
        }
    }

    suspend fun createRoom(roomId: String): Boolean {
        return firebaseSignaling.createRoom(roomId)
    }

    suspend fun joinRoom(roomId: String): Boolean {
        return firebaseSignaling.joinRoom(roomId)
    }

    suspend fun leaveRoom(roomId: String): Boolean {
        return firebaseSignaling.leaveRoom(roomId)
    }

    suspend fun sendSignal(roomId: String, userId: String, signalData: SignalData): Boolean {
        return firebaseSignaling.sendSignal(roomId, userId, signalData)
    }

    fun listenForSignals(roomId: String, userId: String): Flow<SignalData> {
        return firebaseSignaling.listenForSignals(roomId, userId)
    }

    suspend fun canSendMessage(roomId: String, userId: String): Boolean {
        return try {
            val oneMinuteAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1)
            val messageCount = messageDao.getMessageCountSince(roomId, userId, oneMinuteAgo)
            messageCount < Constants.MAX_MESSAGES_PER_MINUTE
        } catch (e: Exception) {
            Logger.e("Failed to check message rate limit", e)
            false
        }
    }

    suspend fun clearMessagesForRoom(roomId: String) {
        try {
            messageDao.deleteMessagesForRoom(roomId)
            Logger.d("Messages cleared for room: $roomId")
        } catch (e: Exception) {
            Logger.e("Failed to clear messages for room", e)
        }
    }

    fun stopListening() {
        firebaseSignaling.stopListening()
    }
}
