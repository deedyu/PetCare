package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 소유자가 발급한 참여 코드. 한 코드는 한 명만 사용하고, 화면에 다시 보여줄 수 있게 평문으로 둔다.
 */
@Entity
@Table(name = "family_invitation")
class FamilyInvitation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family? = null,

    @Column(nullable = false, unique = true, length = 12)
    var code: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InviteStatus = InviteStatus.OPEN,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    var acceptedBy: User? = null
)
