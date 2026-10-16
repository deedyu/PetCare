package com.petcare.petcare.service

import com.petcare.petcare.entity.AppNotification
import com.petcare.petcare.entity.NotificationType
import com.petcare.petcare.entity.Task
import com.petcare.petcare.repository.AppNotificationRepository
import com.petcare.petcare.repository.TaskOccurrenceRepository
import com.petcare.petcare.repository.TaskRepository
import com.petcare.petcare.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NotificationService(
    private val notificationRepository: AppNotificationRepository,
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val userRepository: UserRepository
) {
    fun mine(userId: Long): List<AppNotification> = notificationRepository.findByUserId(userId).take(50)

    fun unreadCount(userId: Long): Long = notificationRepository.countByUser_IdAndReadAtIsNull(userId)

    /**
     * 같은 키면 새로 만들지 않는다. 스케줄이 같은 분을 두 번 돌아도 알림은 하나다.
     */
    @Transactional
    fun createIfAbsent(
        userId: Long,
        type: NotificationType,
        title: String,
        body: String,
        link: String?,
        dedupeKey: String
    ): Boolean {
        if (notificationRepository.existsByUser_IdAndDedupeKey(userId, dedupeKey)) {
            return false
        }
        return try {
            notificationRepository.save(
                AppNotification(
                    user = userRepository.findById(userId).orElse(null) ?: return false,
                    type = type,
                    title = title.take(120),
                    body = body.take(200),
                    link = link?.take(200),
                    dedupeKey = dedupeKey.take(80)
                )
            )
            true
        } catch (_: DataIntegrityViolationException) {
            false
        }
    }

    @Transactional
    fun deliverDueReminders(now: LocalDateTime) {
        deliver(taskRepository.findReminders(), now)
    }

    @Transactional
    fun refreshFor(userId: Long, now: LocalDateTime) {
        val tasks = taskRepository.findVisibleToUser(userId).filter { !it.notificationTime.isNullOrBlank() }
        deliver(tasks, now)
    }

    @Transactional
    fun open(userId: Long, notificationId: Long): String? {
        val notice = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("알림을 찾을 수 없습니다.") }
        if (notice.user?.id != userId) {
            throw IllegalArgumentException("알림을 찾을 수 없습니다.")
        }
        if (notice.readAt == null) {
            notice.readAt = LocalDateTime.now()
            notificationRepository.save(notice)
        }
        return notice.link
    }

    @Transactional
    fun markAllRead(userId: Long) {
        notificationRepository.markAllRead(userId, LocalDateTime.now())
    }

    @Transactional
    fun releaseUser(userId: Long) {
        notificationRepository.findByUserId(userId).forEach { notificationRepository.delete(it) }
    }

    private fun deliver(tasks: List<Task>, now: LocalDateTime) {
        if (tasks.isEmpty()) return
        val today = now.toLocalDate()
        val ids = tasks.mapNotNull { it.id }
        val doneToday = occurrenceRepository.findByTaskIdInAndOccurrenceDate(ids, today)
            .filter { it.done }
            .mapNotNull { it.task?.id }
            .toSet()
        for (task in tasks) {
            val taskId = task.id ?: continue
            if (taskId in doneToday) continue
            if ((task.repeatType.isNullOrBlank() || task.repeatType == "NONE") && task.done) continue
            if (!ScheduleExpander.occursOn(task, today)) continue
            val due = task.dueTime ?: continue
            val remindAt = remindAt(today, due, task.notificationTime) ?: continue
            if (remindAt.isAfter(now)) continue
            val recipientId = (task.assignee ?: task.pet?.user)?.id ?: continue
            val petName = task.pet?.name ?: "반려동물"
            createIfAbsent(
                userId = recipientId,
                type = NotificationType.TASK_REMINDER,
                title = task.title,
                body = "$petName · ${label(task.notificationTime)}",
                link = "/calendar?date=$today",
                dedupeKey = "task:$taskId:$today:${task.notificationTime}"
            )
        }
    }

    private fun remindAt(day: LocalDate, due: LocalTime, notificationTime: String?): LocalDateTime? {
        val minutes = when (notificationTime) {
            "ON_TIME" -> 0L
            "10_MIN_BEFORE" -> 10L
            "30_MIN_BEFORE" -> 30L
            "1_HOUR_BEFORE" -> 60L
            else -> return null
        }
        return LocalDateTime.of(day, due).minusMinutes(minutes)
    }

    private fun label(notificationTime: String?): String {
        return when (notificationTime) {
            "ON_TIME" -> "정시"
            "10_MIN_BEFORE" -> "10분 전"
            "30_MIN_BEFORE" -> "30분 전"
            "1_HOUR_BEFORE" -> "1시간 전"
            else -> "알림"
        }
    }
}
