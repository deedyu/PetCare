package com.petcare.petcare.security

/**
 * 공개 회원가입 아이디 규칙.
 * 이미 만들어진 계정은 이 형식과 달라도 로그인을 유지한다.
 */
object UsernameRules {
    private val pattern = Regex("^[a-z0-9]{4,20}$")

    fun check(username: String) {
        val value = username.trim()
        if (!pattern.matches(value)) {
            throw IllegalArgumentException("아이디는 영문 소문자와 숫자 4~20자여야 합니다.")
        }
        if (value.equals("admin", ignoreCase = true)) {
            throw IllegalArgumentException("사용할 수 없는 아이디입니다.")
        }
    }
}
