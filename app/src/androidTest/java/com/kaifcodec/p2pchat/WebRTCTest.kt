package com.kaifcodec.p2pchat

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaifcodec.p2pchat.webrtc.WebRTCClient
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class WebRTCTest {

    @Test
    fun webRTCClient_initialization_doesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Test that WebRTCClient can be created without throwing exceptions
        try {
            val webRTCClient = WebRTCClient(context)
            webRTCClient.initialize()

            // If we get here, initialization succeeded
            assertTrue("WebRTC client initialized successfully", true)

            // Clean up
            webRTCClient.destroy()
        } catch (e: Exception) {
            fail("WebRTC client initialization should not throw: ${e.message}")
        }
    }

    @Test
    fun webRTCClient_connectionStateFlow_isInitialized() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val webRTCClient = WebRTCClient(context)

        // Test that connection state flow is accessible
        assertNotNull("Connection state flow should not be null", webRTCClient.connectionState)

        webRTCClient.destroy()
    }

    @Test
    fun firebaseClasses_canBeLoaded() {
        try {
            // Test that Firebase classes can be loaded (indicates proper dependencies)
            Class.forName("com.google.firebase.firestore.FirebaseFirestore")
            Class.forName("com.kaifcodec.p2pchat.data.remote.FirebaseSignaling")
            assertTrue("Firebase classes loaded successfully", true)
        } catch (e: ClassNotFoundException) {
            fail("Firebase classes should be available: ${e.message}")
        }
    }
}