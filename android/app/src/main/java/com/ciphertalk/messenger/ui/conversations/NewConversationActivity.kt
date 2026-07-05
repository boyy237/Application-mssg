package com.ciphertalk.messenger.ui.conversations

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ciphertalk.messenger.data.model.User
import com.ciphertalk.messenger.data.repository.ApiResult
import com.ciphertalk.messenger.data.repository.ConversationRepository
import com.ciphertalk.messenger.databinding.ActivityNewConversationBinding
import com.ciphertalk.messenger.ui.chat.ChatActivity
import com.ciphertalk.messenger.util.Constants
import com.ciphertalk.messenger.util.gone
import com.ciphertalk.messenger.util.toast
import com.ciphertalk.messenger.util.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewConversationBinding
    private val conversationRepository = ConversationRepository()
    private lateinit var adapter: UserSearchAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = UserSearchAdapter { user -> startConversationWith(user) }
        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerUsers.adapter = adapter

        binding.buttonBack.setOnClickListener { finish() }

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            adapter.submitList(emptyList())
            return
        }
        searchJob = lifecycleScope.launch {
            delay(Constants.USER_SEARCH_DEBOUNCE_MS)
            performSearch(query.trim())
        }
    }

    private fun performSearch(query: String) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = conversationRepository.searchUsers(query)
            binding.progressBar.gone()
            when (result) {
                is ApiResult.Success -> adapter.submitList(result.data)
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }

    private fun startConversationWith(user: User) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = conversationRepository.createOrGetConversation(user.id)
            binding.progressBar.gone()
            when (result) {
                is ApiResult.Success -> {
                    val intent = Intent(this@NewConversationActivity, ChatActivity::class.java).apply {
                        putExtra(Constants.EXTRA_CONVERSATION_ID, result.data.id)
                        putExtra(Constants.EXTRA_OTHER_USER_ID, user.id)
                        putExtra(Constants.EXTRA_OTHER_USERNAME, user.username)
                        putExtra(Constants.EXTRA_OTHER_AVATAR_COLOR, user.avatarColor)
                    }
                    startActivity(intent)
                    finish()
                }
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }
}
