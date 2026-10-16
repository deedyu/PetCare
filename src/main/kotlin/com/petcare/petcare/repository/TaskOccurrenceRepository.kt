package com.petcare.petcare.repository

import com.petcare.petcare.entity.TaskOccurrence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface TaskOccurrenceRepository : JpaRepository<TaskOccurrence, Long> {
    fun findByTaskIdAndOccurrenceDate(taskId: Long, occurrenceDate: LocalDate): TaskOccurrence?

    fun findByTaskIdInAndOccurrenceDate(taskIds: Collection<Long>, occurrenceDate: LocalDate): List<TaskOccurrence>

    fun findByTaskIdInAndOccurrenceDateBetween(
        taskIds: Collection<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<TaskOccurrence>

    fun deleteByTaskId(taskId: Long)

    @Query(
        """
        select count(o) from TaskOccurrence o
        where o.done = true and (
            o.task.pet.user.id = :userId
            or o.task.pet.family.id in (
                select m.family.id from FamilyMember m where m.user.id = :userId
            )
        )
        """
    )
    fun countDoneByUser(userId: Long): Long

    @Modifying(flushAutomatically = true)
    @Query("update TaskOccurrence o set o.completedBy = null where o.completedBy.id = :userId")
    fun clearCompletedBy(userId: Long)
}
