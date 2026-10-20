package com.petcare.petcare.security

import com.petcare.petcare.entity.RememberMeToken
import com.petcare.petcare.repository.RememberMeTokenRepository
import org.springframework.security.web.authentication.rememberme.InvalidCookieException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64

/**
 * 로그인 유지 토큰은 기기(series)마다 따로 둔다.
 * 비밀값은 SHA-256 해시만 저장하고, 자동 로그인에 성공하면 새 값으로 바꾼다.
 */
@Service
class RememberMeTokenService(
    private val repository: RememberMeTokenRepository,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager
) {
    private val random = SecureRandom()
    private val separateTx = TransactionTemplate(transactionManager).apply {
        propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun issue(username: String, validity: Duration): IssuedToken {
        val series = randomText()
        val secret = randomText()
        val now = clock.instant()
        repository.save(
            RememberMeToken(
                series = series,
                username = username,
                tokenHash = hash(secret),
                expiresAt = now.plus(validity),
                lastUsedAt = now
            )
        )
        return IssuedToken(series, secret, username)
    }

    /**
     * 제시된 비밀이 맞으면 새 비밀을 돌려준다. 예전 비밀을 다시 쓰면 그 계정의 토큰을 모두 지운다.
     */
    fun rotate(series: String, presentedSecret: String): IssuedToken {
        val row = repository.findById(series).orElse(null)
            ?: throw InvalidCookieException("로그인 유지 정보가 없습니다.")
        if (!row.expiresAt.isAfter(clock.instant())) {
            revokeSeries(series)
            throw InvalidCookieException("로그인 유지 기간이 끝났습니다.")
        }
        if (!constantTimeEquals(hash(presentedSecret), row.tokenHash)) {
            revokeUser(row.username)
            throw InvalidCookieException("로그인 유지 토큰이 재사용되었습니다.")
        }
        val secret = randomText()
        row.tokenHash = hash(secret)
        row.lastUsedAt = clock.instant()
        repository.save(row)
        return IssuedToken(series, secret, row.username)
    }

    fun revokeSeries(series: String) {
        separateTx.executeWithoutResult {
            if (repository.existsById(series)) {
                repository.deleteById(series)
            }
        }
    }

    fun revokeUser(username: String) {
        separateTx.executeWithoutResult {
            repository.deleteByUsername(username)
        }
    }

    fun hash(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))
    }

    private fun randomText(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    data class IssuedToken(val series: String, val secret: String, val username: String)
}
