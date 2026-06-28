package com.klic.app.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klic.app.calling.CallManager
import com.klic.app.data.CallSession
import com.klic.app.data.Conversation
import com.klic.app.data.KlicRepository
import com.klic.app.data.Message
import com.klic.app.data.TokenStore
import com.klic.app.data.User
import com.klic.app.realtime.SocketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KlicViewModel(
    private val repo: KlicRepository,
    private val tokenStore: TokenStore,
    private val socket: SocketService,
    val callManager: CallManager,
) : ViewModel() {

    val currentUser = MutableStateFlow<User?>(null)
    val isAuthenticated = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val messages = MutableStateFlow<List<Message>>(emptyList())
    val activeCall = MutableStateFlow<CallSession?>(null)

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
    }

    private var openConversationId: String? = null

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

    fun loadConversations() = viewModelScope.launch {
        runCatching { repo.conversations() }.onSuccess { conversations.value = it }
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

    fun startCall(conversationId: String, kind: String) = viewModelScope.launch {
        runCatching { repo.startCall(conversationId, kind) }.onSuccess { activeCall.value = it }
    }

    fun endCall() {
        callManager.leave()
        activeCall.value = null
    }

    private fun onAuthed() {
        isAuthenticated.value = true
        currentUser.value = repo.currentUser
        tokenStore.cachedAccess?.let { socket.connect(it) }
        loadConversations()
    }

    // Wraps a suspend auth call with error handling.
    private fun launchAuth(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { error.value = "Could not sign in. Check your details." }
    }
}
