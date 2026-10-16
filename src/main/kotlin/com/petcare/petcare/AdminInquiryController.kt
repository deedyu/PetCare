package com.petcare.petcare

import com.petcare.petcare.entity.InquiryStatus
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.InquiryService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AdminInquiryController(
    private val inquiryService: InquiryService
) {
    @GetMapping("/admin/inquiries")
    fun list(model: Model, session: HttpSession): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        model.addAttribute("inquiries", inquiryService.all())
        return "admin-inquiries"
    }

    @GetMapping("/admin/inquiries/{id}")
    fun detail(@PathVariable id: Long, model: Model, session: HttpSession): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        return try {
            model.addAttribute("inquiry", inquiryService.requireAny(id))
            model.addAttribute("replies", inquiryService.replies(id))
            "admin-inquiry"
        } catch (_: IllegalArgumentException) {
            "redirect:/admin/inquiries"
        }
    }

    @PostMapping("/admin/inquiries/{id}/status")
    fun status(
        @PathVariable id: Long,
        @RequestParam status: String,
        session: HttpSession
    ): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        val parsed = runCatching { InquiryStatus.valueOf(status) }.getOrNull()
            ?: return "redirect:/admin/inquiries/$id"
        inquiryService.changeStatus(id, parsed)
        return "redirect:/admin/inquiries/$id"
    }

    @PostMapping("/admin/inquiries/{id}/reply")
    fun reply(
        @PathVariable id: Long,
        @RequestParam body: String,
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User ?: return "redirect:/admin/login"
        if (body.isBlank()) {
            return "redirect:/admin/inquiries/$id?error=empty"
        }
        inquiryService.reply(admin.id!!, id, body)
        return "redirect:/admin/inquiries/$id"
    }
}
