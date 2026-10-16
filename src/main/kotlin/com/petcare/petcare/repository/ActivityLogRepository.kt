package com.petcare.petcare.repository

import com.petcare.petcare.entity.ActivityLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ActivityLogRepository : JpaRepository<ActivityLog, Long> {
    @Query(
        """
        select a from ActivityLog a
        left join fetch a.actor
        where a.family.id = :familyId
        order by a.id desc
        """
    )
    fun findByFamilyIdOrderByIdDesc(familyId: Long): List<ActivityLog>

    fun deleteByFamilyId(familyId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update ActivityLog a set a.actor = null where a.actor.id = :userId")
    fun clearActor(userId: Long)
}
