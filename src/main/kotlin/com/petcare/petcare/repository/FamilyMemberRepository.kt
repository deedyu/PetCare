package com.petcare.petcare.repository

import com.petcare.petcare.entity.FamilyMember
import com.petcare.petcare.entity.FamilyRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
    @Query(
        """
        select m from FamilyMember m
        join fetch m.user
        join fetch m.family
        where m.user.id = :userId
        order by m.id desc
        """
    )
    fun findByUserId(userId: Long): List<FamilyMember>

    @Query(
        """
        select m from FamilyMember m
        join fetch m.user
        where m.family.id = :familyId
        order by m.id
        """
    )
    fun findByFamilyId(familyId: Long): List<FamilyMember>

    fun findByFamilyIdAndUserId(familyId: Long, userId: Long): FamilyMember?

    fun existsByFamilyIdAndUserId(familyId: Long, userId: Long): Boolean

    fun countByFamilyId(familyId: Long): Long

    fun findByUserIdAndRole(userId: Long, role: FamilyRole): List<FamilyMember>

    fun deleteByFamilyId(familyId: Long)

    fun deleteByUserId(userId: Long)
}
