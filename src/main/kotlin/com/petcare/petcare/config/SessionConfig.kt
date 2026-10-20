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
                // 열린 화면의 30분 미사용은 IdleSessionFilter가 판단한다.
                // 컨테이너 만료를 그보다 짧게 두면, 세션이 먼저 사라져 로그인 유지 쿠키가 바로 다시 로그인한다.
                se.session.maxInactiveInterval = 12 * 60 * 60
            }

            override fun sessionDestroyed(se: HttpSessionEvent) {
                // 세션 삭제 시 처리
            }
        })
    }
}

