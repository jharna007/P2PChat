package com.kaifcodec.p2pchat

import com.google.gson.Gson
import com.kaifcodec.p2pchat.models.IceCandidateData
import com.kaifcodec.p2pchat.models.SessionDescriptionData
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.utils.Constants
import org.junit.Test
import org.junit.Assert.*

class SignalDataTest {

    private val gson = Gson()

    @Test
    fun signalData_serialization_isCorrect() {
        val signalData = SignalData(
            type = Constants.SIGNAL_TYPE_OFFER,
            data = mapOf("test" to "value"),
            timestamp = 1234567890L,
            userId = "user123"
        )

        val json = gson.toJson(signalData)
        assertNotNull(json)
        assertTrue(json.contains("offer"))
        assertTrue(json.contains("user123"))
        assertTrue(json.contains("1234567890"))
    }

    @Test
    fun signalData_deserialization_isCorrect() {
        val json = """{"type": "offer", "data": {"test": "value"}, "timestamp": 1234567890, "userId": "user123"}"""

        val signalData = gson.fromJson(json, SignalData::class.java)
        assertEquals(Constants.SIGNAL_TYPE_OFFER, signalData.type)
        assertEquals("user123", signalData.userId)
        assertEquals(1234567890L, signalData.timestamp)
        assertEquals("value", signalData.data["test"])
    }

    @Test
    fun iceCandidateData_serialization_isCorrect() {
        val candidateData = IceCandidateData(
            candidate = "candidate:1 1 UDP 2130706431 192.168.1.1 54400 typ host",
            sdpMid = "data",
            sdpMLineIndex = 0
        )

        val json = gson.toJson(candidateData)
        assertNotNull(json)
        assertTrue(json.contains("candidate:1"))
        assertTrue(json.contains("data"))
        assertTrue(json.contains("0"))
    }

    @Test
    fun sessionDescriptionData_serialization_isCorrect() {
        val sdpData = SessionDescriptionData(
            type = "offer",
            sdp = "v=0
o=- 123 456 IN IP4 192.168.1.1
"
        )

        val json = gson.toJson(sdpData)
        assertNotNull(json)
        assertTrue(json.contains("offer"))
        assertTrue(json.contains("v=0"))
    }

    @Test
    fun signalData_hasDefaultTimestamp() {
        val signalData = SignalData(
            type = Constants.SIGNAL_TYPE_ICE_CANDIDATE,
            data = emptyMap(),
            userId = "user123"
        )

        // Check that timestamp is set to current time (within reasonable range)
        val now = System.currentTimeMillis()
        assertTrue("Timestamp should be recent", signalData.timestamp <= now)
        assertTrue("Timestamp should be recent", signalData.timestamp >= now - 1000)
    }
}