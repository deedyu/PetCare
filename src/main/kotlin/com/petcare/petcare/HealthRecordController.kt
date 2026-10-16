package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.HealthRecordService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Controller
class HealthRecordController(
    private val petService: PetService,
    private val healthRecordService: HealthRecordService,
    private val familyService: FamilyService
) {

    @GetMapping("/pets/{petId}/health")
    fun healthRecords(@PathVariable petId: Long, model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        
        if (!familyService.canAccessPet(user.id!!, petId)) {
            return "redirect:/?error=unauthorized"
        }
        
        val records = healthRecordService.getHealthRecordsByPet(petId)
        
        // 타입별로 분류
        val weightRecords = records.filter { it.recordType == "WEIGHT" }
        val vaccinationRecords = records.filter { it.recordType == "VACCINATION" }
        val hospitalRecords = records.filter { it.recordType == "HOSPITAL" }
        val medicationRecords = records.filter { it.recordType == "MEDICATION" }
        val today = LocalDate.now()
        val withDue = vaccinationRecords.filter { it.nextDueDate != null }

        model.addAttribute("pet", pet)
        model.addAttribute("records", records)
        model.addAttribute("weightRecords", weightRecords)
        model.addAttribute("vaccinationRecords", vaccinationRecords)
        model.addAttribute("hospitalRecords", hospitalRecords)
        model.addAttribute("medicationRecords", medicationRecords)
        model.addAttribute("upcomingVaccines", withDue.filter { !it.nextDueDate!!.isBefore(today) }.sortedBy { it.nextDueDate })
        model.addAttribute("overdueVaccines", withDue.filter { it.nextDueDate!!.isBefore(today) }.sortedBy { it.nextDueDate })
        model.addAttribute("weightChart", healthRecordService.weightChart(weightRecords))
        model.addAttribute("user", user)
        return "health-records"
    }

    @GetMapping("/pets/{petId}/health/new")
    fun newHealthRecord(@PathVariable petId: Long, @RequestParam type: String, model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        
        if (!familyService.canAccessPet(user.id!!, petId)) {
            return "redirect:/?error=unauthorized"
        }
        model.addAttribute("pet", pet)
        model.addAttribute("recordType", type)
        model.addAttribute("user", user)
        return "health-record-form"
    }

    @GetMapping("/pets/{petId}/health/{recordId}/edit")
    fun editHealthRecord(
        @PathVariable petId: Long,
        @PathVariable recordId: Long,
        model: Model,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        val record = healthRecordService.getHealthRecord(recordId)
        
        if (record.pet?.id != petId || !healthRecordService.canChange(user.id!!, record)) {
            return "redirect:/?error=unauthorized"
        }
        
        model.addAttribute("pet", pet)
        model.addAttribute("record", record)
        model.addAttribute("user", user)
        return "health-record-edit"
    }

    @PostMapping("/pets/{petId}/health")
    fun createHealthRecord(
        @PathVariable petId: Long,
        @RequestParam recordType: String,
        @RequestParam recordDate: String,
        @RequestParam title: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) weight: String?,
        @RequestParam(required = false) vaccinationType: String?,
        @RequestParam(required = false) hospitalName: String?,
        @RequestParam(required = false) medicationName: String?,
        @RequestParam(required = false) medicationDosage: String?,
        @RequestParam(required = false) nextDueDate: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        
        if (!familyService.canAccessPet(user.id!!, petId)) {
            return "redirect:/?error=unauthorized"
        }
        
        val parsedDate = try {
            localDateOrThrow(recordDate) ?: return "redirect:/pets/$petId/health/new?type=$recordType"
        } catch (_: IllegalArgumentException) {
            return "redirect:/pets/$petId/health/new?type=$recordType"
        }
        val parsedNext = try {
            localDateOrThrow(nextDueDate)
        } catch (_: IllegalArgumentException) {
            return "redirect:/pets/$petId/health/new?type=$recordType"
        }
        val parsedWeight = weight?.takeIf { it.isNotBlank() }?.toDoubleOrNull()

        healthRecordService.createHealthRecord(
            petId = petId,
            recordType = recordType,
            recordDate = parsedDate,
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            weight = parsedWeight,
            vaccinationType = vaccinationType?.takeIf { it.isNotBlank() },
            hospitalName = hospitalName?.takeIf { it.isNotBlank() },
            medicationName = medicationName?.takeIf { it.isNotBlank() },
            medicationDosage = medicationDosage?.takeIf { it.isNotBlank() },
            nextDueDate = parsedNext,
            authorId = user.id
        )
        
        return "redirect:/pets/$petId/health"
    }

    @PostMapping("/pets/{petId}/health/{recordId}/update")
    fun updateHealthRecord(
        @PathVariable petId: Long,
        @PathVariable recordId: Long,
        @RequestParam recordDate: String,
        @RequestParam title: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) weight: String?,
        @RequestParam(required = false) vaccinationType: String?,
        @RequestParam(required = false) hospitalName: String?,
        @RequestParam(required = false) medicationName: String?,
        @RequestParam(required = false) medicationDosage: String?,
        @RequestParam(required = false) nextDueDate: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        val record = healthRecordService.getHealthRecord(recordId)
        
        if (record.pet?.id != petId || !healthRecordService.canChange(user.id!!, record)) {
            return "redirect:/?error=unauthorized"
        }
        
        val parsedDate = try {
            localDateOrThrow(recordDate) ?: return "redirect:/pets/$petId/health/$recordId/edit"
        } catch (_: IllegalArgumentException) {
            return "redirect:/pets/$petId/health/$recordId/edit"
        }
        val parsedNext = try {
            localDateOrThrow(nextDueDate)
        } catch (_: IllegalArgumentException) {
            return "redirect:/pets/$petId/health/$recordId/edit"
        }
        val parsedWeight = weight?.takeIf { it.isNotBlank() }?.toDoubleOrNull()

        healthRecordService.updateHealthRecord(
            recordId = recordId,
            recordDate = parsedDate,
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            weight = parsedWeight,
            vaccinationType = vaccinationType?.takeIf { it.isNotBlank() },
            hospitalName = hospitalName?.takeIf { it.isNotBlank() },
            medicationName = medicationName?.takeIf { it.isNotBlank() },
            medicationDosage = medicationDosage?.takeIf { it.isNotBlank() },
            nextDueDate = parsedNext
        )
        
        return "redirect:/pets/$petId/health"
    }

    @PostMapping("/pets/{petId}/health/{recordId}/delete")
    fun deleteHealthRecord(
        @PathVariable petId: Long,
        @PathVariable recordId: Long,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        val record = healthRecordService.getHealthRecord(recordId)
        
        if (record.pet?.id != petId || !healthRecordService.canChange(user.id!!, record)) {
            return "redirect:/?error=unauthorized"
        }
        
        healthRecordService.deleteHealthRecord(recordId)
        return "redirect:/pets/$petId/health"
    }
}

