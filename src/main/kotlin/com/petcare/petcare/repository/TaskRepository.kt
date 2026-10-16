package com.petcare.petcare.repository

import com.petcare.petcare.entity.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TaskRepository : JpaRepository<Task, Long> {
    fun findByPetIdOrderByIdDesc(petId: Long): List<Task>

    @Query("SELECT t FROM Task t JOIN FETCH t.pet ORDER BY t.id DESC")
    fun findAllWithPet(): List<Task>

    @Query("SELECT t FROM Task t JOIN FETCH t.pet p WHERE p.user.id = :userId ORDER BY t.id DESC")
    fun findByUserIdOrderByIdDesc(userId: Long): List<Task>

    @Query(
        """
        select distinct t from Task t
        join fetch t.pet p
        left join fetch t.assignee
        where p.user.id = :userId
        or p.family.id in (select m.family.id from FamilyMember m where m.user.id = :userId)
        order by t.id desc
        """
    )
    fun findVisibleToUser(userId: Long): List<Task>

    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.assignee.id = :userId")
    fun clearAssignee(userId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.pet.family.id = :familyId and t.assignee.id = :userId")
    fun clearAssigneeInFamily(familyId: Long, userId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.pet.family.id = :familyId")
    fun clearAssigneesOfFamily(familyId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.pet.id = :petId")
    fun clearAssigneesOfPet(petId: Long)

    @Query(
        """
        select t from Task t
        join fetch t.pet p
        left join fetch p.user
        left join fetch t.assignee
        where t.notificationTime is not null
        """
    )
    fun findReminders(): List<Task>
}
