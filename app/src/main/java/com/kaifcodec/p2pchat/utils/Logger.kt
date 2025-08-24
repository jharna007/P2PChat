package com.kaifcodec.p2pchat.utils

import android.util.Log

object Logger {
    private const val TAG = "P2PChat"
    private const val DEBUG = true

    fun d(message: String, tag: String = TAG) {
        if (DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (DEBUG) {
            Log.e(tag, message, throwable)
        }
    }

    fun i(message: String, tag: String = TAG) {
        if (DEBUG) {
            Log.i(tag, message)
        }
    }

    fun w(message: String, tag: String = TAG) {
        if (DEBUG) {
            Log.w(tag, message)
        }
    }
}
