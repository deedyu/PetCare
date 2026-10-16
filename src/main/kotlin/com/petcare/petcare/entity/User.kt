package com.petcare.petcare.entity

import jakarta.persistence.*

@Entity
@Table(name = "user")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 동시에 고치면 나중 저장을 거절한다. 기존 행은 0으로 시작한다.
    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(nullable = false, unique = true)
    var username: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Column(nullable = false)
    var name: String = "",

    // 로그인 아이디와 별개. 기존 회원은 비어 있을 수 있다.
    @Column(unique = true)
    var email: String? = null,

    // null이면 아직 역할이 채워지지 않은 기존 행이다. 기동 시 USER로 맞춘다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var role: Role? = Role.USER,

    // null이면 기존 회원이다. 정지 표시가 생기기 전에는 정상으로 본다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: AccountStatus? = null
) {
    fun effectiveRole(): Role = role ?: Role.USER

    fun effectiveStatus(): AccountStatus = status ?: AccountStatus.ACTIVE
}

