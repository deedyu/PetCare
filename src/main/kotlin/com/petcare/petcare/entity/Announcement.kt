package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "announcement")
class Announcement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 120)
    var title: String = "",

    @Column(nullable = false, length = 2000)
    var body: String = "",

    @Column(nullable = false)
    var pinned: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    var author: User? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
