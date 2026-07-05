package com.ciphertalk.messenger.data.remote

import com.ciphertalk.messenger.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Définition des endpoints REST exposés par le backend Node.js / Express.
 * Toutes les routes (sauf register/login) nécessitent l'en-tête
 * "Authorization: Bearer <token>", ajouté automatiquement par AuthInterceptor.
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<UserListResponse>

    @GET("api/conversations")
    suspend fun listConversations(): Response<ConversationListResponse>

    @POST("api/conversations")
    suspend fun createOrGetConversation(@Body request: CreateConversationRequest): Response<ConversationResponse>

    @GET("api/conversations/{id}")
    suspend fun getConversation(@Path("id") id: Int): Response<ConversationDetailWrapper>

    @PATCH("api/conversations/{id}/key")
    suspend fun updateKey(@Path("id") id: Int, @Body request: UpdateKeyRequest): Response<UpdateKeyResponse>

    @POST("api/conversations/{id}/read")
    suspend fun markRead(@Path("id") id: Int): Response<Unit>

    @GET("api/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: Int,
        @Query("before") before: Int? = null,
        @Query("limit") limit: Int = 50
    ): Response<MessageListResponse>

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: Int, @Body request: SendMessageRequest): Response<MessageResponse>

    @DELETE("api/conversations/{id}/messages/{messageId}")
    suspend fun deleteMessage(@Path("id") id: Int, @Path("messageId") messageId: Int): Response<Unit>
}
