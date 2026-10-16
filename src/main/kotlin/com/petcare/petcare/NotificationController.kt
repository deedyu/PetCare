package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.NotificationService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.time.LocalDateTime

@Controller
class NotificationController(
    private val notificationService: NotificationService
) {
    @GetMapping("/notifications")
    fun list(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        val userId = user.id!!
        notificationService.refreshFor(userId, LocalDateTime.now())
        model.addAttribute("notifications", notificationService.mine(userId))
        model.addAttribute("unread", notificationService.unreadCount(userId))
        return "notifications"
    }

    @PostMapping("/notifications/{id}/open")
    fun open(@PathVariable id: Long, session: HttpSession): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        val link = try {
            notificationService.open(user.id!!, id)
        } catch (_: IllegalArgumentException) {
            return "redirect:/notifications"
        }
        return "redirect:${safeLink(link)}"
    }

    @PostMapping("/notifications/read-all")
    fun readAll(session: HttpSession): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        notificationService.markAllRead(user.id!!)
        return "redirect:/notifications"
    }

    private fun safeLink(link: String?): String {
        if (link != null && link.startsWith("/") && !link.startsWith("//")) {
            return link
        }
        return "/notifications"
    }
}
