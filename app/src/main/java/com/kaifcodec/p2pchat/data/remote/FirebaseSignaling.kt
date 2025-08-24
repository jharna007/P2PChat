package com.kaifcodec.p2pchat.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseSignaling {

    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()
    private var signalListener: ListenerRegistration? = null

    suspend fun createRoom(roomId: String): Boolean {
        return try {
            val roomData = mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "expiry" to FieldValue.serverTimestamp().let { timestamp ->
                    // Set expiry to 24 hours from now
                    System.currentTimeMillis() + TimeUnit.HOURS.toMillis(Constants.ROOM_EXPIRY_HOURS)
                },
                "activeUsers" to 0
            )

            firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")
                .set(roomData)
                .await()

            Logger.d("Room created successfully: \$roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to create room", e)
            false
        }
    }

    suspend fun joinRoom(roomId: String): Boolean {
        return try {
            val roomRef = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")

            // Check if room exists and is not expired
            val roomDoc = roomRef.get().await()
            if (!roomDoc.exists()) {
                Logger.w("Room does not exist: \$roomId")
                return false
            }

            val expiry = roomDoc.getLong("expiry") ?: 0
            if (System.currentTimeMillis() > expiry) {
                Logger.w("Room has expired: \$roomId")
                return false
            }

            // Increment active users
            roomRef.update("activeUsers", FieldValue.increment(1)).await()
            Logger.d("Joined room successfully: \$roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to join room", e)
            false
        }
    }

    suspend fun sendSignal(roomId: String, userId: String, signalData: SignalData): Boolean {
        return try {
            firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.SIGNALS_COLLECTION)
                .document(userId)
                .set(signalData)
                .await()

            Logger.d("Signal sent successfully: \${signalData.type}")
            true
        } catch (e: Exception) {
            Logger.e("Failed to send signal", e)
            false
        }
    }

    fun listenForSignals(roomId: String, userId: String): Flow<SignalData> = callbackFlow {
        Logger.d("Starting to listen for signals in room: \$roomId")

        signalListener = firestore.collection(Constants.ROOMS_COLLECTION)
            .document(roomId)
            .collection(Constants.SIGNALS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Logger.e("Error listening for signals", error)
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.document.id != userId) { // Ignore our own signals
                        try {
                            val signalData = change.document.toObject(SignalData::class.java)
                            Logger.d("Received signal: \${signalData.type}")
                            trySend(signalData)
                        } catch (e: Exception) {
                            Logger.e("Failed to parse signal data", e)
                        }
                    }
                }
            }

        awaitClose {
            Logger.d("Stopping signal listener")
            signalListener?.remove()
        }
    }

    suspend fun leaveRoom(roomId: String): Boolean {
        return try {
            val roomRef = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")

            // Decrement active users
            roomRef.update("activeUsers", FieldValue.increment(-1)).await()
            Logger.d("Left room successfully: \$roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to leave room", e)
            false
        }
    }

    suspend fun cleanupExpiredRooms(): Boolean {
        return try {
            val currentTime = System.currentTimeMillis()
            val expiredRooms = firestore.collection(Constants.ROOMS_COLLECTION)
                .whereArrayContains("metadata", "info")
                .whereLessThan("expiry", currentTime)
                .get()
                .await()

            expiredRooms.documents.forEach { document ->
                document.reference.delete().await()
                Logger.d("Cleaned up expired room: \${document.id}")
            }

            true
        } catch (e: Exception) {
            Logger.e("Failed to cleanup expired rooms", e)
            false
        }
    }

    fun stopListening() {
        signalListener?.remove()
        signalListener = null
    }
}
