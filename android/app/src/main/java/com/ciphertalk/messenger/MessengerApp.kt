package com.ciphertalk.messenger

import android.app.Application
import com.ciphertalk.messenger.data.local.SessionManager
import com.ciphertalk.messenger.data.remote.RetrofitClient

/**
 * Classe Application : initialise les singletons partagés (session utilisateur,
 * client réseau) dès le démarrage du processus.
 *
 * Récupération depuis une Activity : (application as MessengerApp).sessionManager
 */
class MessengerApp : Application() {

    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
    }
}
