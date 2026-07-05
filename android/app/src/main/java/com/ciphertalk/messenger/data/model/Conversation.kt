package com.ciphertalk.messenger.data.model

import com.google.gson.annotations.SerializedName

data class ConversationPreviewMessage(
    val id: Int,
    @SerializedName("senderId") val senderId: Int,
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("cipherA") val cipherA: Int,
    @SerializedName("cipherB") val cipherB: Int,
    @SerializedName("createdAt") val createdAt: String
)

data class Conversation(
    val id: Int,
    @SerializedName("isGroup") val isGroup: Boolean,
    val name: String?,
    @SerializedName("cipherA") val cipherA: Int,
    @SerializedName("cipherB") val cipherB: Int,
    val participants: List<User>,
    @SerializedName("lastMessage") val lastMessage: ConversationPreviewMessage?,
    @SerializedName("unreadCount") val unreadCount: Int = 0
) {
    /** Pour une conversation 1-à-1, renvoie l'autre participant (l'interlocuteur). */
    fun otherParticipant(): User? = participants.firstOrNull()
}
