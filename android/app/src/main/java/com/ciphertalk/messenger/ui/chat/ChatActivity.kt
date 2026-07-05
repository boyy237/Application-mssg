package com.ciphertalk.messenger.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ciphertalk.messenger.MessengerApp
import com.ciphertalk.messenger.data.local.LocalKeyStore
import com.ciphertalk.messenger.data.model.ConversationDetail
import com.ciphertalk.messenger.data.model.Message
import com.ciphertalk.messenger.data.model.WsEnvelope
import com.ciphertalk.messenger.data.model.WsEventType
import com.ciphertalk.messenger.data.remote.WebSocketManager
import com.ciphertalk.messenger.data.repository.ApiResult
import com.ciphertalk.messenger.data.repository.ConversationRepository
import com.ciphertalk.messenger.data.repository.MessageRepository
import com.ciphertalk.messenger.databinding.ActivityChatBinding
import com.ciphertalk.messenger.util.AffineCipher
import com.ciphertalk.messenger.util.Constants
import com.ciphertalk.messenger.util.initialOrQuestionMark
import com.ciphertalk.messenger.util.invisible
import com.ciphertalk.messenger.util.parseAvatarColor
import com.ciphertalk.messenger.util.setVisible
import com.ciphertalk.messenger.util.toast
import com.ciphertalk.messenger.util.visible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity(), WebSocketManager.Listener {

    private lateinit var binding: ActivityChatBinding
    private val conversationRepository = ConversationRepository()
    private val messageRepository = MessageRepository()
    private lateinit var adapter: MessageAdapter
    private lateinit var localKeyStore: LocalKeyStore

    private var conversationId: Int = -1
    private var otherUserId: Int = -1
    private var otherUsername: String = ""
    private var currentUserId: Int = -1

    // Clé locale de CHIFFREMENT (utilisée pour chiffrer les messages ENVOYÉS)
    // Par défaut (7, 3) tant que l'utilisateur n'a pas saisi sa propre clé.
    private var sendKeyA: Int = 7
    private var sendKeyB: Int = 3

    private var isTypingSent = false
    private var typingResetJob: Job? = null
    private var hideTypingIndicatorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionManager = (application as MessengerApp).sessionManager
        currentUserId = sessionManager.currentUser?.id ?: -1


        conversationId = intent.getIntExtra(Constants.EXTRA_CONVERSATION_ID, -1)
        otherUserId = intent.getIntExtra(Constants.EXTRA_OTHER_USER_ID, -1)
        otherUsername = intent.getStringExtra(Constants.EXTRA_OTHER_USERNAME).orEmpty()
        val avatarColor = intent.getStringExtra(Constants.EXTRA_OTHER_AVATAR_COLOR)

        localKeyStore = LocalKeyStore(this)
        localKeyStore.getKey(conversationId)?.let {
            sendKeyA = it.a
            sendKeyB = it.b
        }


        if (conversationId == -1 || currentUserId == -1) {
            toast("Conversation invalide.")
            finish()
            return
        }

        binding.textName.text = otherUsername
        binding.textAvatar.text = otherUsername.initialOrQuestionMark()
        binding.textAvatar.background.mutate().setTint(parseAvatarColor(avatarColor))

        // Charge la clé locale si elle existe déjà

        adapter = MessageAdapter(
            currentUserId = currentUserId,
            otherUsername = otherUsername,
            localKeyStore = localKeyStore,
            conversationId = conversationId,
            onLongPressDelete = { message -> confirmDelete(message) }
        )

        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.layoutManager = layoutManager
        binding.recyclerMessages.adapter = adapter

        binding.buttonBack.setOnClickListener { finish() }

        // Bouton clé : ouvre le dialogue de saisie de clé locale
        binding.buttonKeySettings.setOnClickListener { openKeySettings() }

        binding.buttonSend.setOnClickListener { sendCurrentMessage() }

        binding.inputMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                notifyTyping()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Affiche un bandeau si aucune clé n'a encore été saisie
        if (!localKeyStore.hasKey(conversationId)) {
            showNoCipherKeyBanner()
        }

        loadConversationDetail()
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        WebSocketManager.setListener(this)
        markRead()
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.setListener(null)
        sendTypingState(false)
    }

    /** Affiche un bandeau discret invitant l'utilisateur à saisir la clé partagée. */
    private fun showNoCipherKeyBanner() {
        binding.typingIndicator.text =
            "🔒 Aucune clé saisie — appuyez sur ⚙ pour déchiffrer les messages"
        binding.typingIndicator.visible()
    }

    private fun loadConversationDetail() {
        lifecycleScope.launch {
            val result = conversationRepository.getConversation(conversationId)
            if (result is ApiResult.Success) {
                applyConversationDetail(result.data)
            }
        }
    }

    private fun applyConversationDetail(detail: ConversationDetail) {
        val other = detail.participants.firstOrNull()
        if (other != null) {
            binding.dotOnline.setVisible(other.isOnline)
            binding.textStatus.text = if (other.isOnline) "En ligne" else "Hors ligne"
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val result = messageRepository.getHistory(conversationId)
            when (result) {
                is ApiResult.Success -> {
                    adapter.submitList(result.data)
                    scrollToBottom()
                }
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }

    /**
     * Envoie un message :
     *  1. L'utilisateur tape son message EN CLAIR.
     *  2. L'app chiffre avec la clé locale (a, b).
     *  3. Seul le texte chiffré est envoyé et stocké sur le serveur.
     *  4. Le destinataire doit avoir la même clé pour déchiffrer.
     */
    private fun sendCurrentMessage() {
        val plainText = binding.inputMessage.text?.toString()?.trim().orEmpty()
        if (plainText.isEmpty()) return

        // Vérifie qu'une clé est bien configurée avant d'envoyer
        if (!localKeyStore.hasKey(conversationId)) {
            AlertDialog.Builder(this)
                .setTitle("Clé requise")
                .setMessage(
                    "Vous n'avez pas encore saisi de clé de chiffrement.\n\n" +
                            "Appuyez sur ⚙ pour saisir une clé (a, b), puis partagez-la " +
                            "avec votre interlocuteur pour qu'il puisse déchiffrer vos messages."
                )
                .setPositiveButton("Saisir la clé") { _, _ -> openKeySettings() }
                .setNegativeButton("Envoyer quand même (non chiffré)") { _, _ ->
                    // Envoie le texte brut sans chiffrement (fallback)
                    doSend(plainText)
                }
                .show()
            return
        }

        val cipherText = try {
            AffineCipher.encrypt(plainText, sendKeyA, sendKeyB)
        } catch (e: Exception) {
            plainText // fallback si clé invalide
        }

        doSend(cipherText)
    }

    private fun doSend(cipherText: String) {
        binding.inputMessage.text?.clear()
        sendTypingState(false)

        lifecycleScope.launch {
            val result = messageRepository.sendMessage(conversationId, cipherText)
            when (result) {
                is ApiResult.Success -> {
                    adapter.addMessage(result.data)
                    scrollToBottom()
                }
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }

    private fun confirmDelete(message: Message) {
        AlertDialog.Builder(this)
            .setTitle(getString(com.ciphertalk.messenger.R.string.delete_message))
            .setMessage(getString(com.ciphertalk.messenger.R.string.confirm_delete_message))
            .setNegativeButton(com.ciphertalk.messenger.R.string.cancel, null)
            .setPositiveButton(com.ciphertalk.messenger.R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    val result = messageRepository.deleteMessage(conversationId, message.id)
                    if (result is ApiResult.Success) {
                        adapter.removeMessage(message.id)
                    } else if (result is ApiResult.Failure) {
                        toast(result.message)
                    }
                }
            }
            .show()
    }

    /**
     * Ouvre le dialogue de saisie de clé.
     * La clé est sauvegardée LOCALEMENT — jamais envoyée au serveur.
     * Une fois appliquée, tous les messages sont re-rendus (déchiffrés ou pas).
     */
    private fun openKeySettings() {
        showKeySettingsDialog(
            context = this,
            conversationId = conversationId,
            localKeyStore = localKeyStore,
            onKeyApplied = { a, b ->
                sendKeyA = a
                sendKeyB = b
                // Re-render tous les messages avec la nouvelle clé
                adapter.refreshKey()
                // Cache le bandeau d'avertissement si affiché
                if (binding.typingIndicator.text.toString().startsWith("🔒")) {
                    binding.typingIndicator.invisible()
                }
                toast("Clé saisie — les messages sont maintenant déchiffrés.")
            },
            onKeyClear = {
                localKeyStore.clearKey(conversationId)
                adapter.refreshKey()
                showNoCipherKeyBanner()
                toast("Clé supprimée — les messages apparaissent à nouveau chiffrés.")
            }
        )
    }

    private fun markRead() {
        lifecycleScope.launch {
            conversationRepository.markRead(conversationId)
        }
    }

    private fun scrollToBottom() {
        if (adapter.itemCount > 0) {
            binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun notifyTyping() {
        sendTypingState(true)
        typingResetJob?.cancel()
        typingResetJob = lifecycleScope.launch {
            delay(Constants.TYPING_TIMEOUT_MS)
            sendTypingState(false)
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        if (isTyping == isTypingSent) return
        isTypingSent = isTyping
        WebSocketManager.sendTyping(conversationId, isTyping)
    }

    private fun showTypingIndicator(show: Boolean) {
        hideTypingIndicatorJob?.cancel()
        // Ne pas écraser le bandeau "aucune clé" avec l'indicateur de frappe
        if (!localKeyStore.hasKey(conversationId) && !show) return
        if (show) {
            binding.typingIndicator.text = "$otherUsername est en train d'écrire…"
            binding.typingIndicator.visible()
            hideTypingIndicatorJob = lifecycleScope.launch {
                delay(Constants.TYPING_TIMEOUT_MS + 500)
                binding.typingIndicator.invisible()
                // Reaffiche le bandeau si pas de clé
                if (!localKeyStore.hasKey(conversationId)) showNoCipherKeyBanner()
            }
        } else {
            binding.typingIndicator.invisible()
            if (!localKeyStore.hasKey(conversationId)) showNoCipherKeyBanner()
        }
    }

    // --- WebSocketManager.Listener
    override fun onEvent(event: WsEnvelope) {
        runOnUiThread {
            when (event.type) {
                WsEventType.MESSAGE_NEW -> {
                    val message = event.message
                    if (message != null && message.conversationId == conversationId) {
                        adapter.addMessage(message)
                        scrollToBottom()
                        if (message.senderId != currentUserId) markRead()
                    }
                }
                WsEventType.MESSAGE_DELETED -> {
                    if (event.conversationId == conversationId && event.messageId != null) {
                        adapter.removeMessage(event.messageId)
                    }
                }
                WsEventType.MESSAGE_READ -> {
                    if (event.conversationId == conversationId &&
                        event.readerId != currentUserId &&
                        event.upToMessageId != null
                    ) {
                        adapter.otherUserReadUpTo = event.upToMessageId
                    }
                }
                WsEventType.PRESENCE -> {
                    if (event.userId == otherUserId) {
                        val online = event.isOnline == true
                        binding.dotOnline.setVisible(online)
                        binding.textStatus.text = if (online) "En ligne" else "Hors ligne"
                    }
                }
                WsEventType.TYPING -> {
                    if (event.conversationId == conversationId && event.userId == otherUserId) {
                        showTypingIndicator(event.isTyping == true)
                    }
                }
            }
        }
    }
}
