package com.petcare.petcare.repository

import com.petcare.petcare.entity.RememberMeToken
import org.springframework.data.jpa.repository.JpaRepository

interface RememberMeTokenRepository : JpaRepository<RememberMeToken, String> {
    fun deleteByUsername(username: String): Long
}
