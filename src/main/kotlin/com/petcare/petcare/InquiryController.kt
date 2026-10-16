package com.petcare.petcare

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
class InquiryController(
    private val inquiryService: InquiryService
) {
    @GetMapping("/inquiries")
    fun list(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        model.addAttribute("inquiries", inquiryService.mine(user.id!!))
        return "inquiries"
    }

    @GetMapping("/inquiries/new")
    fun form(session: HttpSession): String {
        if (session.getAttribute("user") as? User == null) {
            return "redirect:/login"
        }
        return "inquiry-form"
    }

    @PostMapping("/inquiries")
    fun create(
        @RequestParam title: String,
        @RequestParam body: String,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        if (title.isBlank() || body.isBlank()) {
            return "redirect:/inquiries/new?error=empty"
        }
        val inquiry = inquiryService.create(user.id!!, title, body)
        return "redirect:/inquiries/${inquiry.id}"
    }

    @GetMapping("/inquiries/{id}")
    fun detail(@PathVariable id: Long, model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        return try {
            model.addAttribute("inquiry", inquiryService.requireOwn(user.id!!, id))
            model.addAttribute("replies", inquiryService.replies(id))
            "inquiry-detail"
        } catch (_: IllegalArgumentException) {
            "redirect:/inquiries"
        }
    }
}
