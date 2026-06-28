package com.klicmobile.app.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

@Serializable
data class RegisterRequest(val username: String, val password: String, val displayName: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class Member(val id: String, val username: String, val displayName: String)

@Serializable
data class Conversation(
    val id: String,
    val type: String,
    val members: List<Member> = emptyList(),
    val lastMessage: Message? = null,
)

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val kind: String = "TEXT",
    val createdAt: String = "",
)

@Serializable
data class RequestFrom(val id: String, val username: String, val displayName: String)

@Serializable
data class FriendRequest(val requestId: String, val from: RequestFrom)

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class StartCallRequest(val conversationId: String, val kind: String)

@Serializable
data class CallSession(
    val callId: String,
    val roomName: String,
    val livekitUrl: String,
    val token: String,
    val kind: String? = null,
)
