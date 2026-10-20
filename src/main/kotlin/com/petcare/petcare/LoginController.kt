package com.petcare.petcare

import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class LoginController {
    @GetMapping("/login")
    fun loginPage(session: HttpSession): String {
        return redirectIfLoggedIn(session) ?: "login"
    }

    @RequestMapping("/access-denied")
    fun accessDenied(): String = "access-denied"

    @GetMapping("/admin/login")
    fun adminLoginPage(session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user?.effectiveRole() == Role.ADMIN) {
            return "redirect:/admin"
        }
        return "admin-login"
    }

    private fun redirectIfLoggedIn(session: HttpSession): String? {
        val user = session.getAttribute("user") as? User ?: return null
        return if (user.effectiveRole() == Role.ADMIN) "redirect:/admin" else "redirect:/"
    }
}
