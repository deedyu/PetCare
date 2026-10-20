package com.petcare.petcare.security

import java.security.SecureRandom

/**
 * 비밀번호 재설정에 쓰는 일회성 코드.
 * 화면에는 한 번만 보여주고, 서버에는 해시만 남긴다.
 */
object RecoveryCodes {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(): String {
        val raw = buildString(16) {
            repeat(16) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }
        return raw.chunked(4).joinToString("-")
    }

    fun normalize(input: String): String {
        return input.uppercase().filter { it.isLetterOrDigit() }
    }
}
