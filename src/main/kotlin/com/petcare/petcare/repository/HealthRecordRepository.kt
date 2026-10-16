package com.petcare.petcare.repository

import com.petcare.petcare.entity.HealthRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface HealthRecordRepository : JpaRepository<HealthRecord, Long> {
    @Query(
        """
        select h from HealthRecord h
        left join fetch h.author
        where h.pet.id = :petId
        order by h.recordDate desc, h.id desc
        """
    )
    fun findByPetIdOrderByRecordDateDesc(petId: Long): List<HealthRecord>
    
    fun findByPetIdAndRecordTypeOrderByRecordDateDesc(petId: Long, recordType: String): List<HealthRecord>
    
    @Query("SELECT h FROM HealthRecord h JOIN FETCH h.pet WHERE h.pet.id = :petId ORDER BY h.recordDate DESC")
    fun findAllWithPetByPetId(petId: Long): List<HealthRecord>

    @Modifying(flushAutomatically = true)
    @Query("update HealthRecord h set h.author = null where h.author.id = :userId")
    fun clearAuthor(userId: Long)
}



