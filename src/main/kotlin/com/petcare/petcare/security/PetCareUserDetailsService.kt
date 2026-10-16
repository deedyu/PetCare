package com.petcare.petcare.security

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class PetCareUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")
        val id = user.id ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")
        return PetCareUserDetails(
            id = id,
            loginId = user.username,
            passwordHash = user.password,
            role = user.effectiveRole(),
            name = user.name,
            enabled = user.effectiveStatus() == AccountStatus.ACTIVE
        )
    }
}
