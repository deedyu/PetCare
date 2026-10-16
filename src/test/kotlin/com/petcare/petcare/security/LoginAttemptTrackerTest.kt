package com.petcare.petcare.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl

class LoginAttemptTrackerTest {
    @Test
    fun fifthFailureLocksAndSuccessClears() {
        val tracker = LoginAttemptTracker(maxAttempts = 5, windowSeconds = 900)
        val key = tracker.key("member", "127.0.0.1")
        repeat(4) { tracker.recordFailure(key) }
        assertFalse(tracker.isBlocked(key))
        tracker.recordFailure(key)
        assertTrue(tracker.isBlocked(key))
        tracker.recordSuccess(key)
        assertFalse(tracker.isBlocked(key))
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class LoginThrottleTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun repeatedFailureStopsBeforePasswordCheck() {
        val username = "throttle-user"
        repeat(5) {
            mockMvc.perform(
                post("/login").with(csrf())
                    .param("username", username)
                    .param("password", "wrong-pass")
            ).andExpect(redirectedUrl("/login?error"))
        }
        mockMvc.perform(
            post("/login").with(csrf())
                .param("username", username)
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/login?locked"))
    }
}
