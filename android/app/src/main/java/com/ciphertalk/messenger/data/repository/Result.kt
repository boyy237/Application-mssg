package com.ciphertalk.messenger.data.repository

/** Wrapper simple pour représenter le résultat (succès / échec) d'un appel réseau. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}
