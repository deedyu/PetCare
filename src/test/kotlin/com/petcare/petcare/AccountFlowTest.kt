package com.petcare.petcare

import com.petcare.petcare.entity.Role
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.security.RecoveryCodes
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.UserService
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class AccountFlowTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var petService: PetService

    @Test
    fun registerPageHasNoEmailAndNoAdminLink() {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("중복 확인")))
            .andExpect(content().string(containsString("비밀번호 확인")))
            .andExpect(content().string(not(containsString("type=\"email\""))))
            .andExpect(content().string(not(containsString("/admin/login"))))
    }

    @Test
    fun usernameCheckAndDuplicateRegistration() {
        userService.createUser("takenid", "abcd1234", "기존")
        mockMvc.perform(get("/register/username-check").param("username", "takenid"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("이미 사용 중인 아이디입니다.")))
        mockMvc.perform(get("/register/username-check").param("username", "Ab"))
            .andExpect(content().string(containsString("아이디는 영문 소문자와 숫자 4~20자여야 합니다.")))
        mockMvc.perform(get("/register/username-check").param("username", "freshid1"))
            .andExpect(content().string(containsString("사용 가능한 아이디입니다.")))

        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "takenid")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", "중복")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("이미 사용 중인 아이디입니다.")))
        assertEquals(1, userRepository.findAll().count { it.username == "takenid" })
    }

    @Test
    fun registerRejectsWeakOrMismatchedPasswordWithoutEmail() {
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "weakuser")
                .param("password", "short1")
                .param("passwordConfirm", "short1")
                .param("name", "짧은비밀번호")
        ).andExpect(content().string(containsString("비밀번호는 8자 이상이어야 합니다.")))
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "letteruser")
                .param("password", "abcdefgh")
                .param("passwordConfirm", "abcdefgh")
                .param("name", "문자만")
        ).andExpect(content().string(containsString("비밀번호는 영문과 숫자를 모두 포함해야 합니다.")))
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "mismatch1")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1235")
                .param("name", "불일치")
        ).andExpect(content().string(containsString("비밀번호 확인이 일치하지 않습니다.")))
        assertNull(userRepository.findByUsername("weakuser"))
        assertNull(userRepository.findByUsername("mismatch1"))
    }

    @Test
    fun registerLoginLogoutAndRecoveryRotation() {
        val registered = mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "careuser1")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", "케어")
        ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/register/complete"))
            .andReturn()
        val code = registered.flashMap["recoveryCode"] as String
        val user = userRepository.findByUsername("careuser1")
        assertNotNull(user)
        assertEquals(Role.USER, user.effectiveRole())
        assertNull(user.email)
        assertTrue(PasswordRules.isBcrypt(user.password))
        assertTrue(user.recoveryCodeHash != null && user.recoveryCodeHash != code)
        assertTrue(passwordEncoder.matches(RecoveryCodes.normalize(code), user.recoveryCodeHash))

        mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "careuser1")
                .param("password", "wrong123")
        ).andExpect(redirectedUrl("/login?error"))

        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "careuser1")
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/")).andReturn()
        val session = login.request.getSession(false) as MockHttpSession
        mockMvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(redirectedUrl("/login?logout"))

        mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .param("username", "careuser1")
                .param("recoveryCode", "WRONG-CODE-0000")
        ).andExpect(content().string(containsString("아이디 또는 복구 코드를 확인할 수 없습니다.")))
        mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .param("username", "nobodyuser")
                .param("recoveryCode", code)
        ).andExpect(content().string(containsString("아이디 또는 복구 코드를 확인할 수 없습니다.")))

        val verified = mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .param("username", "careuser1")
                .param("recoveryCode", code)
        ).andExpect(redirectedUrl("/forgot")).andReturn()
        val proof = verified.request.getSession(false) as MockHttpSession
        val reset = mockMvc.perform(
            post("/forgot/reset").session(proof).with(csrf())
                .param("password", "efgh5678")
                .param("passwordConfirm", "efgh5678")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("비밀번호를 변경했습니다")))
            .andReturn()
        val newCode = reset.response.contentAsString.substringAfter("id=\"recoveryCode\"").substringAfter(">").substringBefore("<").trim()
        assertTrue(newCode.isNotBlank())
        assertTrue(newCode != code)

        mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .param("username", "careuser1")
                .param("recoveryCode", code)
        ).andExpect(content().string(containsString("아이디 또는 복구 코드를 확인할 수 없습니다.")))

        mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "careuser1")
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/login?error"))
        val relogin = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "careuser1")
                .param("password", "efgh5678")
        ).andExpect(redirectedUrl("/")).andReturn()
        val member = relogin.request.getSession(false) as MockHttpSession

        val otherLogin = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "careuser1")
                .param("password", "efgh5678")
        ).andExpect(redirectedUrl("/")).andReturn()
        val other = otherLogin.request.getSession(false) as MockHttpSession
        mockMvc.perform(get("/mypage").session(other)).andExpect(status().isOk)

        mockMvc.perform(
            post("/mypage/password").session(member).with(csrf())
                .param("currentPassword", "wrong123")
                .param("password", "ijkl9012")
                .param("passwordConfirm", "ijkl9012")
        ).andExpect(content().string(containsString("현재 비밀번호가 올바르지 않습니다.")))
        mockMvc.perform(
            post("/mypage/password").session(member).with(csrf())
                .param("currentPassword", "efgh5678")
                .param("password", "ijkl9012")
                .param("passwordConfirm", "ijkl9012")
        ).andExpect(content().string(containsString("비밀번호를 변경했습니다")))
        mockMvc.perform(get("/mypage").session(member)).andExpect(status().isOk)
        mockMvc.perform(get("/admin").session(member)).andExpect(status().isForbidden)
        mockMvc.perform(get("/mypage").session(other)).andExpect(redirectedUrl("/login?reset"))
    }

    @Test
    fun existingMemberWithoutCodeCanIssueOneAfterLogin() {
        userService.createUser("oldmember", "abcd1234", "기존회원")
        assertNull(userRepository.findByUsername("oldmember")!!.recoveryCodeHash)
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "oldmember")
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/")).andReturn()
        val session = login.request.getSession(false) as MockHttpSession
        val issued = mockMvc.perform(
            post("/mypage/recovery").session(session).with(csrf())
                .param("currentPassword", "abcd1234")
        ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/mypage"))
            .andReturn()
        val code = issued.flashMap["recoveryCode"] as String
        val hash = userRepository.findByUsername("oldmember")!!.recoveryCodeHash
        assertTrue(passwordEncoder.matches(RecoveryCodes.normalize(code), hash))
        mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "oldmember")
                .param("recoveryCode", "nope")
        ).andExpect(content().string(not(containsString("존재하지 않"))))
    }

    @Test
    fun recoveryCodeLocksAfterRepeatedFailures() {
        val registered = mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "lockuser1")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", "잠금")
        ).andReturn()
        val code = registered.flashMap["recoveryCode"] as String
        repeat(5) {
            mockMvc.perform(
                post("/forgot/verify").with(csrf())
                    .param("username", "lockuser1")
                    .param("recoveryCode", "WRONG-CODE-0000")
            )
        }
        mockMvc.perform(
            post("/forgot/verify").with(csrf())
                .param("username", "lockuser1")
                .param("recoveryCode", code)
        ).andExpect(content().string(containsString("시도가 많습니다")))
    }

    @Test
    fun memberPagesRenderWithSharedLayout() {
        mockMvc.perform(
            post("/register").with(csrf())
                .param("username", "uiuser1018")
                .param("password", "abcd1234")
                .param("passwordConfirm", "abcd1234")
                .param("name", "화면확인")
        ).andExpect(status().is3xxRedirection)
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", "uiuser1018")
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/")).andReturn()
        val session = login.request.getSession(false) as MockHttpSession
        val user = userRepository.findByUsername("uiuser1018")!!
        val pet = petService.createPet(user, "보리", "강아지", 3)
        val petId = pet.id!!

        listOf(
            "/",
            "/calendar",
            "/calendar?view=week",
            "/calendar?view=day",
            "/pets",
            "/pets/new",
            "/family",
            "/notifications",
            "/inquiries",
            "/inquiries/new",
            "/mypage",
            "/pets/$petId/health",
            "/pets/$petId/health/new?type=WEIGHT",
            "/pets/$petId/tasks"
        ).forEach { path ->
            mockMvc.perform(get(path).session(session))
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("app.css")))
                .andExpect(content().string(not(containsString("관리자 로그인"))))
        }
        mockMvc.perform(get("/").session(session))
            .andExpect(content().string(containsString("화면확인님, 안녕하세요")))
            .andExpect(content().string(containsString("보리")))
        mockMvc.perform(get("/pets/$petId/health").session(session))
            .andExpect(content().string(containsString("체중 기록이 없어요")))
    }
}
