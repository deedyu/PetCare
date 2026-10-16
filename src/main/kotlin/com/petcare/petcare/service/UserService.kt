package com.petcare.petcare.service

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.security.PasswordRules
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val petService: PetService,
    private val familyService: FamilyService,
    private val inquiryService: InquiryService,
    private val notificationService: NotificationService
) {
    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun createUser(
        username: String,
        password: String,
        name: String,
        email: String? = null,
        role: Role = Role.USER
    ): User {
        if (userRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("이미 존재하는 아이디입니다.")
        }
        val normalizedEmail = email?.trim()?.ifBlank { null }
        if (normalizedEmail != null && userRepository.findByEmail(normalizedEmail) != null) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }
        val user = User(
            username = username,
            password = hash(password),
            name = name,
            email = normalizedEmail,
            role = role
        )
        return userRepository.save(user)
    }

    fun updateUser(userId: Long, username: String, password: String, name: String, email: String? = null): User {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("user not found: $userId") }
        val existingUser = userRepository.findByUsername(username)
        if (existingUser != null && existingUser.id != userId) {
            throw IllegalArgumentException("이미 존재하는 아이디입니다.")
        }
        user.username = username
        user.name = name
        if (password.isNotBlank() && password != user.password) {
            PasswordRules.check(password)
            user.password = hash(password)
        }
        if (email != null) {
            val normalizedEmail = email.trim().ifBlank { null }
            if (normalizedEmail != null) {
                val emailOwner = userRepository.findByEmail(normalizedEmail)
                if (emailOwner != null && emailOwner.id != userId) {
                    throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
                }
            }
            user.email = normalizedEmail
        }
        return userRepository.save(user)
    }

    @Transactional
    fun deleteUser(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("user not found: $userId") }
        if (user.effectiveRole() == Role.ADMIN) {
            throw IllegalArgumentException("관리자 계정은 삭제할 수 없습니다.")
        }
        inquiryService.releaseUser(userId)
        notificationService.releaseUser(userId)
        familyService.releaseUser(userId)
        petService.getPetsByUser(userId).forEach { pet ->
            pet.id?.let { petService.deletePet(it) }
        }
        userRepository.delete(user)
    }

    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("user not found: $userId") }
    }

    fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    @Transactional
    fun changeStatus(userId: Long, status: AccountStatus) {
        val user = getUserById(userId)
        if (user.effectiveRole() == Role.ADMIN) {
            throw IllegalArgumentException("관리자 계정은 정지할 수 없습니다.")
        }
        user.status = status
        userRepository.save(user)
    }

    private fun hash(raw: String): String {
        return passwordEncoder.encode(raw) ?: error("비밀번호 해시에 실패했습니다.")
    }
}
