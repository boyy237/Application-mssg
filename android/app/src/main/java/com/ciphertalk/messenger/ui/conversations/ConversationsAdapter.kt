package com.ciphertalk.messenger.ui.conversations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ciphertalk.messenger.data.model.Conversation
import com.ciphertalk.messenger.databinding.ItemConversationBinding
import com.ciphertalk.messenger.util.AffineCipher
import com.ciphertalk.messenger.util.initialOrQuestionMark
import com.ciphertalk.messenger.util.isoToTimeLabel
import com.ciphertalk.messenger.util.parseAvatarColor
import com.ciphertalk.messenger.util.setVisible

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    private val items = mutableListOf<Conversation>()

    fun submitList(newItems: List<Conversation>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val other = conversation.otherParticipant()
            val displayName = other?.username ?: conversation.name ?: "Conversation"

            binding.textName.text = displayName
            binding.textAvatar.text = displayName.initialOrQuestionMark()
            binding.textAvatar.background.mutate().setTint(parseAvatarColor(other?.avatarColor))

            binding.dotOnline.setVisible(other?.isOnline == true)

            val lastMessage = conversation.lastMessage
            binding.textPreview.text = if (lastMessage != null) {
                try {
                    AffineCipher.decrypt(lastMessage.cipherText, lastMessage.cipherA, lastMessage.cipherB)
                } catch (e: Exception) {
                    lastMessage.cipherText
                }
            } else {
                "Aucun message pour le moment"
            }

            binding.textTime.text = isoToTimeLabel(lastMessage?.createdAt)

            val unread = conversation.unreadCount
            binding.textUnreadBadge.setVisible(unread > 0)
            if (unread > 0) {
                binding.textUnreadBadge.text = if (unread > 99) "99+" else unread.toString()
            }

            binding.root.setOnClickListener { onClick(conversation) }
        }
    }
}
