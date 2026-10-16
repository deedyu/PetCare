package com.petcare.petcare.service

import com.petcare.petcare.entity.Inquiry
import com.petcare.petcare.entity.InquiryReply
import com.petcare.petcare.entity.InquiryStatus
import com.petcare.petcare.entity.NotificationType
import com.petcare.petcare.repository.InquiryReplyRepository
import com.petcare.petcare.repository.InquiryRepository
import com.petcare.petcare.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val replyRepository: InquiryReplyRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    fun mine(userId: Long): List<Inquiry> = inquiryRepository.findByAuthorId(userId)

    fun all(): List<Inquiry> = inquiryRepository.findAllWithAuthor()

    fun countWaiting(): Long = inquiryRepository.countByStatusNot(InquiryStatus.ANSWERED)

    fun replies(inquiryId: Long): List<InquiryReply> = replyRepository.findByInquiryId(inquiryId)

    fun requireOwn(userId: Long, inquiryId: Long): Inquiry {
        val inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow { IllegalArgumentException("문의를 찾을 수 없습니다.") }
        if (inquiry.author?.id != userId) {
            throw IllegalArgumentException("문의를 찾을 수 없습니다.")
        }
        return inquiry
    }

    fun requireAny(inquiryId: Long): Inquiry {
        return inquiryRepository.findById(inquiryId)
            .orElseThrow { IllegalArgumentException("문의를 찾을 수 없습니다.") }
    }

    @Transactional
    fun create(userId: Long, title: String, body: String): Inquiry {
        val inquiry = Inquiry(
            title = title.trim().take(120),
            body = body.trim().take(2000),
            author = userRepository.findById(userId).orElse(null)
        )
        return inquiryRepository.save(inquiry)
    }

    @Transactional
    fun changeStatus(inquiryId: Long, status: InquiryStatus) {
        val inquiry = requireAny(inquiryId)
        inquiry.status = status
        inquiryRepository.save(inquiry)
    }

    @Transactional
    fun reply(adminId: Long, inquiryId: Long, body: String): InquiryReply {
        val inquiry = requireAny(inquiryId)
        val saved = replyRepository.save(
            InquiryReply(
                inquiry = inquiry,
                body = body.trim().take(2000),
                author = userRepository.findById(adminId).orElse(null)
            )
        )
        inquiry.status = InquiryStatus.ANSWERED
        inquiryRepository.save(inquiry)
        val authorId = inquiry.author?.id
        val replyId = saved.id
        if (authorId != null && replyId != null && authorId != adminId) {
            notificationService.createIfAbsent(
                userId = authorId,
                type = NotificationType.INQUIRY_REPLY,
                title = "문의에 답변이 달렸습니다",
                body = inquiry.title,
                link = "/inquiries/${inquiry.id}",
                dedupeKey = "inquiry-reply:$replyId"
            )
        }
        return saved
    }

    /**
     * 탈퇴하는 회원의 문의와 그 답변을 지운다.
     * 답변을 먼저 지우지 않으면 문의 행이 남는다.
     */
    @Transactional
    fun releaseUser(userId: Long) {
        val inquiries = inquiryRepository.findByAuthorId(userId)
        for (inquiry in inquiries) {
            val inquiryId = inquiry.id ?: continue
            replyRepository.findByInquiryId(inquiryId).forEach { replyRepository.delete(it) }
            inquiryRepository.delete(inquiry)
        }
    }
}
