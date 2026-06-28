package com.klic.app.calling

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.klic.app.KlicApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receives FCM pushes: rings incoming calls (full-screen) and posts message notifications. */
class KlicMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val repo = (application as KlicApplication).container.repository
        scope.launch { repo.registerDevice(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            "call.invite" -> CallNotifications.showIncomingCall(this, CallInvite.fromMap(data))
            "message" -> CallNotifications.showMessage(
                this,
                title = data["title"] ?: "New message",
                body = data["body"].orEmpty(),
                conversationId = data["conversationId"].orEmpty(),
            )
        }
    }
}
