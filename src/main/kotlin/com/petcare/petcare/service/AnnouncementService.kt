package com.petcare.petcare.service

import com.petcare.petcare.entity.Announcement
import com.petcare.petcare.repository.AnnouncementRepository
import com.petcare.petcare.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val userRepository: UserRepository
) {
    fun list(): List<Announcement> = announcementRepository.findVisible()

    @Transactional
    fun create(adminId: Long, title: String, body: String, pinned: Boolean): Announcement {
        val notice = Announcement(
            title = title.trim().take(120),
            body = body.trim().take(2000),
            pinned = pinned,
            author = userRepository.findById(adminId).orElse(null)
        )
        return announcementRepository.save(notice)
    }

    @Transactional
    fun setPinned(id: Long, pinned: Boolean) {
        val notice = announcementRepository.findById(id)
            .orElseThrow { IllegalArgumentException("공지를 찾을 수 없습니다.") }
        notice.pinned = pinned
        announcementRepository.save(notice)
    }

    @Transactional
    fun delete(id: Long) {
        if (announcementRepository.existsById(id)) {
            announcementRepository.deleteById(id)
        }
    }
}
