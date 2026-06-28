package com.klic.app.realtime

import com.klic.app.data.Message
import com.klic.app.data.Network
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

/** Socket.io client mirroring klick-server/src/realtime/events.ts. */
class SocketService {
    private var socket: Socket? = null

    val incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 32)
    val incomingCalls = MutableSharedFlow<CallInvite>(extraBufferCapacity = 8)

    data class CallInvite(
        val callId: String,
        val conversationId: String,
        val roomName: String,
        val livekitUrl: String,
        val kind: String,
        val fromDisplayName: String,
    )

    fun connect(accessToken: String) {
        val opts = IO.Options().apply { auth = mapOf("token" to accessToken) }
        val socket = IO.socket(Network.BASE_HTTP, opts).also { this.socket = it }

        socket.on("message:new") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                runCatching {
                    incomingMessages.tryEmit(
                        Json.decodeFromString(Message.serializer(), json.toString())
                    )
                }
            }
        }
        socket.on("call:invite") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                val from = json.optJSONObject("from")
                incomingCalls.tryEmit(
                    CallInvite(
                        callId = json.optString("callId"),
                        conversationId = json.optString("conversationId"),
                        roomName = json.optString("roomName"),
                        livekitUrl = json.optString("livekitUrl"),
                        kind = json.optString("kind", "AUDIO"),
                        fromDisplayName = from?.optString("displayName") ?: "Unknown",
                    )
                )
            }
        }
        socket.connect()
    }

    fun emit(event: String, payload: JsonObject) = socket?.emit(event, JSONObject(payload.toString()))
    fun disconnect() { socket?.disconnect(); socket = null }
}
