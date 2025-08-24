package com.kaifcodec.p2pchat.utils

object Constants {
    // Room codes
    const val ROOM_CODE_LENGTH = 6
    const val ROOM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // Message constraints
    const val MAX_MESSAGE_LENGTH = 1000
    const val MAX_MESSAGES_PER_MINUTE = 10

    // WebRTC configuration
    val ICE_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302"
    )

    // Connection timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L

    // Firestore collections
    const val ROOMS_COLLECTION = "rooms"
    const val SIGNALS_COLLECTION = "signals"
    const val METADATA_COLLECTION = "metadata"

    // Room expiry
    const val ROOM_EXPIRY_HOURS = 24L

    // Database
    const val DATABASE_NAME = "p2pchat_database"
    const val DATABASE_PASSPHRASE_KEY = "db_passphrase"

    // Signal types
    const val SIGNAL_TYPE_OFFER = "offer"
    const val SIGNAL_TYPE_ANSWER = "answer"
    const val SIGNAL_TYPE_ICE_CANDIDATE = "ice-candidate"

    // Message delivery states
    const val MESSAGE_STATE_SENDING = "sending"
    const val MESSAGE_STATE_SENT = "sent"
    const val MESSAGE_STATE_DELIVERED = "delivered"
    const val MESSAGE_STATE_FAILED = "failed"
}
