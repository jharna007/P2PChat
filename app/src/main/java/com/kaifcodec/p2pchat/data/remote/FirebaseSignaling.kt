package com.kaifcodec.p2pchat.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.models.SignalType
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseSignaling {

    private val firestore = FirebaseFirestore.getInstance()
    private var signalListeners = mutableMapOf<String, ListenerRegistration>()

    suspend fun createRoom(roomId: String, userId: String): Boolean {
        return try {
            val roomData = hashMapOf(
                "createdAt" to System.currentTimeMillis(),
                "expiry" to System.currentTimeMillis() + Constants.ROOM_EXPIRY_TIME,
                "activeUsers" to 1,
                "creator" to userId
            )

            firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")
                .set(roomData)
                .await()

            Logger.d("Room created successfully: $roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to create room", e)
            false
        }
    }

    suspend fun joinRoom(roomId: String, userId: String): Boolean {
        return try {
            val roomRef = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")

            // Check if room exists and is not expired
            val roomSnapshot = roomRef.get().await()
            if (!roomSnapshot.exists()) {
                Logger.w("Room does not exist: $roomId")
                return false
            }

            val expiry = roomSnapshot.getLong("expiry") ?: 0L
            if (System.currentTimeMillis() > expiry) {
                Logger.w("Room expired: $roomId")
                return false
            }

            // Update active users count
            val currentUsers = roomSnapshot.getLong("activeUsers") ?: 0L
            roomRef.update("activeUsers", currentUsers + 1).await()

            Logger.d("Joined room successfully: $roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to join room", e)
            false
        }
    }

    suspend fun leaveRoom(roomId: String, userId: String): Boolean {
        return try {
            val roomRef = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")

            val roomSnapshot = roomRef.get().await()
            if (roomSnapshot.exists()) {
                val currentUsers = (roomSnapshot.getLong("activeUsers") ?: 1L) - 1L
                if (currentUsers <= 0) {
                    // Delete the entire room if no users left
                    firestore.collection(Constants.ROOMS_COLLECTION)
                        .document(roomId)
                        .delete()
                        .await()
                } else {
                    roomRef.update("activeUsers", currentUsers).await()
                }
            }

            // Stop listening for signals
            stopListeningForSignals(roomId, userId)

            Logger.d("Left room successfully: $roomId")
            true
        } catch (e: Exception) {
            Logger.e("Failed to leave room", e)
            false
        }
    }

    suspend fun sendSignal(roomId: String, signalData: SignalData): Boolean {
        return try {
            val signalDoc = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.SIGNALS_COLLECTION)
                .document(signalData.senderId)

            val data = hashMapOf(
                "type" to signalData.type.name,
                "data" to signalData.data,
                "senderId" to signalData.senderId,
                "targetId" to signalData.targetId,
                "timestamp" to signalData.timestamp,
                "roomId" to roomId
            )

            signalDoc.set(data).await()
            Logger.d("Signal sent: ${signalData.type}")
            true
        } catch (e: Exception) {
            Logger.e("Failed to send signal", e)
            false
        }
    }

    fun listenForSignals(roomId: String, userId: String): Flow<SignalData> = callbackFlow {
        val listenerKey = "${roomId}_$userId"

        val listener = firestore.collection(Constants.ROOMS_COLLECTION)
            .document(roomId)
            .collection(Constants.SIGNALS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Logger.e("Error listening for signals", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    try {
                        val data = change.document.data
                        val senderId = data["senderId"] as? String ?: ""

                        // Don't process our own signals
                        if (senderId != userId) {
                            val signalData = SignalData(
                                type = SignalType.valueOf(data["type"] as String),
                                data = data["data"] as String,
                                senderId = senderId,
                                targetId = data["targetId"] as? String ?: "",
                                timestamp = data["timestamp"] as Long,
                                roomId = data["roomId"] as? String ?: roomId
                            )

                            trySend(signalData)
                        }
                    } catch (e: Exception) {
                        Logger.e("Error processing signal", e)
                    }
                }
            }

        signalListeners[listenerKey] = listener

        awaitClose {
            listener.remove()
            signalListeners.remove(listenerKey)
        }
    }

    private fun stopListeningForSignals(roomId: String, userId: String) {
        val listenerKey = "${roomId}_$userId"
        signalListeners[listenerKey]?.remove()
        signalListeners.remove(listenerKey)
    }

    suspend fun cleanupExpiredRooms() {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredRoomsQuery = firestore.collection(Constants.ROOMS_COLLECTION)
                .whereArrayContains("metadata.expiry", currentTime)

            val snapshot = expiredRoomsQuery.get().await()

            snapshot.documents.forEach { document ->
                document.reference.delete()
            }

            Logger.d("Cleaned up expired rooms")
        } catch (e: Exception) {
            Logger.e("Failed to cleanup expired rooms", e)
        }
    }

    suspend fun isRoomActive(roomId: String): Boolean {
        return try {
            val roomSnapshot = firestore.collection(Constants.ROOMS_COLLECTION)
                .document(roomId)
                .collection(Constants.METADATA_COLLECTION)
                .document("info")
                .get()
                .await()

            if (!roomSnapshot.exists()) return false

            val expiry = roomSnapshot.getLong("expiry") ?: 0L
            System.currentTimeMillis() <= expiry
        } catch (e: Exception) {
            Logger.e("Failed to check room status", e)
            false
        }
    }
}