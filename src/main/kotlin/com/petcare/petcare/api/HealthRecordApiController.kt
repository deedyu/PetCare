package com.petcare.petcare.api

import com.petcare.petcare.dto.HealthRecordDto
import com.petcare.petcare.localDateOrThrow
import com.petcare.petcare.entity.HealthRecord
import com.petcare.petcare.security.CurrentUser
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.HealthRecordService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/health-records")
class HealthRecordApiController(
    private val healthRecordService: HealthRecordService,
    private val currentUser: CurrentUser,
    private val familyService: FamilyService
) {

    @GetMapping("/pet/{petId}")
    fun getHealthRecordsByPet(@PathVariable petId: Long): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        if (!ownsPet(userId, petId)) return forbidden()
        return ResponseEntity.ok(healthRecordService.getHealthRecordsByPet(petId).map { it.toDto() })
    }

    @GetMapping("/pet/{petId}/type/{recordType}")
    fun getHealthRecordsByPetAndType(
        @PathVariable petId: Long,
        @PathVariable recordType: String
    ): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        if (!ownsPet(userId, petId)) return forbidden()
        return ResponseEntity.ok(
            healthRecordService.getHealthRecordsByPetAndType(petId, recordType).map { it.toDto() }
        )
    }

    @GetMapping("/{recordId}")
    fun getHealthRecord(@PathVariable recordId: Long): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        val record = try {
            healthRecordService.getHealthRecord(recordId)
        } catch (e: IllegalArgumentException) {
            return forbidden()
        }
        val petId = record.pet?.id ?: return forbidden()
        if (!familyService.canAccessPet(userId, petId)) return forbidden()
        return ResponseEntity.ok(record.toDto())
    }

    @PostMapping
    fun createHealthRecord(@RequestBody request: CreateHealthRecordRequest): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        if (!ownsPet(userId, request.petId)) return forbidden()
        return try {
            val record = healthRecordService.createHealthRecord(
                petId = request.petId,
                recordType = request.recordType,
                recordDate = localDateOrThrow(request.recordDate)
                    ?: throw IllegalArgumentException("날짜를 입력하세요."),
                title = request.title,
                description = request.description,
                weight = request.weight,
                vaccinationType = request.vaccinationType,
                hospitalName = request.hospitalName,
                medicationName = request.medicationName,
                medicationDosage = request.medicationDosage,
                nextDueDate = localDateOrThrow(request.nextDueDate),
                authorId = userId
            )
            ResponseEntity.ok(record.toDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PutMapping("/{recordId}")
    fun updateHealthRecord(
        @PathVariable recordId: Long,
        @RequestBody request: UpdateHealthRecordRequest
    ): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        if (ownedRecord(userId, recordId) == null) return forbidden()
        return try {
            val record = healthRecordService.updateHealthRecord(
                recordId = recordId,
                recordDate = localDateOrThrow(request.recordDate)
                    ?: throw IllegalArgumentException("날짜를 입력하세요."),
                title = request.title,
                description = request.description,
                weight = request.weight,
                vaccinationType = request.vaccinationType,
                hospitalName = request.hospitalName,
                medicationName = request.medicationName,
                medicationDosage = request.medicationDosage,
                nextDueDate = localDateOrThrow(request.nextDueDate)
            )
            ResponseEntity.ok(record.toDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @DeleteMapping("/{recordId}")
    fun deleteHealthRecord(@PathVariable recordId: Long): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (ownedRecord(userId, recordId) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        healthRecordService.deleteHealthRecord(recordId)
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): Long? = currentUser.optional()?.id

    private fun unauthorized() = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

    private fun forbidden() = ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()

    private fun ownsPet(userId: Long, petId: Long): Boolean = familyService.canAccessPet(userId, petId)

    private fun ownedRecord(userId: Long, recordId: Long): HealthRecord? {
        return try {
            val record = healthRecordService.getHealthRecord(recordId)
            if (healthRecordService.canChange(userId, record)) record else null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun HealthRecord.toDto() = HealthRecordDto(
        id = id,
        recordType = recordType,
        recordDate = recordDate,
        title = title,
        description = description,
        weight = weight,
        vaccinationType = vaccinationType,
        hospitalName = hospitalName,
        medicationName = medicationName,
        medicationDosage = medicationDosage,
        petId = pet?.id,
        authorName = author?.name,
        createdAt = createdAt,
        nextDueDate = nextDueDate
    )

    data class CreateHealthRecordRequest(
        val petId: Long,
        val recordType: String,
        val recordDate: String,
        val title: String,
        val description: String? = null,
        val weight: Double? = null,
        val vaccinationType: String? = null,
        val hospitalName: String? = null,
        val medicationName: String? = null,
        val medicationDosage: String? = null,
        val nextDueDate: String? = null
    )

    data class UpdateHealthRecordRequest(
        val recordDate: String,
        val title: String,
        val description: String? = null,
        val weight: Double? = null,
        val vaccinationType: String? = null,
        val hospitalName: String? = null,
        val medicationName: String? = null,
        val medicationDosage: String? = null,
        val nextDueDate: String? = null
    )
}
