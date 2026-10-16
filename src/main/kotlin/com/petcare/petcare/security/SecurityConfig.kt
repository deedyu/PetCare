package com.petcare.petcare.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.util.matcher.RequestMatcher
import com.petcare.petcare.repository.UserRepository

@Configuration
class SecurityConfig(
    private val loginSuccessHandler: LoginSuccessHandler,
    private val loginFailureHandler: LoginFailureHandler,
    private val loginAttemptTracker: LoginAttemptTracker
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun sessionUserSyncFilter(userRepository: UserRepository) = SessionUserSyncFilter(userRepository)

    @Bean
    fun loginThrottleFilter() = LoginThrottleFilter(loginAttemptTracker)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionUserSyncFilter: SessionUserSyncFilter,
        loginThrottleFilter: LoginThrottleFilter
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/login", "/register", "/admin/login", "/error", "/access-denied").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(loginSuccessHandler)
                    .failureHandler(loginFailureHandler)
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }
            .sessionManagement { session ->
                session.sessionFixation().changeSessionId()
            }
            .headers { headers ->
                headers.referrerPolicy { referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)
                }
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .accessDeniedPage("/access-denied")
                    .defaultAuthenticationEntryPointFor(
                        HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        apiRequest()
                    )
                    .defaultAccessDeniedHandlerFor(
                        AccessDeniedHandler { _, response, _ ->
                            response.sendError(HttpServletResponse.SC_FORBIDDEN)
                        },
                        apiRequest()
                    )
            }
        http.addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.addFilterAfter(sessionUserSyncFilter, LogoutFilter::class.java)
        return http.build()
    }

    @Bean
    fun loginThrottleFilterRegistration(filter: LoginThrottleFilter): FilterRegistrationBean<LoginThrottleFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    /**
     * 필터를 보안 체인에만 두고, 서블릿 필터로 한 번 더 등록되지 않게 한다.
     */
    @Bean
    fun sessionUserSyncFilterRegistration(filter: SessionUserSyncFilter): FilterRegistrationBean<SessionUserSyncFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    private fun apiRequest() = RequestMatcher { request ->
        val path = request.servletPath.ifEmpty { request.requestURI }
        path.startsWith("/api/")
    }
}
