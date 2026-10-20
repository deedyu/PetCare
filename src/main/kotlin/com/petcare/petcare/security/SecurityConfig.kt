package com.petcare.petcare.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.util.matcher.RequestMatcher
import com.petcare.petcare.repository.UserRepository
import java.time.Clock
import java.time.Duration

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
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun rememberMeServices(
        userDetailsService: PetCareUserDetailsService,
        tokens: RememberMeTokenService,
        @Value("\${petcare.remember.key:petcare-local-remember-key}") key: String,
        @Value("\${petcare.remember.days:30}") days: Long
    ): HashedRememberMeServices {
        val services = HashedRememberMeServices(key, userDetailsService, tokens)
        services.setCookieName(REMEMBER_COOKIE)
        services.setTokenValiditySeconds((days * 24 * 60 * 60).toInt())
        return services
    }

    @Bean
    fun idleSessionFilter(
        rememberMeServices: HashedRememberMeServices,
        clock: Clock,
        @Value("\${petcare.session.idle-seconds:1800}") idleSeconds: Long
    ) = IdleSessionFilter(Duration.ofSeconds(idleSeconds), clock, rememberMeServices)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionUserSyncFilter: SessionUserSyncFilter,
        loginThrottleFilter: LoginThrottleFilter,
        rememberMeServices: HashedRememberMeServices,
        idleSessionFilter: IdleSessionFilter
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/login",
                        "/register",
                        "/register/**",
                        "/forgot",
                        "/forgot/**",
                        "/admin/login",
                        "/error",
                        "/access-denied",
                        "/css/**",
                        "/js/**"
                    ).permitAll()
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
                    .deleteCookies("JSESSIONID", REMEMBER_COOKIE)
                    .addLogoutHandler(rememberMeServices)
                    .permitAll()
            }
            .rememberMe { remember ->
                remember.rememberMeServices(rememberMeServices)
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
                    .authenticationEntryPoint(browserOrApiEntryPoint())
                    .accessDeniedPage("/access-denied")
                    .defaultAccessDeniedHandlerFor(
                        AccessDeniedHandler { _, response, _ ->
                            response.sendError(HttpServletResponse.SC_FORBIDDEN)
                        },
                        apiRequest()
                    )
            }
        http.addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.addFilterAfter(idleSessionFilter, RememberMeAuthenticationFilter::class.java)
        http.addFilterAfter(sessionUserSyncFilter, IdleSessionFilter::class.java)
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

    @Bean
    fun idleSessionFilterRegistration(filter: IdleSessionFilter): FilterRegistrationBean<IdleSessionFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    companion object {
        const val REMEMBER_COOKIE = "PETCARE_REMEMBER"
    }

    private fun apiRequest() = RequestMatcher { request ->
        requestPath(request).startsWith("/api/")
    }

    private fun browserOrApiEntryPoint(): AuthenticationEntryPoint {
        val login = LoginUrlAuthenticationEntryPoint("/login")
        val api = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
        return AuthenticationEntryPoint { request, response, exception ->
            if (requestPath(request).startsWith("/api/")) {
                api.commence(request, response, exception)
            } else {
                login.commence(request, response, exception)
            }
        }
    }

    private fun requestPath(request: HttpServletRequest): String {
        return request.servletPath.ifEmpty { request.requestURI }.substringBefore('?')
    }
}
