package com.petcare.petcare.config

import com.petcare.petcare.entity.TaskOccurrence
import com.petcare.petcare.repository.TaskOccurrenceRepository
import com.petcare.petcare.repository.TaskRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 예전에 일정 전체에 붙어 있던 완료 표시를, 그 날짜의 수행 기록으로 옮긴다.
 * 반복 일정은 시작일만 완료로 남기고, 다음 날짜는 완료되지 않은 상태로 둔다.
 */
@Component
class TaskOccurrenceBackfill(
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {
        taskRepository.findAll().forEach { task ->
            val taskId = task.id ?: return@forEach
            val date = task.dueDate ?: return@forEach
            if (!task.done) return@forEach
            if (occurrenceRepository.findByTaskIdAndOccurrenceDate(taskId, date) == null) {
                occurrenceRepository.save(
                    TaskOccurrence(
                        task = task,
                        occurrenceDate = date,
                        done = true
                    )
                )
            }
            if (task.repeatType != null) {
                task.done = false
                taskRepository.save(task)
            }
        }
    }
}
