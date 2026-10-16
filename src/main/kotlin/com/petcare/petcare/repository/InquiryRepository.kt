package com.petcare.petcare.repository

import com.petcare.petcare.entity.Inquiry
import com.petcare.petcare.entity.InquiryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : JpaRepository<Inquiry, Long> {
    @Query("select i from Inquiry i left join fetch i.author where i.author.id = :userId order by i.createdAt desc")
    fun findByAuthorId(userId: Long): List<Inquiry>

    @Query("select i from Inquiry i left join fetch i.author order by i.createdAt desc")
    fun findAllWithAuthor(): List<Inquiry>

    fun countByStatusNot(status: InquiryStatus): Long
}
