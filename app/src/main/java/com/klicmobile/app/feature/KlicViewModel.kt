package com.klicmobile.app.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klicmobile.app.calling.CallManager
import com.klicmobile.app.data.CallSession
import com.klicmobile.app.data.Conversation
import com.klicmobile.app.data.FriendRequest
import com.klicmobile.app.data.KlicRepository
import com.klicmobile.app.data.Message
import com.klicmobile.app.data.TokenStore
import com.klicmobile.app.data.User
import com.klicmobile.app.realtime.SocketService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KlicViewModel(
    private val repo: KlicRepository,
    private val tokenStore: TokenStore,
    private val socket: SocketService,
    val callManager: CallManager,
    private val container: com.klicmobile.app.AppContainer,
) : ViewModel() {

    val currentUser = MutableStateFlow<User?>(null)
    val isAuthenticated = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val isDark = MutableStateFlow(container.isDark)

    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val messages = MutableStateFlow<List<Message>>(emptyList())
    val activeCall = MutableStateFlow<CallSession?>(null)
    val callPeerName = MutableStateFlow("")
    val callStatus = MutableStateFlow("Calling...")

    val friends = MutableStateFlow<List<User>>(emptyList())
    val friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendStatus = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            tokenStore.load()
            if (repo.isAuthenticated) onAuthed()
        }
        viewModelScope.launch {
            socket.incomingMessages.collect { msg ->
                if (msg.conversationId == openConversationId) messages.value = messages.value + msg
            }
        }
        viewModelScope.launch {
            socket.callEvents.collect { event ->
                handleCallEvent(event)
            }
        }
    }

    private var openConversationId: String? = null
    private var activeCallOutgoing = false
    private var ringTimeoutJob: Job? = null
    private val finishingCallIds = mutableSetOf<String>()

    fun login(username: String, password: String) = launchAuth {
        repo.login(username, password).let { currentUser.value = it }
        onAuthed()
    }

    fun register(username: String, password: String, displayName: String) = launchAuth {
        repo.register(username, password, displayName).let { currentUser.value = it }
        onAuthed()
    }

    fun logout() = viewModelScope.launch {
        repo.logout()
        socket.disconnect()
        isAuthenticated.value = false
    }

    fun setDark(value: Boolean) {
        isDark.value = value
        container.isDark = value
    }

    fun callFriendDirect(userId: String, kind: String, peerName: String, onStarted: () -> Unit) =
        viewModelScope.launch {
            if (activeCall.value != null) return@launch
            callPeerName.value = peerName
            runCatching {
                val convo = repo.openConversation(userId)
                repo.startCall(convo.id, kind)
            }.onSuccess { session ->
                startActiveCall(session, peerName, outgoing = true)
                onStarted()
            }
        }

    fun loadConversations() = viewModelScope.launch {
        runCatching { repo.conversations() }.onSuccess { conversations.value = it }
    }

    fun loadFriends() = viewModelScope.launch {
        runCatching { repo.friends() }.onSuccess { friends.value = it }
        runCatching { repo.friendRequests() }.onSuccess { friendRequests.value = it }
    }

    fun addFriend(username: String) = viewModelScope.launch {
        val name = username.trim().lowercase()
        if (name.isEmpty()) return@launch
        val user = runCatching { repo.findUser(name) }.getOrNull()
        if (user == null) {
            friendStatus.value = "No user named \"$name\"."
        } else {
            runCatching { repo.sendFriendRequest(user.id) }
            friendStatus.value = "Request sent to ${user.displayName}."
        }
    }

    fun acceptRequest(id: String) = viewModelScope.launch {
        runCatching { repo.acceptFriendRequest(id) }; loadFriends()
    }

    fun declineRequest(id: String) = viewModelScope.launch {
        runCatching { repo.declineFriendRequest(id) }; loadFriends()
    }

    fun openConversationWith(userId: String, onReady: (Conversation) -> Unit) = viewModelScope.launch {
        runCatching { repo.openConversation(userId) }.onSuccess { c ->
            if (conversations.value.none { it.id == c.id }) conversations.value = conversations.value + c
            onReady(c)
        }
    }

    fun openChat(conversationId: String) = viewModelScope.launch {
        openConversationId = conversationId
        runCatching { repo.messages(conversationId) }
            .onSuccess { messages.value = it.reversed() }
    }

    fun send(conversationId: String, body: String) = viewModelScope.launch {
        runCatching { repo.send(conversationId, body) }
            .onSuccess { messages.value = messages.value + it }
    }

    fun startCall(conversationId: String, kind: String, peerName: String) = viewModelScope.launch {
        if (activeCall.value != null) return@launch
        callPeerName.value = peerName
        runCatching { repo.startCall(conversationId, kind) }
            .onSuccess { startActiveCall(it, peerName, outgoing = true) }
    }

    /** Answer an incoming call (from the full-screen notification): fetch a join token. */
    fun acceptIncomingCall(callId: String, peerName: String) = viewModelScope.launch {
        callPeerName.value = peerName
        callStatus.value = "Connecting..."
        runCatching { repo.joinToken(callId) }
            .onSuccess { startActiveCall(it, peerName, outgoing = false) }
            .onFailure {
                socket.emit("call:decline", buildJsonObject { put("callId", callId) })
                callStatus.value = "Call failed"
                finishCall(delayMs = 1200)
            }
    }

    fun endCall() {
        val id = activeCall.value?.callId
        if (id != null) {
            socket.emit(
                if (callStatus.value == "Connected") "call:end" else "call:cancel",
                buildJsonObject { put("callId", id) },
            )
            viewModelScope.launch { repo.endCall(id) }
        }
        finishCall(callId = id)
    }

    fun onCallMediaJoined(callId: String) {
        if (activeCall.value?.callId != callId) return
        if (activeCallOutgoing) return
        callStatus.value = "Connected"
        socket.emit("call:accept", buildJsonObject { put("callId", callId) })
    }

    fun onCallJoinFailed(callId: String) {
        if (activeCall.value?.callId != callId) return
        callStatus.value = "Call failed"
        socket.emit("call:decline", buildJsonObject { put("callId", callId) })
        finishCall(delayMs = 1200, callId = callId)
    }

    private fun onAuthed() {
        isAuthenticated.value = true
        currentUser.value = repo.currentUser
        tokenStore.cachedAccess?.let { socket.connect(it) }
        loadConversations()
    }

    /** Emit a read receipt for the open conversation. */
    fun markRead(conversationId: String) {
        socket.emit("message:read", buildJsonObject { put("conversationId", conversationId) })
    }

    private fun startActiveCall(session: CallSession, peerName: String, outgoing: Boolean) {
        if (activeCall.value != null && activeCall.value?.callId != session.callId) return
        finishingCallIds.remove(session.callId)
        activeCallOutgoing = outgoing
        callPeerName.value = peerName
        callStatus.value = if (outgoing) "Calling..." else "Connecting..."
        activeCall.value = session
        if (outgoing) startRingTimeout(session.callId) else cancelRingTimeout()
    }

    private fun startRingTimeout(callId: String) {
        cancelRingTimeout()
        ringTimeoutJob = viewModelScope.launch {
            delay(45_000)
            if (activeCall.value?.callId == callId && activeCallOutgoing && callStatus.value == "Calling...") {
                callStatus.value = "No answer"
                socket.emit("call:cancel", buildJsonObject { put("callId", callId) })
                repo.endCall(callId)
                finishCall(delayMs = 1500, callId = callId)
            }
        }
    }

    private fun cancelRingTimeout() {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
    }

    private fun handleCallEvent(event: SocketService.CallEvent) {
        val currentId = activeCall.value?.callId ?: return
        if (event.callId != currentId) return
        when (event.type) {
            SocketService.CallEvent.Type.ACCEPT -> {
                cancelRingTimeout()
                callStatus.value = "Connected"
            }
            SocketService.CallEvent.Type.DECLINE -> {
                callStatus.value = "Busy"
                viewModelScope.launch { repo.endCall(currentId) }
                finishCall(delayMs = 1500, callId = currentId)
            }
            SocketService.CallEvent.Type.CANCEL,
            SocketService.CallEvent.Type.END -> finishCall(callId = currentId)
        }
    }

    private fun finishCall(delayMs: Long = 0, callId: String? = activeCall.value?.callId) {
        val id = callId ?: activeCall.value?.callId
        if (id != null && !finishingCallIds.add(id)) return
        cancelRingTimeout()
        if (id == null || activeCall.value?.callId == id) {
            activeCall.value = null
            activeCallOutgoing = false
            callStatus.value = "Ended"
        }
        viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            callManager.leave()
            if (id != null) finishingCallIds.remove(id)
        }
    }

    // Wraps a suspend auth call with error handling.
    private fun launchAuth(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { error.value = "Could not sign in. Check your details." }
    }
}
