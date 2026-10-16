package com.petcare.petcare.repository

import com.petcare.petcare.entity.Announcement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AnnouncementRepository : JpaRepository<Announcement, Long> {
    @Query("select a from Announcement a left join fetch a.author order by a.pinned desc, a.createdAt desc")
    fun findVisible(): List<Announcement>
}
