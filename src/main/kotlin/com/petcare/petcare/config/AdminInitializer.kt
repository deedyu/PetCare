package com.petcare.petcare.config

import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.security.PasswordRules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 기존 평문 비밀번호를 BCrypt로 바꾸고, admin 계정에 ADMIN 역할을 붙인다.
 * 초기 비밀번호는 환경 변수에서만 읽는다. 이미 있는 계정은 해시를 다시 쓰지 않는다.
 */
@Component
class AdminInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${petcare.admin.initial-password:}")
    private val initialAdminPassword: String
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {
        userRepository.findAll().forEach { user ->
            var changed = false
            if (!PasswordRules.isBcrypt(user.password)) {
                user.password = hash(user.password)
                changed = true
            }
            if (user.username == "admin" && user.effectiveRole() != Role.ADMIN) {
                user.role = Role.ADMIN
                changed = true
            } else if (user.role == null) {
                user.role = Role.USER
                changed = true
            }
            if (changed) {
                userRepository.save(user)
            }
        }

        if (userRepository.findByUsername("admin") == null) {
            val raw = initialAdminPassword.trim()
            if (raw.isEmpty()) {
                throw IllegalStateException(
                    "환경 변수 PETCARE_ADMIN_INITIAL_PASSWORD 가 없습니다. " +
                        "admin 계정이 없을 때만 사용하며, 이미 저장된 비밀번호 해시는 바꾸지 않습니다. " +
                        "프로젝트 루트의 .env 를 확인하세요."
                )
            }
            userRepository.save(
                User(
                    username = "admin",
                    password = hash(raw),
                    name = "관리자",
                    role = Role.ADMIN
                )
            )
        }
    }

    private fun hash(raw: String): String {
        return passwordEncoder.encode(raw) ?: throw IllegalStateException("비밀번호를 저장하지 못했습니다.")
    }
}
