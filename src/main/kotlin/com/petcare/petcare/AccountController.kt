package com.petcare.petcare

import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.security.RecoveryAttemptTracker
import com.petcare.petcare.security.UsernameRules
import com.petcare.petcare.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AccountController(
    private val userService: UserService,
    private val recoveryAttempts: RecoveryAttemptTracker
) {
    @GetMapping("/register")
    fun registerPage(session: HttpSession): String {
        return redirectIfLoggedIn(session) ?: "register"
    }

    @GetMapping("/register/username-check")
    @ResponseBody
    fun usernameCheck(@RequestParam username: String): Map<String, Any> {
        val message = userService.usernameAvailability(username)
        return if (message == null) {
            mapOf("available" to true, "message" to "사용 가능한 아이디입니다.")
        } else {
            mapOf("available" to false, "message" to message)
        }
    }

    @PostMapping("/register")
    fun register(
        @RequestParam username: String,
        @RequestParam password: String,
        @RequestParam passwordConfirm: String,
        @RequestParam name: String,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val trimmedUsername = username.trim()
        val trimmedName = name.trim()
        var usernameError: String? = null
        var passwordError: String? = null
        var confirmError: String? = null
        var nameError: String? = null
        try {
            UsernameRules.check(trimmedUsername)
        } catch (e: IllegalArgumentException) {
            usernameError = e.message
        }
        if (usernameError == null && userService.findByUsername(trimmedUsername) != null) {
            usernameError = "이미 사용 중인 아이디입니다."
        }
        try {
            PasswordRules.check(password)
        } catch (e: IllegalArgumentException) {
            passwordError = e.message
        }
        if (password != passwordConfirm) {
            confirmError = "비밀번호 확인이 일치하지 않습니다."
        }
        if (trimmedName.isEmpty() || trimmedName.length > 30) {
            nameError = "이름은 1~30자로 입력해 주세요."
        }
        if (usernameError != null || passwordError != null || confirmError != null || nameError != null) {
            fillRegister(model, trimmedUsername, trimmedName, usernameError, passwordError, confirmError, nameError)
            return "register"
        }
        return try {
            val code = userService.registerMember(trimmedUsername, password, trimmedName)
            redirectAttributes.addFlashAttribute("recoveryCode", code)
            "redirect:/register/complete"
        } catch (e: IllegalArgumentException) {
            fillRegister(model, trimmedUsername, trimmedName, e.message, null, null, null)
            "register"
        }
    }

    @GetMapping("/register/complete")
    fun registerComplete(): String = "recovery-code"

    @GetMapping("/forgot")
    fun forgot(session: HttpSession, model: Model): String {
        if (validProof(session)) {
            model.addAttribute("verified", true)
            model.addAttribute("username", session.getAttribute("recoveryUsername"))
        }
        return "forgot"
    }

    @PostMapping("/forgot/verify")
    fun verifyRecovery(
        @RequestParam username: String,
        @RequestParam recoveryCode: String,
        request: HttpServletRequest,
        session: HttpSession,
        model: Model
    ): String {
        val key = recoveryAttempts.key(username, request.remoteAddr)
        if (recoveryAttempts.isBlocked(key)) {
            model.addAttribute("error", "시도가 많습니다. 잠시 후 다시 시도해 주세요.")
            model.addAttribute("username", username.trim())
            return "forgot"
        }
        val userId = userService.recoveryUserId(username, recoveryCode)
        if (userId == null) {
            recoveryAttempts.recordFailure(key)
            model.addAttribute("error", "아이디 또는 복구 코드를 확인할 수 없습니다.")
            model.addAttribute("username", username.trim())
            return "forgot"
        }
        recoveryAttempts.recordSuccess(key)
        request.changeSessionId()
        val next = request.getSession(true)
        next.setAttribute(RECOVERY_USER, userId)
        next.setAttribute("recoveryUsername", username.trim())
        next.setAttribute(RECOVERY_UNTIL, System.currentTimeMillis() + PROOF_MILLIS)
        return "redirect:/forgot"
    }

    @PostMapping("/forgot/reset")
    fun resetPassword(
        @RequestParam password: String,
        @RequestParam passwordConfirm: String,
        session: HttpSession,
        model: Model
    ): String {
        val userId = proofUserId(session)
        if (userId == null) {
            return "redirect:/forgot"
        }
        return try {
            val code = userService.resetPassword(userId, password, passwordConfirm)
            session.invalidate()
            model.addAttribute("recoveryCode", code)
            model.addAttribute("headline", "비밀번호를 변경했습니다")
            model.addAttribute("lead", "이전 복구 코드는 더 이상 쓸 수 없습니다. 새 코드를 안전한 곳에 보관하세요. 이 화면을 벗어나면 다시 볼 수 없습니다.")
            "recovery-code"
        } catch (e: IllegalArgumentException) {
            model.addAttribute("verified", true)
            model.addAttribute("username", session.getAttribute("recoveryUsername"))
            model.addAttribute("formError", e.message)
            "forgot"
        }
    }

    private fun fillRegister(
        model: Model,
        username: String,
        name: String,
        usernameError: String?,
        passwordError: String?,
        confirmError: String?,
        nameError: String?
    ) {
        model.addAttribute("username", username)
        model.addAttribute("name", name)
        model.addAttribute("usernameError", usernameError)
        model.addAttribute("passwordError", passwordError)
        model.addAttribute("confirmError", confirmError)
        model.addAttribute("nameError", nameError)
    }

    private fun validProof(session: HttpSession): Boolean = proofUserId(session) != null

    private fun proofUserId(session: HttpSession): Long? {
        val until = session.getAttribute(RECOVERY_UNTIL) as? Long ?: return null
        if (System.currentTimeMillis() > until) {
            session.removeAttribute(RECOVERY_USER)
            session.removeAttribute(RECOVERY_UNTIL)
            return null
        }
        return when (val id = session.getAttribute(RECOVERY_USER)) {
            is Long -> id
            is Number -> id.toLong()
            else -> null
        }
    }

    private fun redirectIfLoggedIn(session: HttpSession): String? {
        val user = session.getAttribute("user") as? User ?: return null
        return if (user.effectiveRole() == Role.ADMIN) "redirect:/admin" else "redirect:/"
    }

    companion object {
        private const val RECOVERY_USER = "recoveryUserId"
        private const val RECOVERY_UNTIL = "recoveryUntil"
        private const val PROOF_MILLIS = 10 * 60 * 1000L
    }
}
