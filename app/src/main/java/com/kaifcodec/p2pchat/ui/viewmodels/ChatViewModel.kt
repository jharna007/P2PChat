package com.kaifcodec.p2pchat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kaifcodec.p2pchat.data.local.AppDatabase
import com.kaifcodec.p2pchat.data.remote.FirebaseSignaling
import com.kaifcodec.p2pchat.data.repository.ChatRepository
import com.kaifcodec.p2pchat.models.*
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import com.kaifcodec.p2pchat.utils.generateRoomCode
import com.kaifcodec.p2pchat.utils.generateUserId
import com.kaifcodec.p2pchat.webrtc.WebRTCClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(
        AppDatabase.getDatabase(application).messageDao(),
        FirebaseSignaling()
    )

    private var webRTCClient: WebRTCClient? = null
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _roomId = MutableLiveData<String>()
    val roomId: LiveData<String> = _roomId

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentUserId: String = generateUserId()
    private var currentRoomId: String = ""
    private var isCaller: Boolean = false

    // Rate limiting for messages
    private val messageTimes = mutableListOf<Long>()

    fun createRoom(): String {
        val newRoomId = generateRoomCode()
        _roomId.value = newRoomId
        currentRoomId = newRoomId
        isCaller = true

        viewModelScope.launch {
            try {
                val success = repository.createRoom(newRoomId, currentUserId)
                if (success) {
                    initializeWebRTC(newRoomId, true)
                    Logger.d("Room created: $newRoomId")
                } else {
                    _error.value = "Failed to create room"
                }
            } catch (e: Exception) {
                Logger.e("Error creating room", e)
                _error.value = "Error creating room: ${e.message}"
            }
        }

        return newRoomId
    }

    fun joinRoom(roomId: String) {
        if (roomId.length != Constants.ROOM_CODE_LENGTH) {
            _error.value = "Invalid room code"
            return
        }

        _roomId.value = roomId
        currentRoomId = roomId
        isCaller = false

        viewModelScope.launch {
            try {
                val isActive = repository.isRoomActive(roomId)
                if (!isActive) {
                    _error.value = "Room not found or expired"
                    return@launch
                }

                val success = repository.joinRoom(roomId, currentUserId)
                if (success) {
                    initializeWebRTC(roomId, false)
                    Logger.d("Joined room: $roomId")
                } else {
                    _error.value = "Failed to join room"
                }
            } catch (e: Exception) {
                Logger.e("Error joining room", e)
                _error.value = "Error joining room: ${e.message}"
            }
        }
    }

    private fun initializeWebRTC(roomId: String, isCaller: Boolean) {
        try {
            webRTCClient = WebRTCClient(
                context = getApplication(),
                userId = currentUserId,
                onSignalNeed = { signalData ->
                    sendSignal(roomId, signalData)
                },
                onMessageReceived = { message ->
                    handleReceivedMessage(message)
                },
                onConnectionStateChange = { state ->
                    handleConnectionStateChange(state)
                }
            )

            val initialized = webRTCClient?.initializePeerConnection() ?: false
            if (!initialized) {
                _error.value = "Failed to initialize WebRTC"
                return
            }

            if (isCaller) {
                webRTCClient?.createDataChannel()
                startCall()
            }

            // Start listening for signals
            listenForSignals(roomId)

            // Load existing messages
            loadMessages(roomId)

        } catch (e: Exception) {
            Logger.e("Error initializing WebRTC", e)
            _error.value = "Error initializing connection: ${e.message}"
        }
    }

    private fun startCall() {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                webRTCClient?.createOffer()
            } catch (e: Exception) {
                Logger.e("Error starting call", e)
                _connectionState.value = ConnectionState.FAILED
            }
        }
    }

    private fun sendSignal(roomId: String, signalData: SignalData) {
        viewModelScope.launch {
            try {
                repository.sendSignal(roomId, signalData)
            } catch (e: Exception) {
                Logger.e("Error sending signal", e)
            }
        }
    }

    private fun listenForSignals(roomId: String) {
        viewModelScope.launch {
            repository.listenForSignals(roomId, currentUserId)
                .collect { signalData ->
                    handleReceivedSignal(signalData)
                }
        }
    }

    private fun handleReceivedSignal(signalData: SignalData) {
        viewModelScope.launch {
            try {
                when (signalData.type) {
                    SignalType.OFFER -> {
                        webRTCClient?.handleRemoteOffer(signalData.data)
                    }
                    SignalType.ANSWER -> {
                        webRTCClient?.handleRemoteAnswer(signalData.data)
                    }
                    SignalType.ICE_CANDIDATE -> {
                        webRTCClient?.handleIceCandidate(signalData.data)
                    }
                    else -> {
                        Logger.d("Unhandled signal type: ${signalData.type}")
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error handling received signal", e)
            }
        }
    }

    private fun handleReceivedMessage(messageContent: String) {
        val message = ChatMessage(
            id = repository.generateMessageId(),
            content = messageContent,
            senderId = "remote",
            senderName = "Remote User",
            timestamp = System.currentTimeMillis(),
            deliveryStatus = DeliveryStatus.DELIVERED
        )

        viewModelScope.launch {
            repository.insertMessage(message, currentRoomId, false)
        }
    }

    private fun handleConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        val connectionState = when (state) {
            PeerConnection.PeerConnectionState.NEW -> ConnectionState.DISCONNECTED
            PeerConnection.PeerConnectionState.CONNECTING -> ConnectionState.CONNECTING
            PeerConnection.PeerConnectionState.CONNECTED -> ConnectionState.CONNECTED
            PeerConnection.PeerConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
            PeerConnection.PeerConnectionState.FAILED -> ConnectionState.FAILED
            PeerConnection.PeerConnectionState.CLOSED -> ConnectionState.DISCONNECTED
        }

        _connectionState.value = connectionState
        _isConnected.value = state == PeerConnection.PeerConnectionState.CONNECTED

        if (state == PeerConnection.PeerConnectionState.CONNECTED) {
            Logger.d("WebRTC connection established")
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || content.length > Constants.MAX_MESSAGE_LENGTH) {
            _error.value = "Invalid message content"
            return
        }

        // Rate limiting
        val currentTime = System.currentTimeMillis()
        messageTimes.removeAll { currentTime - it > 60000 } // Remove messages older than 1 minute

        if (messageTimes.size >= Constants.MAX_MESSAGES_PER_MINUTE) {
            _error.value = "Too many messages. Please wait."
            return
        }

        messageTimes.add(currentTime)

        val message = ChatMessage(
            id = repository.generateMessageId(),
            content = content,
            senderId = currentUserId,
            senderName = "You",
            timestamp = currentTime,
            deliveryStatus = DeliveryStatus.SENDING
        )

        viewModelScope.launch {
            try {
                // Save to local database first
                repository.insertMessage(message, currentRoomId, true)

                // Send via WebRTC
                val sent = webRTCClient?.sendMessage(content) ?: false
                val status = if (sent) DeliveryStatus.SENT else DeliveryStatus.FAILED

                repository.updateMessageDeliveryStatus(message.id, status)

            } catch (e: Exception) {
                Logger.e("Error sending message", e)
                repository.updateMessageDeliveryStatus(message.id, DeliveryStatus.FAILED)
            }
        }
    }

    private fun loadMessages(roomId: String) {
        viewModelScope.launch {
            repository.getMessagesByRoom(roomId)
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            try {
                repository.leaveRoom(currentRoomId, currentUserId)
                webRTCClient?.close()
                webRTCClient = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _isConnected.value = false
                Logger.d("Left room: $currentRoomId")
            } catch (e: Exception) {
                Logger.e("Error leaving room", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRTCClient?.close()
    }

    fun getCurrentUserId(): String = currentUserId
    fun getCurrentRoomId(): String = currentRoomId
    fun getIsCaller(): Boolean = isCaller
}