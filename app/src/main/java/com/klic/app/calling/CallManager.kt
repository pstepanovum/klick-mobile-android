package com.klic.app.calling

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wraps a LiveKit room for a 1:1 call and exposes the in-call controls.
 * Media is routed by the LiveKit SFU; this only manages local track state.
 */
class CallManager(private val appContext: Context) {

    val isConnected = MutableStateFlow(false)
    val micEnabled = MutableStateFlow(true)
    val cameraEnabled = MutableStateFlow(false)

    private var room: Room? = null

    suspend fun join(url: String, token: String, video: Boolean) {
        val room = LiveKit.create(appContext).also { this.room = it }
        room.connect(url, token)
        room.localParticipant.setMicrophoneEnabled(true)
        if (video) room.localParticipant.setCameraEnabled(true)
        isConnected.value = true
        micEnabled.value = true
        cameraEnabled.value = video
    }

    suspend fun toggleMic() {
        val next = !micEnabled.value
        room?.localParticipant?.setMicrophoneEnabled(next)
        micEnabled.value = next
    }

    suspend fun toggleCamera() {
        val next = !cameraEnabled.value
        room?.localParticipant?.setCameraEnabled(next)
        cameraEnabled.value = next
    }

    fun leave() {
        room?.disconnect()
        room = null
        isConnected.value = false
    }
}
