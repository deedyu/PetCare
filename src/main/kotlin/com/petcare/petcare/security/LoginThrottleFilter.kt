package com.petcare.petcare.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 비밀번호를 확인하기 전에 잠긴 로그인을 돌려보낸다.
 */
class LoginThrottleFilter(
    private val tracker: LoginAttemptTracker
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.servletPath.ifEmpty { request.requestURI }
        if (request.method == "POST" && path == "/login") {
            val key = tracker.key(request.getParameter("username"), request.remoteAddr)
            if (tracker.isBlocked(key)) {
                val target = if (request.getParameter("from") == "admin") "/admin/login?locked" else "/login?locked"
                response.sendRedirect(target)
                return
            }
        }
        filterChain.doFilter(request, response)
    }
}
