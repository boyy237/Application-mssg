package com.ciphertalk.messenger.ui.chat

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import com.ciphertalk.messenger.R
import com.ciphertalk.messenger.data.local.LocalKeyStore
import com.ciphertalk.messenger.databinding.DialogKeySettingsBinding
import com.ciphertalk.messenger.util.AffineCipher
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Boîte de dialogue permettant à chaque utilisateur de saisir SA clé locale
 * pour déchiffrer les messages d'une conversation.
 *
 * La clé n'est JAMAIS envoyée au serveur — elle est stockée uniquement sur
 * l'appareil via LocalKeyStore.
 *
 * @param conversationId  Identifiant de la conversation concernée.
 * @param localKeyStore   Accès au stockage local des clés.
 * @param onKeyApplied    Callback appelé quand la clé est validée et sauvegardée.
 */
fun showKeySettingsDialog(
    context: Context,
    conversationId: Int,
    localKeyStore: LocalKeyStore,
    onKeyApplied: (a: Int, b: Int) -> Unit,
    onKeyClear: () -> Unit
) {
    val dialog = BottomSheetDialog(context)
    val binding = DialogKeySettingsBinding.inflate(dialog.layoutInflater)
    dialog.setContentView(binding.root)

    // Pré-remplir avec la clé actuelle si elle existe
    val existingKey = localKeyStore.getKey(conversationId)
    binding.inputA.setText((existingKey?.a ?: 7).toString())
    binding.inputB.setText((existingKey?.b ?: 3).toString())

    fun currentInputs(): Pair<Int?, Int?> {
        val a = binding.inputA.text?.toString()?.trim()?.toIntOrNull()
        val b = binding.inputB.text?.toString()?.trim()?.toIntOrNull()
        return a to b
    }

    fun refresh() {
        val (a, b) = currentInputs()
        if (a == null || b == null) {
            binding.textValidation.text = "Saisissez deux nombres entiers."
            binding.textValidation.setTextColor(context.getColor(R.color.text_dim))
            binding.buttonApply.isEnabled = false
            return
        }

        val valid = AffineCipher.isValidKey(a, b)
        if (valid) {
            binding.textValidation.text = "✓ Clé valide — pgcd($a, 26) = 1"
            binding.textValidation.setTextColor(context.getColor(R.color.accent))
            val aInv = AffineCipher.modInverse(a)
            binding.textEncFormula.text = "E(x) = ($a·x + $b) mod 26"
            binding.textDecFormula.text = "D(y) = $aInv·(y − $b) mod 26"
        } else {
            binding.textValidation.text = "✗ Clé invalide : pgcd(a, 26) doit valoir 1 et 0 ≤ b ≤ 25."
            binding.textValidation.setTextColor(context.getColor(R.color.danger))
            binding.textEncFormula.text = "E(x) = ($a·x + $b) mod 26"
            binding.textDecFormula.text = "—"
        }

        // Table de substitution
        binding.rowPlain.removeAllViews()
        binding.rowCipher.removeAllViews()
        val table = AffineCipher.buildSubstitutionTable(a, b)
        for ((plain, cipher) in table) {
            binding.rowPlain.addView(buildTableCell(context, plain.toString(), R.color.text_dim))
            binding.rowCipher.addView(buildTableCell(context, cipher.toString(), R.color.accent))
        }

        binding.buttonApply.isEnabled = valid
    }

    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
        override fun afterTextChanged(s: Editable?) {}
    }
    binding.inputA.addTextChangedListener(watcher)
    binding.inputB.addTextChangedListener(watcher)

    // Génère une clé aléatoire valide (pour celui qui initie la conversation)
    binding.buttonGenerate.setOnClickListener {
        val (a, b) = AffineCipher.generateRandomKey()
        binding.inputA.setText(a.toString())
        binding.inputB.setText(b.toString())
    }

    // Applique la clé localement (sans appel serveur)
    binding.buttonApply.setOnClickListener {
        val (a, b) = currentInputs()
        if (a != null && b != null && AffineCipher.isValidKey(a, b)) {
            localKeyStore.saveKey(conversationId, a, b)
            onKeyApplied(a, b)
            dialog.dismiss()
        }
    }

    refresh()
    dialog.show()
}

private fun buildTableCell(context: Context, text: String, colorRes: Int): TextView {
    return TextView(context).apply {
        this.text = text
        setTextColor(context.getColor(colorRes))
        textSize = 12f
        setPadding(10, 4, 10, 4)
        minWidth = 28
        gravity = android.view.Gravity.CENTER
    }
}
