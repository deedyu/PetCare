package com.petcare.petcare.dto

import java.time.LocalDate
import java.time.LocalTime

data class TaskDto(
    val id: Long?,
    val title: String,
    val done: Boolean,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val repeatType: String?,
    val notificationTime: String?,
    val priority: String?,
    val petId: Long?,
    val category: String? = null,
    val description: String? = null,
    val weekdays: String? = null,
    val assigneeId: Long? = null,
    val assigneeName: String? = null
)

