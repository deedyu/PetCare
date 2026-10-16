package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "inquiry_reply")
class InquiryReply(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    var inquiry: Inquiry? = null,

    @Column(nullable = false, length = 2000)
    var body: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    var author: User? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
