package com.ciphertalk.messenger.ui.chat

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ciphertalk.messenger.R
import com.ciphertalk.messenger.data.local.LocalKeyStore
import com.ciphertalk.messenger.data.model.Message
import com.ciphertalk.messenger.databinding.ItemMessageReceivedBinding
import com.ciphertalk.messenger.databinding.ItemMessageSentBinding
import com.ciphertalk.messenger.util.AffineCipher
import com.ciphertalk.messenger.util.isoToTimeLabel

private const val TYPE_SENT = 1
private const val TYPE_RECEIVED = 2

/**
 * Adaptateur de messages avec déchiffrement local.
 *
 * Comportement :
 *  - Si une clé locale est disponible (LocalKeyStore) → le texte est déchiffré et
 *    affiché normalement.
 *  - Si aucune clé n'est saisie → le texte chiffré brut est affiché avec un cadenas 🔒
 *    et un style monospace pour indiquer que le message est illisible sans la clé.
 *
 * La clé n'est jamais envoyée au serveur.
 */
class MessageAdapter(
    private val currentUserId: Int,
    private val otherUsername: String,
    private val localKeyStore: LocalKeyStore,
    private val conversationId: Int,
    private val onLongPressDelete: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()

    var otherUserReadUpTo: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Clé locale active (peut être null si l'utilisateur n'a pas encore saisi la clé). */
    private var localKey: LocalKeyStore.CipherKey? = localKeyStore.getKey(conversationId)

    /** Appelé depuis ChatActivity quand l'utilisateur change sa clé locale. */
    fun refreshKey() {
        localKey = localKeyStore.getKey(conversationId)
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems.filter { !it.isDeleted })
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        if (items.any { it.id == message.id }) return
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun removeMessage(messageId: Int) {
        val index = items.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun lastMessageId(): Int? = items.lastOrNull()?.id

    override fun getItemViewType(position: Int): Int =
        if (items[position].senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT)
            SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        else
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        val key = localKey

        // Déchiffrement local : si la clé est disponible, on déchiffre — sinon on
        // affiche le texte chiffré brut pour signaler que la clé est requise.
        val displayText: String
        val isDecrypted: Boolean
        if (key != null) {
            displayText = try {
                AffineCipher.decrypt(message.cipherText, key.a, key.b)
            } catch (e: Exception) {
                message.cipherText
            }
            isDecrypted = true
        } else {
            displayText = message.cipherText
            isDecrypted = false
        }

        when (holder) {
            is SentViewHolder -> holder.bind(message, displayText, isDecrypted)
            is ReceivedViewHolder -> holder.bind(message, displayText, isDecrypted)
        }
    }

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, displayText: String, isDecrypted: Boolean) {
            binding.textPlain.text = if (isDecrypted) displayText else "🔒 $displayText"
            binding.textPlain.typeface =
                if (isDecrypted) Typeface.DEFAULT else Typeface.MONOSPACE
            binding.textPlain.alpha = if (isDecrypted) 1f else 0.6f

            binding.textTime.text = isoToTimeLabel(message.createdAt)

            val isRead = message.id <= otherUserReadUpTo
            binding.iconReadStatus.setImageResource(
                if (isRead) R.drawable.ic_done_all else R.drawable.ic_done
            )

            binding.root.setOnLongClickListener {
                onLongPressDelete(message)
                true
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, displayText: String, isDecrypted: Boolean) {
            binding.textSenderName.text = otherUsername
            binding.textPlain.text = if (isDecrypted) displayText else "🔒 $displayText"
            binding.textPlain.typeface =
                if (isDecrypted) Typeface.DEFAULT else Typeface.MONOSPACE
            binding.textPlain.alpha = if (isDecrypted) 1f else 0.6f
            binding.textTime.text = isoToTimeLabel(message.createdAt)
        }
    }
}
