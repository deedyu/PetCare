package com.petcare.petcare.repository

import com.petcare.petcare.entity.InquiryReply
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface InquiryReplyRepository : JpaRepository<InquiryReply, Long> {
    @Query("select r from InquiryReply r left join fetch r.author where r.inquiry.id = :inquiryId order by r.createdAt asc")
    fun findByInquiryId(inquiryId: Long): List<InquiryReply>
}
