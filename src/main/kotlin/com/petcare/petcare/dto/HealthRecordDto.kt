package com.petcare.petcare.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class HealthRecordDto(
    val id: Long?,
    val recordType: String,
    val recordDate: LocalDate,
    val title: String,
    val description: String?,
    val weight: Double?,
    val vaccinationType: String?,
    val hospitalName: String?,
    val medicationName: String?,
    val medicationDosage: String?,
    val petId: Long?,
    val authorName: String? = null,
    val createdAt: LocalDateTime? = null,
    val nextDueDate: LocalDate? = null
)

