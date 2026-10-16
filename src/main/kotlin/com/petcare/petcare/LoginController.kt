package com.petcare.petcare

import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class LoginController(
    private val userService: UserService
) {
    @GetMapping("/login")
    fun loginPage(session: HttpSession): String {
        return redirectIfLoggedIn(session) ?: "login"
    }

    @GetMapping("/register")
    fun registerPage(session: HttpSession): String {
        return redirectIfLoggedIn(session) ?: "register"
    }

    @PostMapping("/register")
    fun register(
        @RequestParam username: String,
        @RequestParam password: String,
        @RequestParam name: String,
        @RequestParam(required = false) email: String?,
        model: Model
    ): String {
        if (username == "admin") {
            model.addAttribute("error", "사용할 수 없는 아이디입니다.")
            return "register"
        }
        try {
            PasswordRules.check(password)
            userService.createUser(username, password, name, email, Role.USER)
        } catch (e: IllegalArgumentException) {
            model.addAttribute("error", e.message)
            return "register"
        }
        model.addAttribute("success", "회원가입이 완료되었습니다. 로그인해주세요.")
        return "login"
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
