package com.petcare.petcare.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import java.io.File

/**
 * 프로젝트 루트의 .env 를 읽는다.
 * 이미 셸에 있는 환경 변수가 우선이고, 값은 로그에 남기지 않는다.
 */
class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        DotenvLoader.loadInto(environment)
    }
}

/**
 * MySQL로 접속하는데 주소, 계정, 비밀번호 중 하나가 비어 있으면 기동을 멈춘다.
 * 오류 문구에는 변수 이름만 넣고 값은 넣지 않는다.
 */
class DatasourceEnvironmentGuard : EnvironmentPostProcessor, Ordered {
    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        requireMysqlCredentials(environment)
    }
}

object DotenvLoader {
    const val SOURCE_NAME = "petcareDotenv"

    fun loadInto(environment: ConfigurableEnvironment) {
        if (environment.propertySources.contains(SOURCE_NAME)) {
            return
        }
        val file = File(System.getProperty("user.dir"), ".env")
        if (!file.isFile) {
            return
        }
        val values = parse(file.readText())
        if (values.isEmpty()) {
            return
        }
        environment.propertySources.addLast(MapPropertySource(SOURCE_NAME, values))
    }

    fun parse(text: String): Map<String, Any> {
        val values = linkedMapOf<String, Any>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@forEach
            }
            val body = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
            val separator = body.indexOf('=')
            if (separator <= 0) {
                return@forEach
            }
            val key = body.substring(0, separator).trim()
            val value = unquote(body.substring(separator + 1).trim())
            if (key.isNotEmpty()) {
                values[key] = value
            }
        }
        return values
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length - 1)
        }
        return value
    }
}

fun requireMysqlCredentials(environment: Environment) {
    val url = environment.getProperty("spring.datasource.url").orEmpty()
    if (url.startsWith("jdbc:h2")) {
        return
    }
    val missing = mutableListOf<String>()
    if (!url.startsWith("jdbc:mysql:")) {
        missing += "SPRING_DATASOURCE_URL"
    }
    if (environment.getProperty("spring.datasource.username").isNullOrBlank()) {
        missing += "SPRING_DATASOURCE_USERNAME"
    }
    if (environment.getProperty("spring.datasource.password").isNullOrBlank()) {
        missing += "SPRING_DATASOURCE_PASSWORD"
    }
    if (environment.getProperty("petcare.admin.initial-password").isNullOrBlank()) {
        missing += "PETCARE_ADMIN_INITIAL_PASSWORD"
    }
    if (missing.isEmpty()) {
        return
    }
    throw IllegalStateException(
        "데이터베이스 접속 환경 변수가 없습니다: ${missing.joinToString(", ")}. " +
            "프로젝트 루트의 .env 파일을 확인하세요. .env.example 을 복사한 뒤 값을 채우면 됩니다."
    )
}
