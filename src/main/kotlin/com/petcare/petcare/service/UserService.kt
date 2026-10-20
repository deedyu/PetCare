package com.petcare.petcare.service

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.security.RecoveryCodes
import com.petcare.petcare.security.RememberMeTokenService
import com.petcare.petcare.security.UsernameRules
import org.springframework.dao.DataIntegrityViolationException
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
    private val notificationService: NotificationService,
    private val rememberMeTokenService: RememberMeTokenService
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
        if (username.equals("admin", ignoreCase = true)) {
            throw IllegalArgumentException("사용할 수 없는 아이디입니다.")
        }
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

    /**
     * 공개 회원가입. 역할은 항상 USER이고 이메일은 받지 않는다.
     * 반환값은 이번 응답에서만 보여줄 복구 코드 원문이다.
     */
    @Transactional
    fun registerMember(username: String, password: String, name: String): String {
        val trimmedUsername = username.trim()
        val trimmedName = name.trim()
        UsernameRules.check(trimmedUsername)
        PasswordRules.check(password)
        if (trimmedName.isEmpty() || trimmedName.length > 30) {
            throw IllegalArgumentException("이름은 1~30자로 입력해 주세요.")
        }
        if (userRepository.findByUsername(trimmedUsername) != null) {
            throw IllegalArgumentException("이미 사용 중인 아이디입니다.")
        }
        val code = RecoveryCodes.generate()
        val user = User(
            username = trimmedUsername,
            password = hash(password),
            name = trimmedName,
            email = null,
            role = Role.USER,
            recoveryCodeHash = passwordEncoder.encode(RecoveryCodes.normalize(code))
        )
        try {
            userRepository.save(user)
        } catch (e: DataIntegrityViolationException) {
            throw IllegalArgumentException("이미 사용 중인 아이디입니다.")
        }
        return code
    }

    fun usernameAvailability(username: String): String? {
        val trimmed = username.trim()
        try {
            UsernameRules.check(trimmed)
        } catch (e: IllegalArgumentException) {
            return e.message
        }
        if (userRepository.findByUsername(trimmed) != null) {
            return "이미 사용 중인 아이디입니다."
        }
        return null
    }

    fun changeName(userId: Long, name: String): User {
        val user = getUserById(userId)
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 30) {
            throw IllegalArgumentException("이름은 1~30자로 입력해 주세요.")
        }
        user.name = trimmed
        return userRepository.save(user)
    }

    fun changePassword(userId: Long, currentPassword: String, newPassword: String, confirm: String): User {
        val user = getUserById(userId)
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.")
        }
        if (newPassword != confirm) {
            throw IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.")
        }
        PasswordRules.check(newPassword)
        user.password = hash(newPassword)
        endOtherSessions(user)
        return userRepository.save(user)
    }

    fun issueRecoveryCode(userId: Long, currentPassword: String): String {
        val user = getUserById(userId)
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.")
        }
        val code = RecoveryCodes.generate()
        user.recoveryCodeHash = passwordEncoder.encode(RecoveryCodes.normalize(code))
        userRepository.save(user)
        return code
    }

    /**
     * 아이디와 복구 코드가 맞으면 사용자 번호를 돌려준다.
     * 계정이 없거나 코드가 없어도 같은 실패로 본다.
     */
    fun recoveryUserId(username: String, code: String): Long? {
        val user = userRepository.findByUsername(username.trim())
        val hash = user?.recoveryCodeHash
        val normalized = RecoveryCodes.normalize(code)
        if (user == null || hash.isNullOrBlank() || user.effectiveStatus() == AccountStatus.SUSPENDED || user.effectiveRole() == Role.ADMIN) {
            passwordEncoder.matches(normalized, dummyRecoveryHash)
            return null
        }
        return if (passwordEncoder.matches(normalized, hash)) user.id else null
    }

    @Transactional
    fun resetPassword(userId: Long, newPassword: String, confirm: String): String {
        if (newPassword != confirm) {
            throw IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.")
        }
        PasswordRules.check(newPassword)
        val user = getUserById(userId)
        if (user.recoveryCodeHash.isNullOrBlank() || user.effectiveRole() == Role.ADMIN) {
            throw IllegalArgumentException("복구 코드를 다시 확인해 주세요.")
        }
        val code = RecoveryCodes.generate()
        user.password = hash(newPassword)
        endOtherSessions(user)
        user.recoveryCodeHash = passwordEncoder.encode(RecoveryCodes.normalize(code))
        userRepository.save(user)
        return code
    }

    fun checkCurrentPassword(userId: Long, currentPassword: String) {
        val user = getUserById(userId)
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.")
        }
    }

    fun updateUser(userId: Long, username: String, password: String, name: String, email: String? = null): User {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("user not found: $userId") }
        if (username.equals("admin", ignoreCase = true) && !user.username.equals("admin", ignoreCase = true)) {
            throw IllegalArgumentException("사용할 수 없는 아이디입니다.")
        }
        val existingUser = userRepository.findByUsername(username)
        if (existingUser != null && existingUser.id != userId) {
            throw IllegalArgumentException("이미 존재하는 아이디입니다.")
        }
        val previousUsername = user.username
        user.username = username
        user.name = name
        if (password.isNotBlank() && password != user.password) {
            PasswordRules.check(password)
            user.password = hash(password)
            endOtherSessions(user, previousUsername)
        } else if (previousUsername != username) {
            rememberMeTokenService.revokeUser(previousUsername)
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
        rememberMeTokenService.revokeUser(user.username)
        inquiryService.releaseUser(userId)
        notificationService.releaseUser(userId)
        familyService.releaseUser(userId)
        petService.getPetsByUser(userId).forEach { pet ->
            pet.id?.let { petService.deletePet(it) }
        }
        userRepository.delete(user)
    }

    private fun endOtherSessions(user: User, previousUsername: String = user.username) {
        rememberMeTokenService.revokeUser(previousUsername)
        if (previousUsername != user.username) {
            rememberMeTokenService.revokeUser(user.username)
        }
        user.sessionEpoch = user.sessionEpoch + 1
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

    private val dummyRecoveryHash: String by lazy {
        passwordEncoder.encode("unused-recovery-placeholder") ?: error("복구 코드 검증을 준비하지 못했습니다.")
    }

    private fun hash(raw: String): String {
        return passwordEncoder.encode(raw) ?: error("비밀번호 해시에 실패했습니다.")
    }
}
