package com.petcare.petcare.security

import com.petcare.petcare.entity.Role
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Component

/**
 * 로그인에 성공하면 역할로 첫 화면을 고른다.
 * 세션을 쓰는 이유: 화면과 API가 이미 같은 서버 세션을 보고 있고,
 * 토큰을 브라우저에 따로 저장할 필요가 없다.
 */
@Component
class LoginSuccessHandler(
    private val loginAttemptTracker: LoginAttemptTracker
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        loginAttemptTracker.recordSuccess(
            loginAttemptTracker.key(request.getParameter("username"), request.remoteAddr)
        )
        val details = authentication.principal as? PetCareUserDetails
        val fromAdminDoor = request.getParameter("from") == "admin"
        if (details == null) {
            response.sendRedirect("/login?error")
            return
        }
        if (fromAdminDoor && details.role != Role.ADMIN) {
            SecurityContextLogoutHandler().logout(request, response, authentication)
            response.sendRedirect("/admin/login?denied")
            return
        }
        if (details.role == Role.ADMIN) {
            response.sendRedirect("/admin")
        } else {
            response.sendRedirect("/")
        }
    }
}
