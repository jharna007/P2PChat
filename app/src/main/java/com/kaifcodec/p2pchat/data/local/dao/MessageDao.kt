package com.kaifcodec.p2pchat.data.local.dao

import androidx.room.*
import com.kaifcodec.p2pchat.data.local.entities.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("UPDATE messages SET deliveryState = :deliveryState WHERE id = :messageId")
    suspend fun updateMessageDeliveryState(messageId: String, deliveryState: String)

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun deleteMessagesForRoom(roomId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE roomId = :roomId AND senderId = :senderId AND timestamp > :since")
    suspend fun getMessageCountSince(roomId: String, senderId: String, since: Long): Int

    @Query("SELECT DISTINCT roomId FROM messages ORDER BY MAX(timestamp) DESC")
    suspend fun getRecentRooms(): List<String>
}
