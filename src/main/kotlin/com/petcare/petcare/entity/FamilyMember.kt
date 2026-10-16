package com.petcare.petcare.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "family_member",
    uniqueConstraints = [UniqueConstraint(columnNames = ["family_id", "user_id"])]
)
class FamilyMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: FamilyRole = FamilyRole.MEMBER
)
