package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 가족 화면의 활동 기록. 개인 일정은 여기에 남기지 않는다.
 */
@Entity
@Table(name = "activity_log")
class ActivityLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    var actor: User? = null,

    @Column(nullable = false, length = 40)
    var action: String = "",

    @Column(nullable = false, length = 200)
    var summary: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
