package com.petcare.petcare.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 열린 화면에서 실제 사용이 끊기면 세션과 이 브라우저의 로그인 유지 토큰을 함께 폐기한다.
 * 브라우저를 닫아 세션 쿠키가 사라진 경우에는 호출되지 않으므로, 유지 토큰은 남는다.
 */
class IdleSessionFilter(
    private val idle: Duration,
    private val clock: Clock,
    private val rememberMeServices: HashedRememberMeServices
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val anonymous = authentication == null || authentication is AnonymousAuthenticationToken || !authentication.isAuthenticated
        if (anonymous) {
            filterChain.doFilter(request, response)
            return
        }

        val path = request.servletPath.ifEmpty { request.requestURI }.substringBefore('?')
        if (request.method.equals("POST", ignoreCase = true) && path == "/session/idle") {
            expire(request, response)
            return
        }

        val session = request.getSession(false) ?: request.session
        val last = session.getAttribute(LAST_ACTIVITY) as? Instant
        val now = clock.instant()
        if (last != null && Duration.between(last, now) > idle) {
            expire(request, response)
            return
        }
        if (!isBackground(request)) {
            session.setAttribute(LAST_ACTIVITY, now)
        }
        filterChain.doFilter(request, response)
    }

    private fun expire(request: HttpServletRequest, response: HttpServletResponse) {
        val authentication = SecurityContextHolder.getContext().authentication
        rememberMeServices.logout(request, response, authentication)
        SecurityContextLogoutHandler().logout(request, response, authentication)
        val path = request.servletPath.ifEmpty { request.requestURI }
        if (path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        } else {
            response.sendRedirect("/login?idle")
        }
    }

    private fun isBackground(request: HttpServletRequest): Boolean {
        return request.getHeader("X-PetCare-Background") == "1"
    }

    companion object {
        const val LAST_ACTIVITY = "lastUserActivity"
    }
}
