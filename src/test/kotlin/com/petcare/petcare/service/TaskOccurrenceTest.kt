package com.petcare.petcare.service

import com.petcare.petcare.entity.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class TaskOccurrenceTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var taskService: TaskService

    @Test
    fun completingTodayDoesNotCompleteTomorrow() {
        val user = userService.createUser("occ-user", "abcd1234", "테스터", role = Role.USER)
        val pet = petService.createPet(user, "콩", "강아지", 2)
        val today = LocalDate.of(2026, 10, 15)
        taskService.addTask(
            userId = user.id!!,
            petId = pet.id!!,
            title = "매일 산책",
            dueDate = today,
            dueTime = null,
            repeatType = "DAILY",
            notificationTime = null,
            priority = null,
            category = "WALK"
        )
        val task = taskService.getTasksByPet(pet.id!!).first { it.title == "매일 산책" }

        taskService.setDone(user.id!!, task.id!!, today, true, "공원")
        taskService.setDone(user.id!!, task.id!!, today, true, "공원")

        assertTrue(taskService.isDoneOn(task, today))
        assertFalse(taskService.isDoneOn(task, today.plusDays(1)))
        assertTrue(taskService.slotsOn(user.id!!, today).any { it.id == task.id && it.done && it.memo == "공원" })
        assertTrue(taskService.slotsOn(user.id!!, today.plusDays(1)).any { it.id == task.id && !it.done })
    }
}
