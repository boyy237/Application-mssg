package com.ciphertalk.messenger.util

import com.ciphertalk.messenger.data.model.ApiErrorBody
import com.google.gson.Gson
import retrofit2.Response

/**
 * Extrait un message d'erreur lisible depuis une réponse HTTP non réussie
 * (le backend renvoie toujours { "error": "..." } en cas d'échec).
 */
fun <T> Response<T>.errorMessage(default: String = "Une erreur est survenue."): String {
    return try {
        val raw = errorBody()?.string()
        if (raw.isNullOrBlank()) return default
        Gson().fromJson(raw, ApiErrorBody::class.java)?.error ?: default
    } catch (e: Exception) {
        default
    }
}
