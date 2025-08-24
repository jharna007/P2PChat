package com.kaifcodec.p2pchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaifcodec.p2pchat.data.repository.ChatRepository
import com.kaifcodec.p2pchat.models.ChatMessage
import com.kaifcodec.p2pchat.models.ConnectionState
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.utils.*
import com.kaifcodec.p2pchat.webrtc.WebRTCClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val webRTCClient: WebRTCClient
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _currentRoom = MutableStateFlow<String?>(null)
    val currentRoom: StateFlow<String?> = _currentRoom

    private val _userId = MutableStateFlow(generateUserId())
    val userId: StateFlow<String> = _userId

    private val _recentRooms = MutableStateFlow<List<String>>(emptyList())
    val recentRooms: StateFlow<List<String>> = _recentRooms

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Observe WebRTC connection state
        viewModelScope.launch {
            webRTCClient.connectionState.collect { state ->
                _connectionState.value = state
            }
        }

        // Initialize WebRTC
        webRTCClient.initialize()
        setupWebRTCListeners()
        loadRecentRooms()
    }

    private fun setupWebRTCListeners() {
        webRTCClient.setMessageListener { messageContent ->
            val roomId = _currentRoom.value ?: return@setMessageListener
            val message = ChatMessage(
                id = generateMessageId(),
                content = messageContent,
                timestamp = System.currentTimeMillis(),
                senderId = "remote", // Remote user ID
                roomId = roomId,
                deliveryState = Constants.MESSAGE_STATE_DELIVERED,
                isFromMe = false
            )

            viewModelScope.launch {
                repository.insertMessage(message)
            }
        }

        webRTCClient.setSignalListener { signalType, data ->
            val roomId = _currentRoom.value ?: return@setSignalListener
            val signalData = SignalData(
                type = signalType,
                data = data,
                userId = _userId.value
            )

            viewModelScope.launch {
                repository.sendSignal(roomId, _userId.value, signalData)
            }
        }
    }

    fun createRoom(): String {
        val roomCode = generateRoomCode()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val success = repository.createRoom(roomCode)
                if (success) {
                    _currentRoom.value = roomCode
                    startListeningForSignals(roomCode)
                    loadMessagesForRoom(roomCode)
                } else {
                    _errorMessage.emit("Failed to create room")
                }
            } catch (e: Exception) {
                _errorMessage.emit("Error creating room: ${e.message}")
                Logger.e("Error creating room", e)
            } finally {
                _isLoading.value = false
            }
        }

        return roomCode
    }

    fun joinRoom(roomCode: String) {
        if (!roomCode.isValidRoomCode()) {
            viewModelScope.launch {
                _errorMessage.emit("Invalid room code format")
            }
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val success = repository.joinRoom(roomCode)
                if (success) {
                    _currentRoom.value = roomCode
                    startListeningForSignals(roomCode)
                    loadMessagesForRoom(roomCode)
                    createWebRTCOffer()
                } else {
                    _errorMessage.emit("Failed to join room. Room may not exist or has expired.")
                }
            } catch (e: Exception) {
                _errorMessage.emit("Error joining room: ${e.message}")
                Logger.e("Error joining room", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createWebRTCOffer() {
        webRTCClient.createOffer(
            onSuccess = { type, sdp ->
                val roomId = _currentRoom.value ?: return@createOffer
                val signalData = SignalData(
                    type = Constants.SIGNAL_TYPE_OFFER,
                    data = mapOf("type" to type, "sdp" to sdp),
                    userId = _userId.value
                )

                viewModelScope.launch {
                    repository.sendSignal(roomId, _userId.value, signalData)
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _errorMessage.emit("Failed to create offer: $error")
                }
            }
        )
    }

    private fun startListeningForSignals(roomId: String) {
        viewModelScope.launch {
            repository.listenForSignals(roomId, _userId.value).collect { signalData ->
                handleSignalData(signalData)
            }
        }
    }

    private fun handleSignalData(signalData: SignalData) {
        when (signalData.type) {
            Constants.SIGNAL_TYPE_OFFER -> {
                val type = signalData.data["type"] as? String ?: return
                val sdp = signalData.data["sdp"] as? String ?: return

                webRTCClient.setRemoteDescription(type, sdp,
                    onSuccess = {
                        createWebRTCAnswer()
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            _errorMessage.emit("Failed to set remote description: $error")
                        }
                    }
                )
            }

            Constants.SIGNAL_TYPE_ANSWER -> {
                val type = signalData.data["type"] as? String ?: return
                val sdp = signalData.data["sdp"] as? String ?: return

                webRTCClient.setRemoteDescription(type, sdp,
                    onSuccess = {
                        Logger.d("Remote answer set successfully")
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            _errorMessage.emit("Failed to set remote answer: $error")
                        }
                    }
                )
            }

            Constants.SIGNAL_TYPE_ICE_CANDIDATE -> {
                val candidate = signalData.data["candidate"] as? String ?: return
                val sdpMid = signalData.data["sdpMid"] as? String
                val sdpMLineIndex = (signalData.data["sdpMLineIndex"] as? Number)?.toInt() ?: return

                webRTCClient.addIceCandidate(candidate, sdpMid, sdpMLineIndex)
            }
        }
    }

    private fun createWebRTCAnswer() {
        webRTCClient.createAnswer(
            onSuccess = { type, sdp ->
                val roomId = _currentRoom.value ?: return@createAnswer
                val signalData = SignalData(
                    type = Constants.SIGNAL_TYPE_ANSWER,
                    data = mapOf("type" to type, "sdp" to sdp),
                    userId = _userId.value
                )

                viewModelScope.launch {
                    repository.sendSignal(roomId, _userId.value, signalData)
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _errorMessage.emit("Failed to create answer: $error")
                }
            }
        )
    }

    private fun loadMessagesForRoom(roomId: String) {
        viewModelScope.launch {
            repository.getMessagesForRoom(roomId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(content: String) {
        if (!content.isValidMessage()) {
            viewModelScope.launch {
                _errorMessage.emit("Invalid message. Check length and content.")
            }
            return
        }

        val roomId = _currentRoom.value ?: return

        viewModelScope.launch {
            try {
                // Check rate limit
                if (!repository.canSendMessage(roomId, _userId.value)) {
                    _errorMessage.emit("Rate limit exceeded. Please wait before sending another message.")
                    return@launch
                }

                val message = ChatMessage(
                    id = generateMessageId(),
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    senderId = _userId.value,
                    roomId = roomId,
                    deliveryState = Constants.MESSAGE_STATE_SENDING,
                    isFromMe = true
                )

                // Insert message locally first
                repository.insertMessage(message)

                // Try to send via WebRTC
                val sent = webRTCClient.sendMessage(content)

                val newState = if (sent) {
                    Constants.MESSAGE_STATE_SENT
                } else {
                    Constants.MESSAGE_STATE_FAILED
                }

                repository.updateMessageDeliveryState(message.id, newState)

            } catch (e: Exception) {
                _errorMessage.emit("Failed to send message: ${e.message}")
                Logger.e("Error sending message", e)
            }
        }
    }

    fun leaveRoom() {
        val roomId = _currentRoom.value ?: return

        viewModelScope.launch {
            repository.leaveRoom(roomId)
            webRTCClient.disconnect()
            _currentRoom.value = null
            _messages.value = emptyList()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun loadRecentRooms() {
        viewModelScope.launch {
            try {
                val rooms = repository.getRecentRooms()
                _recentRooms.value = rooms
            } catch (e: Exception) {
                Logger.e("Error loading recent rooms", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        webRTCClient.destroy()
        Logger.d("ChatViewModel cleared")
    }
}
