package com.ciphertalk.messenger.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("avatarColor") val avatarColor: String,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("lastSeen") val lastSeen: String?
)
