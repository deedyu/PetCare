package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 일정을 함께 보는 가족 그룹. 반려동물은 여기에 공유되기 전까지 개인 소유다.
 */
@Entity
@Table(name = "care_family")
class Family(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 80)
    var name: String = "",

    @Column(length = 500)
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
