package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "app_notification",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "dedupe_key"])]
)
class AppNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: NotificationType = NotificationType.TASK_REMINDER,

    @Column(nullable = false, length = 120)
    var title: String = "",

    @Column(nullable = false, length = 200)
    var body: String = "",

    @Column(length = 200)
    var link: String? = null,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    // 같은 사용자에게 같은 키는 한 번만 쌓인다.
    @Column(name = "dedupe_key", nullable = false, length = 80)
    var dedupeKey: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
