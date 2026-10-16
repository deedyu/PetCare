package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.AnnouncementService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AdminNoticeController(
    private val announcementService: AnnouncementService
) {
    @GetMapping("/admin/notices")
    fun notices(model: Model, session: HttpSession): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        model.addAttribute("notices", announcementService.list())
        return "admin-notices"
    }

    @PostMapping("/admin/notices")
    fun create(
        @RequestParam title: String,
        @RequestParam body: String,
        @RequestParam(required = false) pinned: String?,
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User ?: return "redirect:/admin/login"
        if (title.isBlank() || body.isBlank()) {
            return "redirect:/admin/notices?error=empty"
        }
        announcementService.create(admin.id!!, title, body, pinned == "on")
        return "redirect:/admin/notices"
    }

    @PostMapping("/admin/notices/{id}/pin")
    fun pin(
        @PathVariable id: Long,
        @RequestParam pinned: Boolean,
        session: HttpSession
    ): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        announcementService.setPinned(id, pinned)
        return "redirect:/admin/notices"
    }

    @PostMapping("/admin/notices/{id}/delete")
    fun delete(@PathVariable id: Long, session: HttpSession): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        announcementService.delete(id)
        return "redirect:/admin/notices"
    }
}
