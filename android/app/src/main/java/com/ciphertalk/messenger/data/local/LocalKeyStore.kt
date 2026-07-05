package com.ciphertalk.messenger.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Stocke les clés de chiffrement affine (a, b) LOCALEMENT sur l'appareil,
 * par conversation — elles ne sont JAMAIS envoyées au serveur.
 *
 * Principe de sécurité :
 *   - Le serveur ne stocke que le texte chiffré.
 *   - Seuls les utilisateurs qui connaissent la clé (partagée hors-application,
 *     par exemple de vive voix ou par SMS sécurisé) peuvent lire les messages.
 *   - Chaque utilisateur saisit lui-même la clé dans les paramètres de la
 *     conversation pour déchiffrer les messages reçus.
 */
class LocalKeyStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ciphertalk_keys", Context.MODE_PRIVATE)

    data class CipherKey(val a: Int, val b: Int)

    /** Enregistre la clé (a, b) pour une conversation donnée. */
    fun saveKey(conversationId: Int, a: Int, b: Int) {
        prefs.edit()
            .putInt("key_a_$conversationId", a)
            .putInt("key_b_$conversationId", b)
            .putBoolean("key_set_$conversationId", true)
            .apply()
    }

    /** Renvoie la clé pour une conversation, ou null si aucune clé n'a été saisie. */
    fun getKey(conversationId: Int): CipherKey? {
        if (!prefs.getBoolean("key_set_$conversationId", false)) return null
        val a = prefs.getInt("key_a_$conversationId", 7)
        val b = prefs.getInt("key_b_$conversationId", 3)
        return CipherKey(a, b)
    }

    /** Supprime la clé locale d'une conversation (messages redeviendront illisibles). */
    fun clearKey(conversationId: Int) {
        prefs.edit()
            .remove("key_a_$conversationId")
            .remove("key_b_$conversationId")
            .remove("key_set_$conversationId")
            .apply()
    }

    /** Indique si une clé a déjà été saisie pour cette conversation. */
    fun hasKey(conversationId: Int): Boolean =
        prefs.getBoolean("key_set_$conversationId", false)
}
