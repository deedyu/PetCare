package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.NotificationService
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * 일반 화면 사이드바에서 안 읽은 알림 수를 보여 준다.
 */
@ControllerAdvice
class MemberViewAdvice(
    private val notificationService: NotificationService,
    @Value("\${petcare.session.idle-seconds:1800}") private val idleSeconds: Long
) {
    @ModelAttribute("idleSeconds")
    fun idleSeconds(): Long = idleSeconds

    @ModelAttribute("navUnread")
    fun navUnread(session: HttpSession): Long {
        val user = session.getAttribute("user") as? User ?: return 0L
        val userId = user.id ?: return 0L
        return try {
            notificationService.unreadCount(userId)
        } catch (_: Exception) {
            0L
        }
    }
}
