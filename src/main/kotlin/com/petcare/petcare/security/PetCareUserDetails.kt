package com.petcare.petcare.security

import com.petcare.petcare.entity.Role
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * 로그인 세션에 넣는 최소 정보.
 * 엔티티 전체를 넣지 않아서, 요청마다 DB에서 최신 사용자를 다시 읽을 수 있다.
 */
class PetCareUserDetails(
    val id: Long,
    private val loginId: String,
    private val passwordHash: String,
    val role: Role,
    val name: String,
    private val enabled: Boolean = true
) : UserDetails {
    override fun getAuthorities() = listOf(SimpleGrantedAuthority("ROLE_$role"))
    override fun getPassword() = passwordHash
    override fun getUsername() = loginId
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = enabled
}
