package com.petcare.petcare.config

import jakarta.servlet.http.HttpSessionEvent
import jakarta.servlet.http.HttpSessionListener
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SessionConfig {

    @Bean
    fun httpSessionListener(): ServletListenerRegistrationBean<HttpSessionListener> {
        return ServletListenerRegistrationBean(object : HttpSessionListener {
            override fun sessionCreated(se: HttpSessionEvent) {
                se.session.maxInactiveInterval = 1800 // 30분
            }

            override fun sessionDestroyed(se: HttpSessionEvent) {
                // 세션 삭제 시 처리
            }
        })
    }
}

