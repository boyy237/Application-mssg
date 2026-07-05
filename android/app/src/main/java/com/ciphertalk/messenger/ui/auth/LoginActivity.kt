package com.ciphertalk.messenger.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ciphertalk.messenger.MessengerApp
import com.ciphertalk.messenger.data.repository.ApiResult
import com.ciphertalk.messenger.data.repository.AuthRepository
import com.ciphertalk.messenger.databinding.ActivityLoginBinding
import com.ciphertalk.messenger.ui.conversations.ConversationsActivity
import com.ciphertalk.messenger.util.gone
import com.ciphertalk.messenger.util.visible
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionManager = (application as MessengerApp).sessionManager
        authRepository = AuthRepository(sessionManager)

        if (sessionManager.isLoggedIn) {
            goToConversations()
            return
        }

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.linkRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = binding.inputUsername.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString()?.trim().orEmpty()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Merci de renseigner votre identifiant et votre mot de passe.")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = authRepository.login(username, password)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> goToConversations()
                is ApiResult.Failure -> showError(result.message)
            }
        }
    }

    private fun goToConversations() {
        startActivity(Intent(this, ConversationsActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visible()
    }

    private fun setLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
        if (loading) {
            binding.progressBar.visible()
            binding.textError.gone()
        } else {
            binding.progressBar.gone()
        }
    }
}
