package com.ciphertalk.messenger.data.repository

import com.ciphertalk.messenger.data.local.SessionManager
import com.ciphertalk.messenger.data.model.LoginRequest
import com.ciphertalk.messenger.data.model.RegisterRequest
import com.ciphertalk.messenger.data.model.User
import com.ciphertalk.messenger.data.remote.RetrofitClient
import com.ciphertalk.messenger.util.errorMessage

class AuthRepository(private val sessionManager: SessionManager) {

    private val api get() = RetrofitClient.api

    suspend fun register(username: String, email: String, password: String): ApiResult<User> {
        return try {
            val response = api.register(RegisterRequest(username, email, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                sessionManager.token = body.token
                sessionManager.currentUser = body.user
                ApiResult.Success(body.user)
            } else {
                ApiResult.Failure(response.errorMessage("Inscription impossible."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    suspend fun login(usernameOrEmail: String, password: String): ApiResult<User> {
        return try {
            val response = api.login(LoginRequest(usernameOrEmail, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                sessionManager.token = body.token
                sessionManager.currentUser = body.user
                ApiResult.Success(body.user)
            } else {
                ApiResult.Failure(response.errorMessage("Identifiants invalides."))
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Impossible de contacter le serveur.")
        }
    }

    fun logout() {
        sessionManager.clear()
    }
}
