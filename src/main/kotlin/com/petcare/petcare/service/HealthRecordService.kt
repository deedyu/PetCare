package com.petcare.petcare.service

import com.petcare.petcare.entity.HealthRecord
import com.petcare.petcare.repository.HealthRecordRepository
import com.petcare.petcare.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class HealthRecordService(
    private val healthRecordRepository: HealthRecordRepository,
    private val petService: PetService,
    private val userRepository: UserRepository
) {

    fun getHealthRecordsByPet(petId: Long): List<HealthRecord> {
        return healthRecordRepository.findByPetIdOrderByRecordDateDesc(petId)
    }

    fun getHealthRecordsByPetAndType(petId: Long, recordType: String): List<HealthRecord> {
        return healthRecordRepository.findByPetIdAndRecordTypeOrderByRecordDateDesc(petId, recordType)
    }

    fun getHealthRecord(recordId: Long): HealthRecord {
        return healthRecordRepository.findById(recordId)
            .orElseThrow { IllegalArgumentException("health record not found: $recordId") }
    }

    @Transactional
    fun createHealthRecord(
        petId: Long,
        recordType: String,
        recordDate: LocalDate,
        title: String,
        description: String? = null,
        weight: Double? = null,
        vaccinationType: String? = null,
        hospitalName: String? = null,
        medicationName: String? = null,
        medicationDosage: String? = null,
        nextDueDate: LocalDate? = null,
        authorId: Long? = null
    ): HealthRecord {
        val pet = petService.getPet(petId)
        val record = HealthRecord(
            recordType = recordType,
            recordDate = recordDate,
            title = title,
            description = description,
            weight = weight,
            vaccinationType = vaccinationType,
            hospitalName = hospitalName,
            medicationName = medicationName,
            medicationDosage = medicationDosage,
            pet = pet,
            author = authorId?.let { userRepository.findById(it).orElse(null) },
            createdAt = LocalDateTime.now(),
            nextDueDate = nextDueDate
        )
        return healthRecordRepository.save(record)
    }

    @Transactional
    fun updateHealthRecord(
        recordId: Long,
        recordDate: LocalDate,
        title: String,
        description: String? = null,
        weight: Double? = null,
        vaccinationType: String? = null,
        hospitalName: String? = null,
        medicationName: String? = null,
        medicationDosage: String? = null,
        nextDueDate: LocalDate? = null
    ): HealthRecord {
        val record = healthRecordRepository.findById(recordId)
            .orElseThrow { IllegalArgumentException("health record not found: $recordId") }
        
        record.recordDate = recordDate
        record.title = title
        record.description = description
        record.weight = weight
        record.vaccinationType = vaccinationType
        record.hospitalName = hospitalName
        record.medicationName = medicationName
        record.medicationDosage = medicationDosage
        record.nextDueDate = nextDueDate
        
        return healthRecordRepository.save(record)
    }

    fun canChange(userId: Long, record: HealthRecord): Boolean {
        if (record.pet?.user?.id == userId) return true
        return record.author?.id == userId
    }

    fun weightChart(records: List<HealthRecord>): WeightChart? = HealthCharts.of(records)

    @Transactional
    fun deleteHealthRecord(recordId: Long) {
        healthRecordRepository.deleteById(recordId)
    }
}



