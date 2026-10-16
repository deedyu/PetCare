package com.petcare.petcare.repository

import com.petcare.petcare.entity.FamilyInvitation
import com.petcare.petcare.entity.InviteStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface FamilyInvitationRepository : JpaRepository<FamilyInvitation, Long> {
    fun findByCode(code: String): FamilyInvitation?

    fun findByFamilyIdAndStatus(familyId: Long, status: InviteStatus): List<FamilyInvitation>

    fun deleteByFamilyId(familyId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update FamilyInvitation i set i.acceptedBy = null where i.acceptedBy.id = :userId")
    fun clearAcceptedBy(userId: Long)

    @Modifying(flushAutomatically = true)
    @Query("update FamilyInvitation i set i.createdBy = null where i.createdBy.id = :userId")
    fun clearCreatedBy(userId: Long)
}
