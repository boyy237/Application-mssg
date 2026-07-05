package com.ciphertalk.messenger.ui.conversations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ciphertalk.messenger.data.model.User
import com.ciphertalk.messenger.databinding.ItemUserSearchBinding
import com.ciphertalk.messenger.util.initialOrQuestionMark
import com.ciphertalk.messenger.util.parseAvatarColor

class UserSearchAdapter(
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    private val items = mutableListOf<User>()

    fun submitList(newItems: List<User>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemUserSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textUsername.text = user.username
            binding.textAvatar.text = user.username.initialOrQuestionMark()
            binding.textAvatar.background.mutate().setTint(parseAvatarColor(user.avatarColor))
            binding.root.setOnClickListener { onClick(user) }
        }
    }
}
