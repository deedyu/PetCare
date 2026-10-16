package com.petcare.petcare.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class LoginFailureHandler(
    private val loginAttemptTracker: LoginAttemptTracker
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        loginAttemptTracker.recordFailure(
            loginAttemptTracker.key(request.getParameter("username"), request.remoteAddr)
        )
        val suspended = exception is DisabledException
        if (request.getParameter("from") == "admin") {
            response.sendRedirect(if (suspended) "/admin/login?suspended" else "/admin/login?error")
        } else {
            response.sendRedirect(if (suspended) "/login?suspended" else "/login?error")
        }
    }
}
