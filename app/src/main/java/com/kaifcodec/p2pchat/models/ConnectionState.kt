package com.kaifcodec.p2pchat.models

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}
