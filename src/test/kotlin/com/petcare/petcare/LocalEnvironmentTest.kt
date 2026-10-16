package com.petcare.petcare

import com.petcare.petcare.config.DotenvLoader
import com.petcare.petcare.config.requireMysqlCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class LocalEnvironmentTest {

    @Test
    fun `dotenv는 주석과 따옴표를 빼고 값만 읽는다`() {
        val parsed = DotenvLoader.parse(
            """
            # comment
            SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/petcare"
            export SPRING_DATASOURCE_USERNAME=root
            SPRING_DATASOURCE_PASSWORD=
            """.trimIndent()
        )
        assertEquals("jdbc:mysql://localhost:3306/petcare", parsed["SPRING_DATASOURCE_URL"])
        assertEquals("root", parsed["SPRING_DATASOURCE_USERNAME"])
        assertEquals("", parsed["SPRING_DATASOURCE_PASSWORD"])
    }

    @Test
    fun `mysql 비밀번호가 없으면 값은 숨기고 변수 이름만 안내한다`() {
        val environment = MockEnvironment()
        environment.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/petcare")
        environment.setProperty("spring.datasource.username", "app-user")
        environment.setProperty("spring.datasource.password", "")

        val error = assertThrows(IllegalStateException::class.java) {
            requireMysqlCredentials(environment)
        }
        val message = error.message.orEmpty()
        assertTrue(message.contains("SPRING_DATASOURCE_PASSWORD"))
        assertTrue(message.contains("PETCARE_ADMIN_INITIAL_PASSWORD"))
        assertFalse(message.contains("app-user"))
        assertFalse(message.contains("jdbc:mysql"))
    }

    @Test
    fun `h2 테스트 설정은 비밀번호 검사를 건너뛴다`() {
        val environment = MockEnvironment()
        environment.setProperty("spring.datasource.url", "jdbc:h2:mem:petcare")
        environment.setProperty("spring.datasource.password", "")
        requireMysqlCredentials(environment)
    }
}
