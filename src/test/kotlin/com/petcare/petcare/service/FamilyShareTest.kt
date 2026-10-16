package com.petcare.petcare.service

import com.petcare.petcare.entity.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class FamilyShareTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var taskService: TaskService
    @Autowired lateinit var familyService: FamilyService

    @Test
    @Transactional
    fun memberSeesSharedTaskAndCompletionStaysOnThatDate() {
        val owner = userService.createUser("fam-owner", "abcd1234", "집사", role = Role.USER)
        val member = userService.createUser("fam-member", "abcd1234", "동생", role = Role.USER)
        val outsider = userService.createUser("fam-outsider", "abcd1234", "손님", role = Role.USER)
        val pet = petService.createPet(owner, "콩", "강아지", 2)
        val today = LocalDate.of(2026, 10, 16)

        val family = familyService.create(owner.id!!, "우리집", "함께 돌보기")
        val invitation = familyService.invite(owner.id!!, family.id!!)
        assertFailsWith<IllegalArgumentException> { familyService.join(outsider.id!!, "없는코드") }
        val joined = familyService.join(member.id!!, invitation.code)

        assertEquals(family.id, joined.id)
        assertFalse(familyService.canAccessPet(member.id!!, pet.id!!))

        familyService.sharePet(owner.id!!, family.id!!, pet.id!!)
        assertTrue(familyService.canAccessPet(member.id!!, pet.id!!))
        assertFalse(familyService.canAccessPet(outsider.id!!, pet.id!!))

        taskService.addTask(
            userId = owner.id!!,
            petId = pet.id!!,
            title = "가족 산책",
            dueDate = today,
            dueTime = null,
            repeatType = "DAILY",
            notificationTime = null,
            priority = null,
            category = "WALK",
            assigneeId = member.id
        )

        val task = taskService.getTasksByPet(pet.id!!).first { it.title == "가족 산책" }
        assertEquals(member.id, task.assignee?.id)
        assertTrue(taskService.slotsOn(member.id!!, today).any { it.id == task.id && it.assigneeName == "동생" })
        assertTrue(taskService.slotsOn(outsider.id!!, today).none { it.id == task.id })

        taskService.setDone(member.id!!, task.id!!, today, true, "같이 산책")
        assertTrue(taskService.isDoneOn(task, today))
        assertFalse(taskService.isDoneOn(task, today.plusDays(1)))
        assertTrue(familyService.recentActivity(family.id!!).any { it.action == "COMPLETE" })

        assertFailsWith<IllegalArgumentException> { familyService.leave(owner.id!!, family.id!!) }
        familyService.removeMember(owner.id!!, family.id!!, member.id!!)
        assertFalse(familyService.canAccessPet(member.id!!, pet.id!!))
    }
}
