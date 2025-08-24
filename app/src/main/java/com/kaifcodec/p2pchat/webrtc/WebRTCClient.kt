package com.kaifcodec.p2pchat.webrtc

import android.content.Context
import com.kaifcodec.p2pchat.models.SignalData
import com.kaifcodec.p2pchat.models.SignalType
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class WebRTCClient(
    private val context: Context,
    private val userId: String,
    private val onSignalNeed: (SignalData) -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var remoteDataChannel: DataChannel? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            false,
            false
        )

        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun initializePeerConnection(): Boolean {
        try {
            val iceServers = Constants.STUN_SERVERS.map { url ->
                PeerConnection.IceServer.builder(url).createIceServer()
            }

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            peerConnection = peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                PeerConnectionObserver()
            )

            return peerConnection != null
        } catch (e: Exception) {
            Logger.e("Failed to initialize peer connection", e)
            return false
        }
    }

    fun createDataChannel() {
        try {
            val init = DataChannel.Init().apply {
                ordered = true
                maxRetransmitTimeMs = -1
                maxRetransmits = -1
                protocol = ""
                negotiated = false
                id = -1
            }

            dataChannel = peerConnection?.createDataChannel("messages", init)
            dataChannel?.registerObserver(DataChannelObserver())
            Logger.d("Data channel created successfully")
        } catch (e: Exception) {
            Logger.e("Failed to create data channel", e)
        }
    }

    suspend fun createOffer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val mediaConstraints = MediaConstraints()
                val offerSdp = peerConnection?.createOffer(mediaConstraints)
                if (offerSdp != null) {
                    peerConnection?.setLocalDescription(offerSdp)

                    val signalData = SignalData(
                        type = SignalType.OFFER,
                        data = offerSdp.description,
                        senderId = userId
                    )

                    withContext(Dispatchers.Main) {
                        onSignalNeed(signalData)
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Logger.e("Failed to create offer", e)
                false
            }
        }
    }

    suspend fun createAnswer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val mediaConstraints = MediaConstraints()
                val answerSdp = peerConnection?.createAnswer(mediaConstraints)
                if (answerSdp != null) {
                    peerConnection?.setLocalDescription(answerSdp)

                    val signalData = SignalData(
                        type = SignalType.ANSWER,
                        data = answerSdp.description,
                        senderId = userId
                    )

                    withContext(Dispatchers.Main) {
                        onSignalNeed(signalData)
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Logger.e("Failed to create answer", e)
                false
            }
        }
    }

    suspend fun handleRemoteOffer(offerSdp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sessionDescription = SessionDescription(
                    SessionDescription.Type.OFFER,
                    offerSdp
                )
                peerConnection?.setRemoteDescription(sessionDescription)
                createAnswer()
            } catch (e: Exception) {
                Logger.e("Failed to handle remote offer", e)
                false
            }
        }
    }

    suspend fun handleRemoteAnswer(answerSdp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sessionDescription = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    answerSdp
                )
                peerConnection?.setRemoteDescription(sessionDescription)
                true
            } catch (e: Exception) {
                Logger.e("Failed to handle remote answer", e)
                false
            }
        }
    }

    fun handleIceCandidate(candidateData: String) {
        try {
            val parts = candidateData.split("|||")
            if (parts.size >= 3) {
                val candidate = IceCandidate(
                    parts[0], // sdpMid
                    parts[1].toInt(), // sdpMLineIndex
                    parts[2] // sdp
                )
                peerConnection?.addIceCandidate(candidate)
                Logger.d("Added ICE candidate")
            }
        } catch (e: Exception) {
            Logger.e("Failed to handle ICE candidate", e)
        }
    }

    fun sendMessage(message: String): Boolean {
        return try {
            val dataChannel = this.dataChannel ?: this.remoteDataChannel
            if (dataChannel?.state() == DataChannel.State.OPEN) {
                val buffer = StandardCharsets.UTF_8.encode(message)
                val dataBuffer = DataChannel.Buffer(buffer, false)
                dataChannel.send(dataBuffer)
                Logger.d("Message sent: $message")
                true
            } else {
                Logger.w("Data channel not open, state: \${dataChannel?.state()}")
                false
            }
        } catch (e: Exception) {
            Logger.e("Failed to send message", e)
            false
        }
    }

    fun close() {
        try {
            dataChannel?.close()
            remoteDataChannel?.close()
            peerConnection?.close()
            peerConnectionFactory?.dispose()
            scope.cancel()
            Logger.d("WebRTC client closed")
        } catch (e: Exception) {
            Logger.e("Error closing WebRTC client", e)
        }
    }

    private inner class PeerConnectionObserver : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            val candidateData = "\${candidate.sdpMid}|||\${candidate.sdpMLineIndex}|||\${candidate.sdp}"
            val signalData = SignalData(
                type = SignalType.ICE_CANDIDATE,
                data = candidateData,
                senderId = userId
            )
            onSignalNeed(signalData)
            Logger.d("ICE candidate generated")
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            Logger.d("Connection state changed: $newState")
            _isConnected.value = newState == PeerConnection.PeerConnectionState.CONNECTED
            onConnectionStateChange(newState)
        }

        override fun onDataChannel(dataChannel: DataChannel) {
            Logger.d("Data channel received")
            remoteDataChannel = dataChannel
            dataChannel.registerObserver(DataChannelObserver())
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            Logger.d("ICE connection state: $newState")
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
            Logger.d("ICE gathering state: $newState")
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState) {
            Logger.d("Signaling state: $newState")
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {
            Logger.d("ICE candidates removed")
        }

        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
        override fun onRenegotiationNeeded() {}
    }

    private inner class DataChannelObserver : DataChannel.Observer {

        override fun onMessage(buffer: DataChannel.Buffer) {
            try {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data, StandardCharsets.UTF_8)
                Logger.d("Message received: $message")
                onMessageReceived(message)
            } catch (e: Exception) {
                Logger.e("Failed to process received message", e)
            }
        }

        override fun onStateChange() {
            val state = dataChannel?.state() ?: remoteDataChannel?.state()
            Logger.d("Data channel state changed: $state")
        }

        override fun onBufferedAmountChange(amount: Long) {
            Logger.d("Data channel buffered amount: $amount")
        }
    }
}

// Extension functions for better coroutine support
private suspend fun PeerConnection.createOffer(constraints: MediaConstraints): SessionDescription? =
    suspendCancellableCoroutine { continuation ->
        createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                continuation.resume(sessionDescription, null)
            }

            override fun onCreateFailure(error: String) {
                continuation.resume(null, null)
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

private suspend fun PeerConnection.createAnswer(constraints: MediaConstraints): SessionDescription? =
    suspendCancellableCoroutine { continuation ->
        createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                continuation.resume(sessionDescription, null)
            }

            override fun onCreateFailure(error: String) {
                continuation.resume(null, null)
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

private suspend fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription): Boolean =
    suspendCancellableCoroutine { continuation ->
        setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() {
                continuation.resume(true, null)
            }

            override fun onSetFailure(error: String) {
                continuation.resume(false, null)
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription) {}
            override fun onCreateFailure(error: String) {}
        }, sessionDescription)
    }

private suspend fun PeerConnection.setRemoteDescription(sessionDescription: SessionDescription): Boolean =
    suspendCancellableCoroutine { continuation ->
        setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                continuation.resume(true, null)
            }

            override fun onSetFailure(error: String) {
                continuation.resume(false, null)
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription) {}
            override fun onCreateFailure(error: String) {}
        }, sessionDescription)
    }