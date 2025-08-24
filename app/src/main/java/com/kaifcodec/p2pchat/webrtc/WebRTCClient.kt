package com.kaifcodec.p2pchat.webrtc

import android.content.Context
import com.kaifcodec.p2pchat.models.ConnectionState
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*

class WebRTCClient(private val context: Context) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var onMessageReceived: ((String) -> Unit)? = null
    private var onSignalData: ((String, Map<String, Any>) -> Unit)? = null

    fun initialize() {
        Logger.d("Initializing WebRTC")
        initializePeerConnectionFactory()
        createPeerConnection()
    }

    private fun initializePeerConnectionFactory() {
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection() {
        val iceServers = Constants.ICE_SERVERS.map { url ->
            PeerConnection.IceServer.builder(url).createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                Logger.d("Signaling state changed: \$newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Logger.d("ICE connection state changed: \$newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        _connectionState.value = ConnectionState.Connected
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        _connectionState.value = ConnectionState.Failed("ICE connection failed")
                    }
                    PeerConnection.IceConnectionState.CHECKING -> {
                        _connectionState.value = ConnectionState.Connecting
                    }
                    else -> { /* Handle other states */ }
                }
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                Logger.d("ICE gathering state changed: \$newState")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Logger.d("New ICE candidate: \${it.sdp}")
                    val candidateData = mapOf(
                        "candidate" to it.sdp,
                        "sdpMid" to (it.sdpMid ?: ""),
                        "sdpMLineIndex" to it.sdpMLineIndex
                    )
                    onSignalData?.invoke(Constants.SIGNAL_TYPE_ICE_CANDIDATE, candidateData)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Logger.d("ICE candidates removed")
            }

            override fun onAddStream(stream: MediaStream?) {
                // Not used for data channel only
            }

            override fun onRemoveStream(stream: MediaStream?) {
                // Not used for data channel only
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Logger.d("Data channel received")
                setupDataChannel(dataChannel)
            }

            override fun onRenegotiationNeeded() {
                Logger.d("Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                // Not used for data channel only
            }
        })
    }

    fun createOffer(onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        Logger.d("Creating offer")
        _connectionState.value = ConnectionState.Connecting

        // Create data channel first
        val dataChannelInit = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 3
        }

        dataChannel = peerConnection?.createDataChannel("messages", dataChannelInit)
        setupDataChannel(dataChannel)

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Logger.d("Local description set successfully")
                            onSuccess(sdp.type.canonicalForm(), sdp.description)
                        }

                        override fun onSetFailure(error: String?) {
                            Logger.e("Failed to set local description: \$error")
                            onError(error ?: "Failed to set local description")
                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
            }

            override fun onCreateFailure(error: String?) {
                Logger.e("Failed to create offer: \$error")
                onError(error ?: "Failed to create offer")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun createAnswer(onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        Logger.d("Creating answer")
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Logger.d("Local description set successfully")
                            onSuccess(sdp.type.canonicalForm(), sdp.description)
                        }

                        override fun onSetFailure(error: String?) {
                            Logger.e("Failed to set local description: \$error")
                            onError(error ?: "Failed to set local description")
                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
            }

            override fun onCreateFailure(error: String?) {
                Logger.e("Failed to create answer: \$error")
                onError(error ?: "Failed to create answer")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun setRemoteDescription(type: String, sdp: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Logger.d("Setting remote description")
        val sessionDescription = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type),
            sdp
        )

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Logger.d("Remote description set successfully")
                onSuccess()
            }

            override fun onSetFailure(error: String?) {
                Logger.e("Failed to set remote description: \$error")
                onError(error ?: "Failed to set remote description")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }

    fun addIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) {
        Logger.d("Adding ICE candidate")
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun setupDataChannel(channel: DataChannel?) {
        channel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {
                Logger.d("Data channel buffered amount changed: \$amount")
            }

            override fun onStateChange() {
                Logger.d("Data channel state changed: \${channel.state()}")
                when (channel.state()) {
                    DataChannel.State.OPEN -> {
                        _connectionState.value = ConnectionState.Connected
                    }
                    DataChannel.State.CLOSED -> {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    else -> { /* Handle other states */ }
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.data?.let { data ->
                    val message = String(data.array())
                    Logger.d("Received message: \$message")
                    onMessageReceived?.invoke(message)
                }
            }
        })
    }

    fun sendMessage(message: String): Boolean {
        return try {
            val buffer = ByteBuffer.wrap(message.toByteArray())
            val dataChannelBuffer = DataChannel.Buffer(buffer, false)
            dataChannel?.send(dataChannelBuffer) == true
        } catch (e: Exception) {
            Logger.e("Failed to send message", e)
            false
        }
    }

    fun setMessageListener(listener: (String) -> Unit) {
        onMessageReceived = listener
    }

    fun setSignalListener(listener: (String, Map<String, Any>) -> Unit) {
        onSignalData = listener
    }

    fun disconnect() {
        Logger.d("Disconnecting WebRTC")
        dataChannel?.close()
        peerConnection?.close()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun destroy() {
        disconnect()
        peerConnectionFactory?.dispose()
        PeerConnectionFactory.shutdownInternalTracer()
    }
}
