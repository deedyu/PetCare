package com.petcare.petcare.repository

import com.petcare.petcare.entity.Pet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PetRepository : JpaRepository<Pet, Long> {
    fun findByUserIdOrderByIdDesc(userId: Long): List<Pet>
    fun findTop3ByUserIdOrderByIdDesc(userId: Long): List<Pet>

    fun findByFamilyIdOrderByIdDesc(familyId: Long): List<Pet>

    fun findByUserIdAndFamilyIsNullOrderByIdDesc(userId: Long): List<Pet>

    @Query(
        """
        select distinct p from Pet p
        left join fetch p.family
        left join fetch p.user
        where p.user.id = :userId
        or p.family.id in (select m.family.id from FamilyMember m where m.user.id = :userId)
        order by p.id desc
        """
    )
    fun findVisibleToUser(userId: Long): List<Pet>

    @Query(
        """
        select count(p) from Pet p
        where p.id = :petId and (
            p.user.id = :userId
            or p.family.id in (select m.family.id from FamilyMember m where m.user.id = :userId)
        )
        """
    )
    fun countVisible(petId: Long, userId: Long): Long

    @Modifying(flushAutomatically = true)
    @Query("update Pet p set p.family = null where p.family.id = :familyId")
    fun clearFamily(familyId: Long)
}
