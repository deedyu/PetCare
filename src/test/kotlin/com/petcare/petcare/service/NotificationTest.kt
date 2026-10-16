package com.petcare.petcare.service

import com.petcare.petcare.entity.NotificationType
import com.petcare.petcare.entity.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class NotificationTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var taskService: TaskService
    @Autowired lateinit var inquiryService: InquiryService
    @Autowired lateinit var notificationService: NotificationService

    @Test
    @Transactional
    fun reminderAndReplyAreStoredOnce() {
        val owner = userService.createUser("note-owner", "abcd1234", "집사", role = Role.USER)
        val member = userService.createUser("note-member", "abcd1234", "동생", role = Role.USER)
        val pet = petService.createPet(owner, "콩", "강아지", 2)
        val today = LocalDate.of(2026, 10, 16)
        val now = LocalDateTime.of(today, LocalTime.of(12, 0))
        taskService.addTask(
            userId = owner.id!!,
            petId = pet.id!!,
            title = "산책",
            dueDate = today,
            dueTime = LocalTime.of(11, 50),
            repeatType = "NONE",
            notificationTime = "ON_TIME",
            priority = null
        )
        taskService.addTask(
            userId = owner.id!!,
            petId = pet.id!!,
            title = "약",
            dueDate = today,
            dueTime = LocalTime.of(18, 0),
            repeatType = "NONE",
            notificationTime = "30_MIN_BEFORE",
            priority = null
        )

        notificationService.deliverDueReminders(now)
        notificationService.deliverDueReminders(now)
        val reminders = notificationService.mine(owner.id!!)
        assertEquals(1, reminders.size)
        assertEquals("산책", reminders.first().title)
        assertEquals(NotificationType.TASK_REMINDER, reminders.first().type)

        val inquiry = inquiryService.create(member.id!!, "밥 시간", "알려 주세요")
        inquiryService.reply(owner.id!!, inquiry.id!!, "저녁에 주세요")
        inquiryService.reply(owner.id!!, inquiry.id!!, "저녁에 주세요")
        val replies = notificationService.mine(member.id!!).filter { it.type == NotificationType.INQUIRY_REPLY }
        assertEquals(2, replies.size)
        assertTrue(replies.all { it.link == "/inquiries/${inquiry.id}" })

        notificationService.markAllRead(owner.id!!)
        assertEquals(0, notificationService.unreadCount(owner.id!!))
        userService.deleteUser(member.id!!)
        assertEquals(0, notificationService.mine(member.id!!).size)
    }
}
