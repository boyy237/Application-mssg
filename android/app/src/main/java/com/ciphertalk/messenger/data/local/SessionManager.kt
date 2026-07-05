package com.ciphertalk.messenger.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ciphertalk.messenger.data.model.User
import com.google.gson.Gson

/**
 * Stocke localement le token JWT et le profil de l'utilisateur connecté
 * (SharedPreferences). Pour une sécurité renforcée en production, on peut
 * remplacer ceci par androidx.security:security-crypto (EncryptedSharedPreferences).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ciphertalk_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var currentUser: User?
        get() {
            val json = prefs.getString(KEY_USER, null) ?: return null
            return try {
                gson.fromJson(json, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            prefs.edit().putString(KEY_USER, value?.let { gson.toJson(it) }).apply()
        }

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
    }
}




