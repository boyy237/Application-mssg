package com.ciphertalk.messenger.util

import kotlin.random.Random

/**
 * Chiffrement affine — repris à l'identique du concept de la démo web originale.
 *
 *   Chiffrement   : E(x) = (a·x + b) mod 26
 *   Déchiffrement : D(y) = a⁻¹·(y − b) mod 26
 *
 * Seules les lettres A-Z (sans accents) sont chiffrées ; tout le reste
 * (espaces, ponctuation, chiffres, accents) est conservé tel quel, exactement
 * comme dans la démo HTML d'origine.
 */
object AffineCipher {

    private const val M = 26

    /** Valeurs de "a" valides : doivent être premières avec 26 (pgcd(a, 26) = 1). */
    val VALID_A_VALUES = listOf(1, 3, 5, 7, 9, 11, 15, 17, 19, 21, 23, 25)

    fun isValidKey(a: Int, b: Int): Boolean {
        return a in VALID_A_VALUES && b in 0 until M
    }

    fun generateRandomKey(): Pair<Int, Int> {
        val a = VALID_A_VALUES.random(Random)
        val b = Random.nextInt(0, M)
        return a to b
    }

    private fun gcd(x: Int, y: Int): Int = if (y == 0) x else gcd(y, x % y)

    fun modInverse(a: Int): Int {
        val aMod = ((a % M) + M) % M
        for (x in 1 until M) {
            if ((aMod * x) % M == 1) return x
        }
        throw IllegalArgumentException("Pas d'inverse pour a=$a mod $M (pgcd(a,26) doit valoir 1)")
    }

    fun encrypt(text: String, a: Int, b: Int): String {
        require(isValidKey(a, b)) { "Clé affine invalide (a=$a, b=$b)" }
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isLetter() && ch.code < 128) {
                val upper = ch.uppercaseChar()
                val x = upper.code - 'A'.code
                val y = (a * x + b).mod(M)
                sb.append(('A' + y))
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun decrypt(text: String, a: Int, b: Int): String {
        require(isValidKey(a, b)) { "Clé affine invalide (a=$a, b=$b)" }
        val aInv = modInverse(a)
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isLetter() && ch.code < 128) {
                val upper = ch.uppercaseChar()
                val y = upper.code - 'A'.code
                val x = (aInv * (y - b)).mod(M)
                sb.append(('A' + x))
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /** Construit la table de substitution complète A→Z pour une clé donnée (pour l'aperçu pédagogique). */
    fun buildSubstitutionTable(a: Int, b: Int): List<Pair<Char, Char>> {
        if (!isValidKey(a, b)) return emptyList()
        return ('A'..'Z').map { plain ->
            val x = plain.code - 'A'.code
            val y = (a * x + b).mod(M)
            plain to ('A' + y)
        }
    }
}
