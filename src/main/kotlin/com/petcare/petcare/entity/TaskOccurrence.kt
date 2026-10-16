package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 반복 규칙과 분리된 날짜별 수행 기록.
 * 같은 일정의 같은 날짜는 한 행만 가진다.
 */
@Entity
@Table(
    name = "task_occurrence",
    uniqueConstraints = [UniqueConstraint(columnNames = ["task_id", "occurrence_date"])]
)
class TaskOccurrence(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    var task: Task? = null,

    @Column(name = "occurrence_date", nullable = false)
    var occurrenceDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var done: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    var completedBy: User? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(length = 2000)
    var memo: String? = null
)
