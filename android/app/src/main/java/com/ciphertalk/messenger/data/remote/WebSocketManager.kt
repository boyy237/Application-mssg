package com.ciphertalk.messenger.data.remote

import com.ciphertalk.messenger.data.model.WsEnvelope
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response as OkResponse
import java.util.concurrent.TimeUnit

/**
 * Gère la connexion WebSocket temps réel vers le backend : réception des nouveaux
 * messages, présence en ligne, indicateur de frappe, mise à jour de la clé...
 *
 * Usage : WebSocketManager.connect(token) puis WebSocketManager.listener = { ... }
 */
object WebSocketManager {

    interface Listener {
        fun onEvent(event: WsEnvelope)
        fun onConnected() {}
        fun onDisconnected() {}
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var listener: Listener? = null

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    val isConnected: Boolean
        get() = webSocket != null

    fun connect(token: String) {
        disconnect()

        val request = Request.Builder()
            .url(RetrofitClient.webSocketUrl(token))
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: OkResponse) {
                listener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, WsEnvelope::class.java)
                    listener?.onEvent(event)
                } catch (e: Exception) {
                    // Message non reconnu : on l'ignore silencieusement.
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: OkResponse?) {
                listener?.onDisconnected()
            }
        })
    }

    /** Envoie l'indicateur "est en train d'écrire…" aux autres participants de la conversation. */
    fun sendTyping(conversationId: Int, isTyping: Boolean) {
        val payload = mapOf(
            "type" to "typing",
            "conversationId" to conversationId,
            "isTyping" to isTyping
        )
        webSocket?.send(gson.toJson(payload))
    }

    fun disconnect() {
        webSocket?.close(1000, "Déconnexion normale")
        webSocket = null
    }
}
