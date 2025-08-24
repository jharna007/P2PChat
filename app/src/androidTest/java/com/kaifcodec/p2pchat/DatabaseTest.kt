package com.kaifcodec.p2pchat

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaifcodec.p2pchat.data.local.AppDatabase
import com.kaifcodec.p2pchat.data.local.dao.MessageDao
import com.kaifcodec.p2pchat.data.local.entities.Message
import com.kaifcodec.p2pchat.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        messageDao = database.messageDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveMessage() = runTest {
        val message = Message(
            id = "test-id",
            content = "Test message",
            timestamp = System.currentTimeMillis(),
            senderId = "sender-123",
            roomId = "room-ABC",
            deliveryState = Constants.MESSAGE_STATE_SENT,
            isFromMe = true
        )

        messageDao.insertMessage(message)
        val messages = messageDao.getMessagesForRoom("room-ABC").first()

        assertEquals(1, messages.size)
        assertEquals("Test message", messages[0].content)
        assertEquals("sender-123", messages[0].senderId)
        assertTrue(messages[0].isFromMe)
    }

    @Test
    fun updateMessageDeliveryState() = runTest {
        val message = Message(
            id = "test-id",
            content = "Test message",
            timestamp = System.currentTimeMillis(),
            senderId = "sender-123",
            roomId = "room-ABC",
            deliveryState = Constants.MESSAGE_STATE_SENDING,
            isFromMe = true
        )

        messageDao.insertMessage(message)
        messageDao.updateMessageDeliveryState("test-id", Constants.MESSAGE_STATE_DELIVERED)

        val messages = messageDao.getMessagesForRoom("room-ABC").first()
        assertEquals(Constants.MESSAGE_STATE_DELIVERED, messages[0].deliveryState)
    }

    @Test
    fun getMessageCountSince() = runTest {
        val baseTime = System.currentTimeMillis()
        val roomId = "room-ABC"
        val senderId = "sender-123"

        // Insert messages at different times
        val messages = listOf(
            Message("id1", "msg1", baseTime - 2000, senderId, roomId, Constants.MESSAGE_STATE_SENT, true),
            Message("id2", "msg2", baseTime - 1000, senderId, roomId, Constants.MESSAGE_STATE_SENT, true),
            Message("id3", "msg3", baseTime, senderId, roomId, Constants.MESSAGE_STATE_SENT, true)
        )

        messages.forEach { messageDao.insertMessage(it) }

        val count = messageDao.getMessageCountSince(roomId, senderId, baseTime - 1500)
        assertEquals(2, count) // Should count messages from last 1.5 seconds
    }
}