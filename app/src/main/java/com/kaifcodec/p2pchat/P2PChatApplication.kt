package com.kaifcodec.p2pchat

import android.app.Application
import com.kaifcodec.p2pchat.data.local.AppDatabase
import com.kaifcodec.p2pchat.data.remote.FirebaseSignaling
import com.kaifcodec.p2pchat.data.repository.ChatRepository
import com.kaifcodec.p2pchat.utils.Logger

class P2PChatApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val firebaseSignaling by lazy { FirebaseSignaling() }
    val repository by lazy { ChatRepository(database.messageDao(), firebaseSignaling) }

    override fun onCreate() {
        super.onCreate()
        Logger.d("P2PChat Application started")
    }

    override fun onTerminate() {
        super.onTerminate()
        repository.stopListening()
        Logger.d("P2PChat Application terminated")
    }
}
