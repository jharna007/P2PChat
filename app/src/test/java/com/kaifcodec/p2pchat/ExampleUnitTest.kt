package com.kaifcodec.p2pchat

import com.kaifcodec.p2pchat.utils.generateRoomCode
import com.kaifcodec.p2pchat.utils.Constants
import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun generateRoomCode_returnsCorrectLength() {
        val roomCode = generateRoomCode()
        assertEquals(Constants.ROOM_CODE_LENGTH, roomCode.length)
    }

    @Test
    fun generateRoomCode_containsValidCharacters() {
        val roomCode = generateRoomCode()
        assertTrue(roomCode.all { it in Constants.ROOM_CODE_CHARSET })
    }

    @Test
    fun generateRoomCode_isUppercase() {
        val roomCode = generateRoomCode()
        assertEquals(roomCode.uppercase(), roomCode)
    }
}