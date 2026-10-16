package com.petcare.petcare.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "health_record")
class HealthRecord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "record_type", nullable = false)
    var recordType: String = "", // "WEIGHT", "VACCINATION", "HOSPITAL", "MEDICATION"

    @Column(name = "record_date", nullable = false)
    var recordDate: LocalDate = LocalDate.now(),

    @Column(name = "title", nullable = false)
    var title: String = "", // 제목 (예: "1차 예방접종", "정기 검진", "심장사상충약")

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null, // 상세 설명

    @Column(name = "weight")
    var weight: Double? = null, // 체중 (kg) - WEIGHT 타입일 때만 사용

    @Column(name = "vaccination_type")
    var vaccinationType: String? = null, // 예방접종 종류 - VACCINATION 타입일 때만 사용

    @Column(name = "hospital_name")
    var hospitalName: String? = null, // 병원 이름 - HOSPITAL 타입일 때만 사용

    @Column(name = "medication_name")
    var medicationName: String? = null, // 약 이름 - MEDICATION 타입일 때만 사용

    @Column(name = "medication_dosage")
    var medicationDosage: String? = null, // 복용량 - MEDICATION 타입일 때만 사용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet? = null,

    // 기존 기록은 작성자가 비어 있을 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    var author: User? = null,

    @Column(name = "created_at")
    var createdAt: java.time.LocalDateTime? = null,

    // 예방접종을 다시 맞을 날짜. 없으면 예정에 나오지 않는다.
    @Column(name = "next_due_date")
    var nextDueDate: LocalDate? = null
)



