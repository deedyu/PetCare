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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
        ).andExpect(redirectedUrl("/admin"))
            .andReturn()

        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(get("/").session(session))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
        mockMvc.perform(get("/admin").session(session))
            .andExpect(status().isOk)
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
