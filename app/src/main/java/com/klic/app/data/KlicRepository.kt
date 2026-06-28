package com.klic.app.data

/** Single entry point for the UI to reach the API + token storage. */
class KlicRepository(
    private val api: KlicApi,
    private val tokenStore: TokenStore,
) {
    var currentUser: User? = null
        private set

    val isAuthenticated: Boolean get() = tokenStore.cachedAccess != null

    suspend fun register(username: String, password: String, displayName: String): User {
        val res = api.register(RegisterRequest(username, password, displayName))
        persist(res)
        return res.user
    }

    suspend fun login(username: String, password: String): User {
        val res = api.login(LoginRequest(username, password))
        persist(res)
        return res.user
    }

    suspend fun logout() {
        tokenStore.clear()
        currentUser = null
    }

    suspend fun conversations(): List<Conversation> = api.conversations()
    suspend fun messages(conversationId: String): List<Message> = api.messages(conversationId)
    suspend fun send(conversationId: String, body: String): Message =
        api.send(conversationId, SendMessageRequest(body))

    suspend fun startCall(conversationId: String, kind: String): CallSession =
        api.startCall(StartCallRequest(conversationId, kind))

    suspend fun joinToken(callId: String): CallSession = api.joinToken(callId)

    private suspend fun persist(res: AuthResponse) {
        tokenStore.save(res.accessToken, res.refreshToken)
        currentUser = res.user
    }
}
