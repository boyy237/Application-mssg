package com.ciphertalk.messenger.data.model

import com.google.gson.annotations.SerializedName

data class Message(
    val id: Int,
    @SerializedName("conversationId") val conversationId: Int,
    @SerializedName("senderId") val senderId: Int,
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("cipherA") val cipherA: Int,
    @SerializedName("cipherB") val cipherB: Int,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("createdAt") val createdAt: String
)
