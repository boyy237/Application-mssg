package com.ciphertalk.messenger.data.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class UserListResponse(val users: List<User>)

data class ConversationListResponse(val conversations: List<Conversation>)

data class ConversationResponse(val conversation: Conversation)

data class ConversationDetailWrapper(val conversation: ConversationDetail)

data class ConversationDetail(
    val id: Int,
    val isGroup: Boolean,
    val name: String?,
    val cipherA: Int,
    val cipherB: Int,
    val participants: List<User>
)

data class MessageListResponse(val messages: List<Message>)

data class MessageResponse(val message: Message)

data class CreateConversationRequest(val participantId: Int)

data class SendMessageRequest(val cipherText: String)

data class UpdateKeyRequest(val a: Int, val b: Int)

data class UpdateKeyResponse(val cipherA: Int, val cipherB: Int)

data class ApiErrorBody(val error: String?)
