package com.kaifcodec.p2pchat.utils

object Constants {

    // Firebase Collections
    const val ROOMS_COLLECTION = "rooms"
    const val SIGNALS_COLLECTION = "signals" 
    const val METADATA_COLLECTION = "metadata"

    // Room expiry time (24 hours in milliseconds)
    const val ROOM_EXPIRY_TIME = 24 * 60 * 60 * 1000L

    // WebRTC Configuration
    val STUN_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302"
    )

    // Connection timeouts
    const val CONNECTION_TIMEOUT = 30000L // 30 seconds
    const val RECONNECTION_TIMEOUT = 5000L // 5 seconds

    // Message limits
    const val MAX_MESSAGE_LENGTH = 1000
    const val MAX_MESSAGES_PER_MINUTE = 10

    // Room code configuration
    const val ROOM_CODE_LENGTH = 6
    const val ROOM_CODE_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // Intent extras
    const val EXTRA_ROOM_ID = "room_id"
    const val EXTRA_IS_CALLER = "is_caller"
    const val EXTRA_USER_ID = "user_id"

    // Shared preferences
    const val PREF_NAME = "p2pchat_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USERNAME = "username"
}