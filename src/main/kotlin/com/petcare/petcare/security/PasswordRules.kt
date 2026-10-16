package com.petcare.petcare.security

/**
 * 새로 정하는 비밀번호 규칙.
 * 이미 저장된 비밀번호는 로그인에 그대로 쓰고, 바꿀 때만 이 규칙을 적용한다.
 */
object PasswordRules {
    fun check(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.")
        }
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            throw IllegalArgumentException("비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
        }
    }

    fun isBcrypt(stored: String): Boolean {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")
    }
}
