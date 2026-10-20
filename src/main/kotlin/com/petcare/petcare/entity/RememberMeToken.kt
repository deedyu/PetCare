package com.petcare.petcare.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * 로그인 유지 토큰. series는 기기마다 하나이고, 비밀값은 해시만 저장한다.
 */
@Entity
@Table(
    name = "remember_me_token",
    indexes = [Index(name = "idx_remember_username", columnList = "username")]
)
class RememberMeToken(
    @Id
    @Column(length = 64)
    var series: String = "",

    @Column(nullable = false, length = 64)
    var username: String = "",

    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.EPOCH,

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant = Instant.EPOCH
)
