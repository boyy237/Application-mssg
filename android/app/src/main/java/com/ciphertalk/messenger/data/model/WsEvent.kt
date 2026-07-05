package com.ciphertalk.messenger.data.model

/**
 * Enveloppe générique pour tous les événements reçus depuis le serveur WebSocket.
 * Le champ [type] indique quel(s) autre(s) champ(s) sont pertinents :
 *
 *  - "connected"        -> userId
 *  - "presence"         -> userId, isOnline
 *  - "message:new"      -> message
 *  - "message:deleted"  -> conversationId, messageId
 *  - "message:read"     -> conversationId, readerId, upToMessageId
 *  - "key:updated"      -> conversationId, cipherA, cipherB
 *  - "typing"           -> conversationId, userId, isTyping
 */
data class WsEnvelope(
    val type: String? = null,
    val userId: Int? = null,
    val isOnline: Boolean? = null,
    val message: Message? = null,
    val conversationId: Int? = null,
    val messageId: Int? = null,
    val readerId: Int? = null,
    val upToMessageId: Int? = null,
    val cipherA: Int? = null,
    val cipherB: Int? = null,
    val isTyping: Boolean? = null
)

object WsEventType {
    const val CONNECTED = "connected"
    const val PRESENCE = "presence"
    const val MESSAGE_NEW = "message:new"
    const val MESSAGE_DELETED = "message:deleted"
    const val MESSAGE_READ = "message:read"
    const val KEY_UPDATED = "key:updated"
    const val TYPING = "typing"
}
