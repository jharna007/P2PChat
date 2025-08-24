package com.kaifcodec.p2pchat

import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.generateRoomCode
import com.kaifcodec.p2pchat.utils.isValidRoomCode
import org.junit.Test
import org.junit.Assert.*

class RoomCodeGeneratorTest {

    @Test
    fun generateRoomCode_hasCorrectLength() {
        val roomCode = generateRoomCode()
        assertEquals(Constants.ROOM_CODE_LENGTH, roomCode.length)
    }

    @Test
    fun generateRoomCode_containsOnlyValidCharacters() {
        val roomCode = generateRoomCode()
        assertTrue(roomCode.all { it in Constants.ROOM_CODE_CHARS })
    }

    @Test
    fun generateRoomCode_isUppercase() {
        val roomCode = generateRoomCode()
        assertEquals(roomCode, roomCode.uppercase())
    }

    @Test
    fun generateRoomCode_producesUniqueResults() {
        val codes = mutableSetOf<String>()
        repeat(100) {
            codes.add(generateRoomCode())
        }
        // Should generate mostly unique codes (allowing for rare collisions)
        assertTrue("Should generate mostly unique room codes", codes.size >= 95)
    }

    @Test
    fun isValidRoomCode_validCode_returnsTrue() {
        val validCode = "ABC123"
        assertTrue(validCode.isValidRoomCode())
    }

    @Test
    fun isValidRoomCode_invalidLength_returnsFalse() {
        assertFalse("ABCD".isValidRoomCode()) // too short
        assertFalse("ABCDEFG".isValidRoomCode()) // too long
    }

    @Test
    fun isValidRoomCode_invalidCharacters_returnsFalse() {
        assertFalse("ABC12@".isValidRoomCode()) // contains @
        assertFalse("abc123".isValidRoomCode()) // lowercase
        assertFalse("ABC 23".isValidRoomCode()) // contains space
    }

    @Test
    fun isValidRoomCode_emptyString_returnsFalse() {
        assertFalse("".isValidRoomCode())
    }
}