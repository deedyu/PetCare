package com.petcare.petcare.security

import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 컨트롤러가 세션에 넣어 둔 객체를 믿지 않고, 인증된 사용자 id로 DB를 조회한다.
 */
@Component
class CurrentUser(
    private val userRepository: UserRepository
) {
    fun optional(): User? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal !is PetCareUserDetails) {
            return null
        }
        return userRepository.findById(principal.id).orElse(null)
    }
}
