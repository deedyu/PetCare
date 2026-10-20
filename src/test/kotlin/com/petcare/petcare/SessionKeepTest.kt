package com.petcare.petcare

import com.petcare.petcare.repository.RememberMeTokenRepository
import com.petcare.petcare.security.IdleSessionFilter
import com.petcare.petcare.security.SecurityConfig
import com.petcare.petcare.service.UserService
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class SessionKeepTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var rememberMeTokenRepository: RememberMeTokenRepository

    @Test
    fun loginPageOffersRememberMeAndAdminPageDoesNot() {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("name=\"remember-me\"")))
            .andExpect(content().string(not(containsString("checked"))))
        mockMvc.perform(get("/admin/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(not(containsString("remember-me"))))
        mockMvc.perform(get("/login").param("idle", ""))
            .andExpect(content().string(containsString("일정 시간 동안 사용하지 않아 로그아웃되었습니다.")))
    }

    @Test
    fun uncheckedLoginDoesNotSetRememberCookie() {
        register("plain1019", "그냥")
        val login = login("plain1019", remember = false)
        assertNull(login.response.getCookie(SecurityConfig.REMEMBER_COOKIE))
        mockMvc.perform(get("/"))
            .andExpect(redirectedUrl("/login"))
    }

    @Test
    fun checkedLoginSurvivesNewBrowserAndRotatesToken() {
        register("keep1019", "유지")
        val login = login("keep1019", remember = true)
        val first = rememberCookie(login)
        assertTrue(first.isHttpOnly)
        assertTrue(!first.secure)
        assertEquals("Lax", first.getAttribute("SameSite"))
        assertTrue(first.maxAge >= 29 * 24 * 60 * 60)
        val stored = rememberMeTokenRepository.findAll().filter { it.username == "keep1019" }
        assertEquals(1, stored.size)
        assertEquals(64, stored[0].tokenHash.length)
        assertTrue(first.value != stored[0].tokenHash)

        val restored = mockMvc.perform(get("/").cookie(first))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("유지님, 안녕하세요")))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andReturn()
        val rotated = rememberCookie(restored)
        assertTrue(rotated.value != first.value)

        mockMvc.perform(get("/").cookie(first))
            .andExpect(redirectedUrl("/login"))
        mockMvc.perform(get("/").cookie(rotated))
            .andExpect(redirectedUrl("/login"))
        assertEquals(0, rememberMeTokenRepository.findAll().count { it.username == "keep1019" })
    }

    @Test
    fun idleExpiryRevokesRememberMeSoItCannotSignInAgain() {
        register("idle1019", "자리비움")
        val login = login("idle1019", remember = true)
        val cookie = rememberCookie(login)
        val session = login.request.getSession(false) as MockHttpSession
        session.setAttribute(IdleSessionFilter.LAST_ACTIVITY, Instant.now().minusSeconds(1801))

        mockMvc.perform(get("/mypage").session(session).cookie(cookie))
            .andExpect(redirectedUrl("/login?idle"))

        mockMvc.perform(get("/").cookie(cookie))
            .andExpect(redirectedUrl("/login"))
        assertEquals(0, rememberMeTokenRepository.findAll().count { it.username == "idle1019" })
    }

    @Test
    fun manualLogoutRevokesOnlyThatBrowser() {
        register("multi1019", "여러기기")
        val firstLogin = login("multi1019", remember = true)
        val firstCookie = rememberCookie(firstLogin)
        val firstSession = firstLogin.request.getSession(false) as MockHttpSession
        val secondCookie = rememberCookie(login("multi1019", remember = true))

        mockMvc.perform(
            post("/logout").session(firstSession).cookie(firstCookie).with(csrf())
        ).andExpect(redirectedUrl("/login?logout"))

        mockMvc.perform(get("/").cookie(firstCookie))
            .andExpect(redirectedUrl("/login"))
        mockMvc.perform(get("/").cookie(secondCookie))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("여러기기님, 안녕하세요")))
    }

    @Test
    fun passwordChangeRevokesRememberMeAndMemberCannotOpenAdmin() {
        register("lock1019", "잠금회원")
        val login = login("lock1019", remember = true)
        val cookie = rememberCookie(login)
        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(get("/admin").session(session))
            .andExpect(status().isForbidden)
        val user = userService.findByUsername("lock1019")!!
        userService.changePassword(user.id!!, "abcd1234", "abcd5678", "abcd5678")
        mockMvc.perform(get("/").cookie(cookie))
            .andExpect(redirectedUrl("/login"))
    }

    @Test
    fun adminLoginDoesNotIssueRememberCookie() {
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "admin")
                .param("password", "admin")
                .param("from", "admin")
                .param("remember-me", "on")
        ).andExpect(redirectedUrl("/admin")).andReturn()
        assertNull(login.response.getCookie(SecurityConfig.REMEMBER_COOKIE))
        mockMvc.perform(get("/admin").session(login.request.getSession(false) as MockHttpSession))
            .andExpect(status().isOk)
    }

    @Test
    fun backgroundRequestDoesNotRefreshActivity() {
        register("back1019", "배경")
        val login = login("back1019", remember = false)
        val session = login.request.getSession(false) as MockHttpSession
        val stale = Instant.now().minusSeconds(60)
        session.setAttribute(IdleSessionFilter.LAST_ACTIVITY, stale)
        mockMvc.perform(
            get("/").session(session).header("X-PetCare-Background", "1")
        ).andExpect(status().isOk)
        assertEquals(stale, session.getAttribute(IdleSessionFilter.LAST_ACTIVITY))
    }

    private fun register(username: String, name: String) {
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", username)
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", name)
        ).andExpect(status().is3xxRedirection)
    }

    private fun login(username: String, remember: Boolean): MvcResult {
        val request = post("/login").with(csrf())
            .param("username", username)
            .param("password", "abcd1234")
        if (remember) {
            request.param("remember-me", "on")
        }
        return mockMvc.perform(request)
            .andExpect(redirectedUrl("/"))
            .andReturn()
    }

    private fun rememberCookie(result: MvcResult) =
        result.response.getCookie(SecurityConfig.REMEMBER_COOKIE) ?: error("로그인 유지 쿠키가 없습니다.")
}
