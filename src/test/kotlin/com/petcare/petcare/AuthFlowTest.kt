package com.petcare.petcare

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import com.petcare.petcare.entity.Role
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.service.UserService
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun loginPageDoesNotOfferAdminLink() {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(not(containsString("/admin/login"))))
            .andExpect(content().string(not(containsString("관리자 로그인"))))
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk)
            .andExpect(content().string(not(containsString("/admin/login"))))
    }

    @Test
    fun registerCannotCreateAdminAccount() {
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "admin")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", "가짜관리자")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("사용할 수 없는 아이디입니다.")))
        assertEquals(Role.ADMIN, userRepository.findByUsername("admin")!!.effectiveRole())
    }

    @Test
    fun memberCannotCallAdminUserApi() {
        userService.createUser("member-api", "abcd1234", "회원")
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "member-api")
                .param("password", "abcd1234")
        ).andReturn()
        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(
            post("/api/users")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"made-by-member","password":"abcd1234","name":"회원생성"}""")
        ).andExpect(status().isForbidden)
        mockMvc.perform(get("/admin/notices").session(session))
            .andExpect(status().isForbidden)
    }

    @Test
    fun anonymousApiIsRejected() {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/health-records/1"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun memberLoginOpensHomeAndBlocksAdmin() {
        userService.createUser("member-phase1", "abcd1234", "회원")
        val saved = userRepository.findByUsername("member-phase1")!!
        assertTrue(PasswordRules.isBcrypt(saved.password))
        assertNotEquals("abcd1234", saved.password)
        assertTrue(passwordEncoder.matches("abcd1234", saved.password))
        assertEquals(Role.USER, saved.effectiveRole())

        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "member-phase1")
                .param("password", "abcd1234")
        ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
            .andReturn()

        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(get("/admin").session(session))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/").session(session))
            .andExpect(status().isOk)
    }

    @Test
    fun adminLoginOpensAdminAndLeavesMemberHome() {
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "admin")
                .param("password", "admin")
                .param("from", "admin")
        ).andExpect(redirectedUrl("/admin"))
            .andReturn()

        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(get("/").session(session))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
        mockMvc.perform(get("/admin").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("대시보드")))
        mockMvc.perform(get("/admin/notices").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("공지사항 관리")))
        mockMvc.perform(get("/admin/inquiries").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("문의 관리")))
    }

    @Test
    fun memberCannotUseAdminDoor() {
        userService.createUser("member-door", "abcd1234", "회원")
        mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "member-door")
                .param("password", "abcd1234")
                .param("from", "admin")
        ).andExpect(redirectedUrl("/admin/login?denied"))
    }
}
