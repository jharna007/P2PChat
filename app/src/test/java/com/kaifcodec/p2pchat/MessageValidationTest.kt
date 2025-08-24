package com.kaifcodec.p2pchat

import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.isValidMessage
import org.junit.Test
import org.junit.Assert.*

class MessageValidationTest {

    @Test
    fun isValidMessage_validMessage_returnsTrue() {
        assertTrue("Hello World!".isValidMessage())
        assertTrue("A".isValidMessage())
        assertTrue("This is a valid message with spaces and punctuation.".isValidMessage())
    }

    @Test
    fun isValidMessage_emptyMessage_returnsFalse() {
        assertFalse("".isValidMessage())
        assertFalse("   ".isValidMessage()) // only whitespace
    }

    @Test
    fun isValidMessage_tooLongMessage_returnsFalse() {
        val longMessage = "x".repeat(Constants.MAX_MESSAGE_LENGTH + 1)
        assertFalse(longMessage.isValidMessage())
    }

    @Test
    fun isValidMessage_maxLengthMessage_returnsTrue() {
        val maxLengthMessage = "x".repeat(Constants.MAX_MESSAGE_LENGTH)
        assertTrue(maxLengthMessage.isValidMessage())
    }

    @Test
    fun isValidMessage_messageWithNewlines_returnsTrue() {
        val messageWithNewlines = "Line 1
Line 2
Line 3"
        assertTrue(messageWithNewlines.isValidMessage())
    }

    @Test
    fun isValidMessage_messageWithSpecialCharacters_returnsTrue() {
        val messageWithSpecial = "Hello! @user #hashtag $$money 100% 🚀"
        assertTrue(messageWithSpecial.isValidMessage())
    }

    @Test
    fun constants_messageLength_isPositive() {
        assertTrue("Max message length must be positive", Constants.MAX_MESSAGE_LENGTH > 0)
    }

    @Test
    fun constants_messagesPerMinute_isPositive() {
        assertTrue("Max messages per minute must be positive", Constants.MAX_MESSAGES_PER_MINUTE > 0)
    }
}