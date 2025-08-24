package com.kaifcodec.p2pchat.models

import com.google.gson.Gson

data class SignalData(
    val type: SignalType = SignalType.OFFER,
    val data: String = "",
    val senderId: String = "",
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val roomId: String = ""
)

enum class SignalType {
    OFFER, ANSWER, ICE_CANDIDATE, ROOM_JOIN, ROOM_LEAVE, HEARTBEAT
}

fun SignalData.toJson(): String = Gson().toJson(this)

fun String.toSignalData(): SignalData? {
    return try {
        Gson().fromJson(this, SignalData::class.java)
    } catch (e: Exception) {
        null
    }
}