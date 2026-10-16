package com.petcare.petcare.service

import com.petcare.petcare.entity.FamilyRole

data class FamilyMemberView(
    val userId: Long,
    val name: String,
    val username: String,
    val role: FamilyRole
)
