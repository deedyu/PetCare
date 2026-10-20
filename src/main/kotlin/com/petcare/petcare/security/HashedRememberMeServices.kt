package com.petcare.petcare.security

import com.petcare.petcare.entity.Role
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices
import org.springframework.security.web.authentication.rememberme.InvalidCookieException
import java.time.Duration

/**
 * 일반 회원만 로그인 유지를 발급한다.
 * 쿠키에는 series와 일회용 비밀만 넣고, 서버에는 해시를 둔다.
 * 로컬 HTTP에서는 Secure를 켜지 않고, HTTPS 요청에서만 켠다.
 */
class HashedRememberMeServices(
    key: String,
    userDetailsService: UserDetailsService,
    private val tokens: RememberMeTokenService
) : AbstractRememberMeServices(key, userDetailsService) {

    override fun onLoginSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        successfulAuthentication: Authentication
    ) {
        val details = successfulAuthentication.principal as? PetCareUserDetails ?: return
        if (details.role == Role.ADMIN || request.getParameter("from") == "admin") {
            return
        }
        val issued = tokens.issue(details.username, Duration.ofSeconds(tokenValiditySeconds.toLong()))
        setCookie(arrayOf(issued.series, issued.secret), tokenValiditySeconds, request, response)
    }

    override fun processAutoLoginCookie(
        cookieTokens: Array<String>,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): UserDetails {
        if (cookieTokens.size != 2) {
            throw InvalidCookieException("로그인 유지 쿠키 형식이 올바르지 않습니다.")
        }
        val issued = tokens.rotate(cookieTokens[0], cookieTokens[1])
        val user = userDetailsService.loadUserByUsername(issued.username)
        if (user is PetCareUserDetails && user.role == Role.ADMIN) {
            tokens.revokeSeries(issued.series)
            throw InvalidCookieException("관리자 로그인 유지는 허용되지 않습니다.")
        }
        setCookie(arrayOf(issued.series, issued.secret), tokenValiditySeconds, request, response)
        return user
    }

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        revokePresentedSeries(request)
        super.logout(request, response, authentication)
    }

    fun revokePresentedSeries(request: HttpServletRequest) {
        val raw = extractRememberMeCookie(request) ?: return
        try {
            val parts = decodeCookie(raw)
            if (parts.size == 2) {
                tokens.revokeSeries(parts[0])
            }
        } catch (_: Exception) {
            // 깨진 쿠키는 세션 로그아웃만 진행한다.
        }
    }

    override fun setCookie(
        cookieTokens: Array<String>,
        maxAge: Int,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val cookie = Cookie(cookieName, encodeCookie(cookieTokens))
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.path = cookiePath(request)
        cookie.maxAge = maxAge
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)
    }

    override fun cancelCookie(request: HttpServletRequest, response: HttpServletResponse) {
        val cookie = Cookie(cookieName, "")
        cookie.maxAge = 0
        cookie.path = cookiePath(request)
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)
    }

    private fun cookiePath(request: HttpServletRequest): String {
        val context = request.contextPath
        return if (context.isNullOrBlank()) "/" else context
    }
}
