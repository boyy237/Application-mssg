package com.ciphertalk.messenger.ui.conversations

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ciphertalk.messenger.MessengerApp
import com.ciphertalk.messenger.data.local.SessionManager
import com.ciphertalk.messenger.data.model.WsEventType
import com.ciphertalk.messenger.data.remote.WebSocketManager
import com.ciphertalk.messenger.data.repository.ApiResult
import com.ciphertalk.messenger.data.repository.AuthRepository
import com.ciphertalk.messenger.data.repository.ConversationRepository
import com.ciphertalk.messenger.databinding.ActivityConversationsBinding
import com.ciphertalk.messenger.ui.auth.LoginActivity
import com.ciphertalk.messenger.ui.chat.ChatActivity
import com.ciphertalk.messenger.util.Constants
import com.ciphertalk.messenger.util.setVisible
import com.ciphertalk.messenger.util.toast
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity(), WebSocketManager.Listener {

    private lateinit var binding: ActivityConversationsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private val conversationRepository = ConversationRepository()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = (application as MessengerApp).sessionManager
        authRepository = AuthRepository(sessionManager)

        if (!sessionManager.isLoggedIn) {
            goToLogin()
            return
        }

        adapter = ConversationsAdapter { conversation ->
            val other = conversation.otherParticipant()
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(Constants.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(Constants.EXTRA_OTHER_USER_ID, other?.id ?: -1)
                putExtra(Constants.EXTRA_OTHER_USERNAME, other?.username ?: conversation.name.orEmpty())
                putExtra(Constants.EXTRA_OTHER_AVATAR_COLOR, other?.avatarColor)
            }
            startActivity(intent)
        }
        binding.recyclerConversations.layoutManager = LinearLayoutManager(this)
        binding.recyclerConversations.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadConversations() }
        binding.buttonNewConversation.setOnClickListener {
            startActivity(Intent(this, NewConversationActivity::class.java))
        }
        binding.buttonLogout.setOnClickListener { logout() }

        sessionManager.token?.let { token ->
            if (!WebSocketManager.isConnected) {
                WebSocketManager.connect(token)
            }
        }

        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        WebSocketManager.setListener(this)
        loadConversations()
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.setListener(null)
    }

    private fun loadConversations() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val result = conversationRepository.listConversations()
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is ApiResult.Success -> {
                    adapter.submitList(result.data)
                    binding.textEmpty.setVisible(result.data.isEmpty())
                }
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }

    private fun logout() {
        WebSocketManager.disconnect()
        authRepository.logout()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    // --- WebSocketManager.Listener : on rafraîchit simplement la liste des
    // conversations quand un événement pertinent arrive pendant que cet écran est visible.
    override fun onEvent(event: com.ciphertalk.messenger.data.model.WsEnvelope) {
        when (event.type) {
            WsEventType.MESSAGE_NEW, WsEventType.PRESENCE, WsEventType.MESSAGE_READ -> {
                runOnUiThread { loadConversations() }
            }
        }
    }
}
