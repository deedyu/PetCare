package com.petcare.petcare.service

import com.petcare.petcare.entity.HealthRecord
import com.petcare.petcare.entity.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthChartsTest {
    @Test
    fun lighterWeightSitsLowerAndMissingWeightIsSkipped() {
        val records = listOf(
            HealthRecord(recordType = "WEIGHT", recordDate = LocalDate.of(2026, 10, 16), title = "나중", weight = 4.0),
            HealthRecord(recordType = "WEIGHT", recordDate = LocalDate.of(2026, 10, 1), title = "처음", weight = 3.0),
            HealthRecord(recordType = "WEIGHT", recordDate = LocalDate.of(2026, 10, 8), title = "없음")
        )
        val chart = HealthCharts.of(records)!!
        assertEquals(LocalDate.of(2026, 10, 1), chart.points.first().date)
        assertEquals(4.0, chart.points.last().weight)
        assertTrue(chart.points.first().y > chart.points.last().y)
        assertEquals(2, chart.points.size)
    }
}

@SpringBootTest
class HealthRecordAuthorTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var healthRecordService: HealthRecordService
    @Autowired lateinit var familyService: FamilyService

    @Test
    @Transactional
    fun sharedPetRecordKeepsAuthorAndNextDueDate() {
        val owner = userService.createUser("health-owner", "abcd1234", "집사", role = Role.USER)
        val member = userService.createUser("health-member", "abcd1234", "동생", role = Role.USER)
        val outsider = userService.createUser("health-outsider", "abcd1234", "손님", role = Role.USER)
        val pet = petService.createPet(owner, "콩", "강아지", 2)
        val family = familyService.create(owner.id!!, "건강가족", null)
        val code = familyService.invite(owner.id!!, family.id!!).code
        familyService.join(member.id!!, code)
        familyService.sharePet(owner.id!!, family.id!!, pet.id!!)

        val due = LocalDate.of(2026, 12, 1)
        val record = healthRecordService.createHealthRecord(
            petId = pet.id!!,
            recordType = "VACCINATION",
            recordDate = LocalDate.of(2026, 10, 16),
            title = "종합백신",
            vaccinationType = "종합",
            nextDueDate = due,
            authorId = member.id
        )
        healthRecordService.createHealthRecord(
            petId = pet.id!!,
            recordType = "WEIGHT",
            recordDate = LocalDate.of(2026, 10, 1),
            title = "첫째",
            weight = 3.2,
            authorId = owner.id
        )
        healthRecordService.createHealthRecord(
            petId = pet.id!!,
            recordType = "WEIGHT",
            recordDate = LocalDate.of(2026, 10, 16),
            title = "둘째",
            weight = 3.5,
            authorId = owner.id
        )

        assertEquals("동생", record.author?.name)
        assertEquals(due, record.nextDueDate)
        assertTrue(healthRecordService.canChange(member.id!!, record))
        assertTrue(healthRecordService.canChange(owner.id!!, record))
        assertFalse(healthRecordService.canChange(outsider.id!!, record))
        assertTrue(familyService.canAccessPet(member.id!!, pet.id!!))
        assertFalse(familyService.canAccessPet(outsider.id!!, pet.id!!))

        val chart = healthRecordService.weightChart(healthRecordService.getHealthRecordsByPet(pet.id!!))
        assertEquals(3.2, chart!!.points.first().weight)
        assertEquals(3.5, chart.points.last().weight)
    }
}
