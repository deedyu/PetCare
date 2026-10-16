package com.petcare.petcare.repository

import com.petcare.petcare.entity.AppNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface AppNotificationRepository : JpaRepository<AppNotification, Long> {
    fun existsByUser_IdAndDedupeKey(userId: Long, dedupeKey: String): Boolean

    @Query(
        """
        select n from AppNotification n
        where n.user.id = :userId
        order by n.createdAt desc
        """
    )
    fun findByUserId(userId: Long): List<AppNotification>

    fun countByUser_IdAndReadAtIsNull(userId: Long): Long

    @Modifying(flushAutomatically = true)
    @Query("update AppNotification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
    fun markAllRead(userId: Long, readAt: LocalDateTime)
}
