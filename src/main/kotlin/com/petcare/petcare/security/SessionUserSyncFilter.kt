package com.petcare.petcare.security

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.entity.Role
import com.petcare.petcare.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 기존 화면 컨트롤러는 세션의 user, admin 속성을 읽는다.
 * 이 필터가 인증 정보로 그 속성을 채우고, 관리자가 회원 화면으로 들어가지 않게 한다.
 */
class SessionUserSyncFilter(
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal is PetCareUserDetails) {
            val session = request.session
            val user = userRepository.findById(principal.id).orElse(null)
            if (user != null) {
                if (user.effectiveStatus() == AccountStatus.SUSPENDED) {
                    SecurityContextLogoutHandler().logout(
                        request,
                        response,
                        SecurityContextHolder.getContext().authentication
                    )
                    response.sendRedirect("/login?suspended")
                    return
                }
                session.setAttribute("user", user)
                if (user.effectiveRole() == Role.ADMIN) {
                    session.setAttribute("admin", user)
                } else {
                    session.removeAttribute("admin")
                }
            }
            if (principal.role == Role.ADMIN && isMemberPage(request.requestURI)) {
                response.sendRedirect("/admin")
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun isMemberPage(uri: String): Boolean {
        return uri == "/" || uri.startsWith("/pets") || uri.startsWith("/mypage") ||
            uri.startsWith("/calendar") || uri.startsWith("/family") ||
            uri.startsWith("/inquiries") || uri.startsWith("/notifications")
    }
}
