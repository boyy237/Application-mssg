package com.ciphertalk.messenger.util

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.setVisible(isVisible: Boolean) { visibility = if (isVisible) View.VISIBLE else View.GONE }

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

/** Convertit un horodatage ISO-8601 (envoyé par le backend) en heure locale "HH:mm". */
fun isoToTimeLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date = parser.parse(iso.substring(0, 19)) ?: return ""
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

/** Couleur de remplacement si le serveur ne renvoie pas de couleur d'avatar valide. */
fun parseAvatarColor(hex: String?): Int {
    return try {
        Color.parseColor(hex)
    } catch (e: Exception) {
        Color.parseColor("#4AF0C4")
    }
}

fun String.initialOrQuestionMark(): String {
    return this.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
