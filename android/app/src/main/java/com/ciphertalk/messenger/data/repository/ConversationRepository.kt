package com.ciphertalk.messenger.data.repository

import com.ciphertalk.messenger.data.model.Conversation
import com.ciphertalk.messenger.data.model.ConversationDetail
import com.ciphertalk.messenger.data.model.CreateConversationRequest
import com.ciphertalk.messenger.data.model.UpdateKeyRequest
import com.ciphertalk.messenger.data.model.UpdateKeyResponse
import com.ciphertalk.messenger.data.model.User
import com.ciphertalk.messenger.data.remote.RetrofitClient
import com.ciphertalk.messenger.util.errorMessage

class ConversationRepository {

    private val api get() = RetrofitClient.api

    suspend fun listConversations(): ApiResult<List<Conversation>> {
        return try {
            val response = api.listConversations()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.conversations)
            } else {
                ApiResult.Failure(response.errorMessage("Impossible de charger les conversations."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun searchUsers(query: String): ApiResult<List<User>> {
        return try {
            val response = api.searchUsers(query)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.users)
            } else {
                ApiResult.Failure(response.errorMessage("Recherche impossible."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun createOrGetConversation(participantId: Int): ApiResult<Conversation> {
        return try {
            val response = api.createOrGetConversation(CreateConversationRequest(participantId))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.conversation)
            } else {
                ApiResult.Failure(response.errorMessage("Impossible de créer la conversation."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun getConversation(id: Int): ApiResult<ConversationDetail> {
        return try {
            val response = api.getConversation(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.conversation)
            } else {
                ApiResult.Failure(response.errorMessage("Conversation introuvable."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun updateKey(conversationId: Int, a: Int, b: Int): ApiResult<UpdateKeyResponse> {
        return try {
            val response = api.updateKey(conversationId, UpdateKeyRequest(a, b))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Failure(response.errorMessage("Clé invalide."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun markRead(conversationId: Int): ApiResult<Unit> {
        return try {
            val response = api.markRead(conversationId)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Failure(response.errorMessage())
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }
}
