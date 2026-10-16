package com.petcare.petcare.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 로그인 실패를 메모리에만 센다.
 * 계정을 지우거나 비밀번호를 바꾸지 않고, 서버를 재시작하면 횟수도 사라진다.
 */
@Component
class LoginAttemptTracker(
    @param:Value("\${petcare.login.max-attempts:5}") private val maxAttempts: Int,
    @param:Value("\${petcare.login.window-seconds:900}") private val windowSeconds: Long
) {
    private val attempts = ConcurrentHashMap<String, State>()

    fun key(username: String?, address: String?): String {
        return "${username.orEmpty().trim().lowercase()}|${address.orEmpty()}"
    }

    fun isBlocked(key: String): Boolean {
        val state = attempts[key] ?: return false
        val now = System.currentTimeMillis()
        synchronized(state) {
            return now < state.lockedUntil
        }
    }

    fun recordFailure(key: String) {
        val state = attempts.computeIfAbsent(key) { State() }
        val now = System.currentTimeMillis()
        val windowMillis = windowSeconds * 1000
        synchronized(state) {
            while (state.failures.isNotEmpty() && now - state.failures.first() > windowMillis) {
                state.failures.removeFirst()
            }
            state.failures.addLast(now)
            if (state.failures.size >= maxAttempts) {
                state.lockedUntil = now + windowMillis
                state.failures.clear()
            }
        }
    }

    fun recordSuccess(key: String) {
        attempts.remove(key)
    }

    private class State {
        val failures = ArrayDeque<Long>()
        var lockedUntil: Long = 0
    }
}
