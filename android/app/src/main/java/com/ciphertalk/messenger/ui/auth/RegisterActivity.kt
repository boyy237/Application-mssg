package com.ciphertalk.messenger.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ciphertalk.messenger.MessengerApp
import com.ciphertalk.messenger.data.repository.ApiResult
import com.ciphertalk.messenger.data.repository.AuthRepository
import com.ciphertalk.messenger.databinding.ActivityRegisterBinding
import com.ciphertalk.messenger.ui.conversations.ConversationsActivity
import com.ciphertalk.messenger.util.gone
import com.ciphertalk.messenger.util.visible
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionManager = (application as MessengerApp).sessionManager
        authRepository = AuthRepository(sessionManager)

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val username = binding.inputUsername.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString()?.trim().orEmpty()

        if (username.length < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères.")
            return
        }
        if (!email.contains("@")) {
            showError("Adresse email invalide.")
            return
        }
        if (password.length < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = authRepository.register(username, email, password)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> {
                    startActivity(Intent(this@RegisterActivity, ConversationsActivity::class.java))
                    finishAffinity()
                }
                is ApiResult.Failure -> showError(result.message)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visible()
    }

    private fun setLoading(loading: Boolean) {
        binding.buttonRegister.isEnabled = !loading
        if (loading) {
            binding.progressBar.visible()
            binding.textError.gone()
        } else {
            binding.progressBar.gone()
        }
    }
}
