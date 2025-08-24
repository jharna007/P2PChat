package com.kaifcodec.p2pchat.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.IO, block = block)
}

fun CoroutineScope.launchMain(block: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.Main, block = block)
}

fun Long.toTimeFormat(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

fun Long.toDateTimeFormat(): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

fun generateRoomCode(): String {
    return (1..Constants.ROOM_CODE_LENGTH)
        .map { Constants.ROOM_CODE_CHARSET.random() }
        .joinToString("")
}

fun generateUserId(): String {
    return UUID.randomUUID().toString()
}