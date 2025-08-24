package com.kaifcodec.p2pchat.models

import com.google.gson.annotations.SerializedName

data class SignalData(
    @SerializedName("type")
    val type: String,

    @SerializedName("data")
    val data: Map<String, Any>,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("userId")
    val userId: String
)

data class IceCandidateData(
    @SerializedName("candidate")
    val candidate: String,

    @SerializedName("sdpMid")
    val sdpMid: String?,

    @SerializedName("sdpMLineIndex")
    val sdpMLineIndex: Int
)

data class SessionDescriptionData(
    @SerializedName("type")
    val type: String,

    @SerializedName("sdp")
    val sdp: String
)
