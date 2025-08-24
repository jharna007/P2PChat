package com.kaifcodec.p2pchat.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.kaifcodec.p2pchat.data.local.entities.Message
import com.kaifcodec.p2pchat.models.ChatMessage
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

fun String.generateRoomCode(): String {
    val random = SecureRandom()
    return (1..Constants.ROOM_CODE_LENGTH)
        .map { Constants.ROOM_CODE_CHARS[random.nextInt(Constants.ROOM_CODE_CHARS.length)] }
        .joinToString("")
}

fun generateRoomCode(): String {
    val random = SecureRandom()
    return (1..Constants.ROOM_CODE_LENGTH)
        .map { Constants.ROOM_CODE_CHARS[random.nextInt(Constants.ROOM_CODE_CHARS.length)] }
        .joinToString("")
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.copyToClipboard(text: String, label: String = "Copied Text") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    showToast("Copied to clipboard")
}

fun Long.formatTimestamp(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

fun String.isValidRoomCode(): Boolean {
    return length == Constants.ROOM_CODE_LENGTH && all { it in Constants.ROOM_CODE_CHARS }
}

fun String.isValidMessage(): Boolean {
    return isNotBlank() && length <= Constants.MAX_MESSAGE_LENGTH
}

fun Message.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        timestamp = timestamp,
        senderId = senderId,
        roomId = roomId,
        deliveryState = deliveryState,
        isFromMe = isFromMe
    )
}

fun ChatMessage.toMessage(): Message {
    return Message(
        id = id,
        content = content,
        timestamp = timestamp,
        senderId = senderId,
        roomId = roomId,
        deliveryState = deliveryState,
        isFromMe = isFromMe
    )
}

fun generateUserId(): String {
    return UUID.randomUUID().toString()
}

fun generateMessageId(): String {
    return UUID.randomUUID().toString()
}
