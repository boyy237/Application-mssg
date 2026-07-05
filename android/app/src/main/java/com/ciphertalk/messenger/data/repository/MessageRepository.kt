package com.ciphertalk.messenger.data.repository

import com.ciphertalk.messenger.data.model.Message
import com.ciphertalk.messenger.data.model.SendMessageRequest
import com.ciphertalk.messenger.data.remote.RetrofitClient
import com.ciphertalk.messenger.util.errorMessage

class MessageRepository {

    private val api get() = RetrofitClient.api

    suspend fun getHistory(conversationId: Int, before: Int? = null, limit: Int = 50): ApiResult<List<Message>> {
        return try {
            val response = api.getMessages(conversationId, before, limit)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.messages)
            } else {
                ApiResult.Failure(response.errorMessage("Impossible de charger les messages."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun sendMessage(conversationId: Int, cipherText: String): ApiResult<Message> {
        return try {
            val response = api.sendMessage(conversationId, SendMessageRequest(cipherText))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.message)
            } else {
                ApiResult.Failure(response.errorMessage("Envoi impossible."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun deleteMessage(conversationId: Int, messageId: Int): ApiResult<Unit> {
        return try {
            val response = api.deleteMessage(conversationId, messageId)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Failure(response.errorMessage("Suppression impossible."))
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }
}
