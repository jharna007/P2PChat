package com.kaifcodec.p2pchat.models

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING, 
    CONNECTED,
    RECONNECTING,
    FAILED
}

data class ConnectionInfo(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val roomId: String = "",
    val userId: String = "",
    val peerId: String = "",
    val lastConnectedTime: Long = 0L,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
)

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}