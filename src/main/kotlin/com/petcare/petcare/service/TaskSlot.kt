package com.petcare.petcare.service

import com.petcare.petcare.entity.Pet
import com.petcare.petcare.entity.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 특정 날짜에 펼쳐진 일정. 홈과 캘린더가 이 단위로 완료 여부를 본다.
 */
class TaskSlot(
    val id: Long,
    val title: String,
    val done: Boolean,
    val pet: Pet?,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val priority: String?,
    val category: String?,
    val occurrenceDate: LocalDate,
    val memo: String?,
    val completedByName: String?,
    val completedAt: LocalDateTime?,
    val repeatType: String?,
    val assigneeName: String? = null
) {
    companion object {
        fun of(task: Task, date: LocalDate, done: Boolean, memo: String?, completedByName: String?, completedAt: LocalDateTime?) =
            TaskSlot(
                id = task.id!!,
                title = task.title,
                done = done,
                pet = task.pet,
                dueDate = task.dueDate,
                dueTime = task.dueTime,
                priority = task.priority,
                category = task.category,
                occurrenceDate = date,
                memo = memo,
                completedByName = completedByName,
                completedAt = completedAt,
                repeatType = task.repeatType,
                assigneeName = task.assignee?.name
            )
    }
}
