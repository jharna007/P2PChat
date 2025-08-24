package com.kaifcodec.p2pchat

import android.app.Application
import org.webrtc.PeerConnectionFactory

class P2PChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize WebRTC
        initializeWebRTC()
    }

    private fun initializeWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(false)
            .setFieldTrials("")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }
}