package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "task")
class Task(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var done: Boolean = false,

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    @Column(name = "due_time")
    var dueTime: LocalTime? = null,

    @Column(name = "repeat_type")
    var repeatType: String? = null, // "DAILY", "WEEKLY", "MONTHLY", null

    @Column(name = "notification_time")
    var notificationTime: String? = null, // "ON_TIME", "10_MIN_BEFORE", "1_HOUR_BEFORE", null

    @Column(name = "priority")
    var priority: String? = null, // "HIGH", "MEDIUM", "LOW", null

    // 식사, 산책, 건강 등. 기존 일정은 비어 있을 수 있다.
    @Column(length = 20)
    var category: String? = null,

    @Column(length = 2000)
    var description: String? = null,

    // WEEKDAYS 반복일 때 MON,WED 처럼 저장한다.
    @Column(length = 40)
    var weekdays: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet? = null,

    // 가족 공유 일정에서만 지정한다. 기존 일정은 비어 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    var assignee: User? = null
) {
    // 화면에서만 쓰는 값. DB에 저장하지 않는다.
    @Transient
    var doneOnView: Boolean = false

    @Transient
    var viewDate: LocalDate? = null
}
