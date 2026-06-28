package com.klicmobile.app.calling

import android.content.Intent
import android.os.Bundle

/** Incoming-call payload, passed via FCM data and Intent extras. */
data class CallInvite(
    val callId: String,
    val conversationId: String,
    val roomName: String,
    val livekitUrl: String,
    val kind: String,
    val fromName: String,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString("callId", callId)
        putString("conversationId", conversationId)
        putString("roomName", roomName)
        putString("livekitUrl", livekitUrl)
        putString("kind", kind)
        putString("fromName", fromName)
    }

    companion object {
        fun fromMap(d: Map<String, String>) = CallInvite(
            callId = d["callId"].orEmpty(),
            conversationId = d["conversationId"].orEmpty(),
            roomName = d["roomName"].orEmpty(),
            livekitUrl = d["livekitUrl"].orEmpty(),
            kind = d["kind"] ?: "AUDIO",
            fromName = d["fromName"] ?: "Incoming call",
        )

        fun fromIntent(intent: Intent): CallInvite? {
            val callId = intent.getStringExtra("callId") ?: return null
            return CallInvite(
                callId = callId,
                conversationId = intent.getStringExtra("conversationId").orEmpty(),
                roomName = intent.getStringExtra("roomName").orEmpty(),
                livekitUrl = intent.getStringExtra("livekitUrl").orEmpty(),
                kind = intent.getStringExtra("kind") ?: "AUDIO",
                fromName = intent.getStringExtra("fromName") ?: "Incoming call",
            )
        }
    }
}
