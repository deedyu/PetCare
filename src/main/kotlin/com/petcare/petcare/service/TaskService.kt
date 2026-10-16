package com.petcare.petcare.service

import com.petcare.petcare.entity.Task
import com.petcare.petcare.entity.TaskOccurrence
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
class TaskService(
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val petService: PetService,
    private val userRepository: UserRepository,
    private val familyService: FamilyService
) {

    fun getTasksByPet(petId: Long): List<Task> {
        return taskRepository.findByPetIdOrderByIdDesc(petId)
    }

    fun getTasksByUser(userId: Long): List<Task> {
        return taskRepository.findVisibleToUser(userId)
    }

    fun getTask(taskId: Long): Task {
        return taskRepository.findById(taskId)
            .orElseThrow { IllegalArgumentException("task not found: $taskId") }
    }

    @Transactional
    fun addTask(
        userId: Long,
        petId: Long,
        title: String,
        dueDate: LocalDate?,
        dueTime: LocalTime?,
        repeatType: String?,
        notificationTime: String?,
        priority: String?,
        category: String? = null,
        description: String? = null,
        weekdays: String? = null,
        assigneeId: Long? = null
    ) {
        val pet = petService.getPet(petId)
        if (!familyService.canAccessPet(userId, petId)) {
            throw IllegalArgumentException("unauthorized: pet does not belong to user")
        }
        if (title.isBlank()) {
            throw IllegalArgumentException("제목을 입력하세요.")
        }

        val task = Task(
            title = title.trim(),
            done = false,
            dueDate = dueDate,
            dueTime = dueTime,
            repeatType = repeatType,
            notificationTime = notificationTime,
            priority = priority,
            category = category,
            description = description,
            weekdays = weekdays,
            pet = pet
        )
        applyAssignee(userId, task, assigneeId)
        taskRepository.save(task)
        recordAssignment(userId, task)
    }

    @Transactional
    fun updateTask(
        userId: Long,
        taskId: Long,
        title: String,
        dueDate: LocalDate?,
        dueTime: LocalTime?,
        repeatType: String?,
        notificationTime: String?,
        priority: String?,
        category: String? = null,
        description: String? = null,
        weekdays: String? = null,
        assigneeId: Long? = null
    ) {
        val task = accessibleTask(userId, taskId)
        val previousAssignee = task.assignee?.id
        if (title.isBlank()) {
            throw IllegalArgumentException("제목을 입력하세요.")
        }

        task.title = title.trim()
        task.dueDate = dueDate
        task.dueTime = dueTime
        task.repeatType = repeatType
        task.notificationTime = notificationTime
        task.priority = priority
        task.category = category
        task.description = description
        task.weekdays = weekdays
        applyAssignee(userId, task, assigneeId)
        taskRepository.save(task)
        if (task.assignee?.id != previousAssignee) {
            recordAssignment(userId, task)
        }
    }

    @Transactional
    fun toggleDone(userId: Long, taskId: Long, date: LocalDate? = null, memo: String? = null) {
        val task = accessibleTask(userId, taskId)
        val target = date ?: task.dueDate ?: LocalDate.now()
        val current = isDoneOn(task, target)
        setDone(userId, taskId, target, !current, memo)
    }

    /**
     * 같은 날짜를 다시 완료해도 수행 기록은 한 행만 유지한다.
     */
    @Transactional
    fun setDone(userId: Long, taskId: Long, date: LocalDate, done: Boolean, memo: String? = null) {
        val task = accessibleTask(userId, taskId)
        if (!ScheduleExpander.occursOn(task, date)) {
            throw IllegalArgumentException("이 날짜에는 일정이 없습니다.")
        }
        val user = userRepository.findById(userId).orElse(null)
        val occurrence = loadOrCreate(task, date)
        occurrence.done = done
        if (done) {
            occurrence.completedBy = user
            occurrence.completedAt = LocalDateTime.now()
        } else {
            occurrence.completedBy = null
            occurrence.completedAt = null
        }
        if (memo != null) {
            occurrence.memo = memo.trim().ifBlank { null }
        }
        occurrenceRepository.save(occurrence)
        if (task.repeatType == null) {
            task.done = done
            taskRepository.save(task)
        }
        val family = task.pet?.family
        if (done && family != null && user != null) {
            familyService.addActivity(
                family,
                user,
                "COMPLETE",
                "${user.name}님이 ${date} ${task.title}을 완료했습니다."
            )
        }
    }

    @Transactional
    fun deleteTask(userId: Long, taskId: Long) {
        val task = taskRepository.findById(taskId)
            .orElseThrow { IllegalArgumentException("task not found: $taskId") }
        
        if (!familyService.canAccessPet(userId, task.pet?.id ?: throw IllegalArgumentException("task not found"))) {
            throw IllegalArgumentException("unauthorized: task does not belong to user")
        }
        
        occurrenceRepository.deleteByTaskId(taskId)
        taskRepository.deleteById(taskId)
    }

    fun isDoneOn(task: Task, date: LocalDate): Boolean {
        val taskId = task.id ?: return false
        val occurrence = occurrenceRepository.findByTaskIdAndOccurrenceDate(taskId, date)
        if (occurrence != null) return occurrence.done
        return task.repeatType == null && task.done && task.dueDate == date
    }

    fun applyViewState(tasks: List<Task>, selectedDate: LocalDate?) {
        tasks.forEach { task ->
            val date = selectedDate ?: task.dueDate
            task.viewDate = date
            task.doneOnView = if (date == null) task.done else isDoneOn(task, date)
        }
    }

    @Transactional(readOnly = true)
    fun slotsOn(userId: Long, date: LocalDate, petId: Long? = null, category: String? = null): List<TaskSlot> {
        return slotsBetween(userId, date, date, petId, category)[date].orEmpty()
    }

    @Transactional(readOnly = true)
    fun slotsBetween(
        userId: Long,
        from: LocalDate,
        to: LocalDate,
        petId: Long? = null,
        category: String? = null
    ): Map<LocalDate, List<TaskSlot>> {
        val tasks = getTasksByUser(userId).filter { task ->
            (petId == null || task.pet?.id == petId) &&
                (category.isNullOrBlank() || task.category == category)
        }
        val ids = tasks.mapNotNull { it.id }
        val occurrences = if (ids.isEmpty()) {
            emptyList()
        } else {
            occurrenceRepository.findByTaskIdInAndOccurrenceDateBetween(ids, from, to)
        }
        val byKey = occurrences.associateBy { (it.task?.id to it.occurrenceDate) }
        val result = linkedMapOf<LocalDate, MutableList<TaskSlot>>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            val day = tasks.filter { ScheduleExpander.occursOn(it, cursor) }.map { task ->
                val occurrence = byKey[task.id to cursor]
                val done = occurrence?.done ?: (task.repeatType == null && task.done && task.dueDate == cursor)
                TaskSlot.of(
                    task = task,
                    date = cursor,
                    done = done,
                    memo = occurrence?.memo,
                    completedByName = occurrence?.completedBy?.name,
                    completedAt = occurrence?.completedAt
                )
            }
            result[cursor] = day.toMutableList()
            cursor = cursor.plusDays(1)
        }
        return result
    }

    fun countCompleted(userId: Long): Int {
        val legacy = getTasksByUser(userId).count { it.done && it.dueDate == null && it.repeatType == null }
        return occurrenceRepository.countDoneByUser(userId).toInt() + legacy
    }

    private fun accessibleTask(userId: Long, taskId: Long): Task {
        val task = taskRepository.findById(taskId)
            .orElseThrow { IllegalArgumentException("task not found: $taskId") }
        val petId = task.pet?.id ?: throw IllegalArgumentException("task not found: $taskId")
        if (!familyService.canAccessPet(userId, petId)) {
            throw IllegalArgumentException("unauthorized: task does not belong to user")
        }
        return task
    }

    private fun applyAssignee(actorId: Long, task: Task, assigneeId: Long?) {
        if (assigneeId == null) {
            task.assignee = null
            return
        }
        val pet = task.pet ?: throw IllegalArgumentException("반려동물이 없는 일정입니다.")
        val familyId = pet.family?.id
            ?: throw IllegalArgumentException("가족에 공유된 반려동물만 담당자를 지정할 수 있습니다.")
        if (!familyService.isMember(familyId, assigneeId) || !familyService.canAccessPet(actorId, pet.id!!)) {
            throw IllegalArgumentException("가족 구성원만 담당자로 지정할 수 있습니다.")
        }
        task.assignee = userRepository.findById(assigneeId)
            .orElseThrow { IllegalArgumentException("담당자를 찾을 수 없습니다.") }
    }

    private fun recordAssignment(actorId: Long, task: Task) {
        val assignee = task.assignee ?: return
        val family = task.pet?.family ?: return
        val actor = userRepository.findById(actorId).orElse(null)
        familyService.addActivity(family, actor, "ASSIGN", "${task.title} 담당자를 ${assignee.name}님으로 지정했습니다.")
    }

    private fun ownedTask(userId: Long, taskId: Long): Task = accessibleTask(userId, taskId)

    private fun loadOrCreate(task: Task, date: LocalDate): TaskOccurrence {
        val taskId = task.id ?: throw IllegalArgumentException("task not found")
        val existing = occurrenceRepository.findByTaskIdAndOccurrenceDate(taskId, date)
        if (existing != null) return existing
        return try {
            occurrenceRepository.save(
                TaskOccurrence(
                    task = task,
                    occurrenceDate = date,
                    done = false
                )
            )
        } catch (_: DataIntegrityViolationException) {
            occurrenceRepository.findByTaskIdAndOccurrenceDate(taskId, date)
                ?: throw IllegalStateException("수행 기록을 만들지 못했습니다.")
        }
    }

    // 관리자용 메서드들
    @Transactional
    fun addTask(petId: Long, title: String, dueDate: LocalDate?, dueTime: LocalTime?, repeatType: String?, notificationTime: String?, priority: String?) {
        val pet = petService.getPet(petId)
        val task = Task(
            title = title,
            done = false,
            dueDate = dueDate,
            dueTime = dueTime,
            repeatType = repeatType,
            notificationTime = notificationTime,
            priority = priority,
            pet = pet
        )
        taskRepository.save(task)
    }

    @Transactional
    fun updateTask(taskId: Long, title: String, dueDate: LocalDate?, dueTime: LocalTime?, repeatType: String?, notificationTime: String?, priority: String?) {
        val task = taskRepository.findById(taskId)
            .orElseThrow { IllegalArgumentException("task not found: $taskId") }
        
        task.title = title
        task.dueDate = dueDate
        task.dueTime = dueTime
        task.repeatType = repeatType
        task.notificationTime = notificationTime
        task.priority = priority
        
        taskRepository.save(task)
    }

    @Transactional
    fun toggleDone(taskId: Long) {
        val task = taskRepository.findById(taskId)
            .orElseThrow { IllegalArgumentException("task not found: $taskId") }

        task.done = !task.done
        taskRepository.save(task)
    }

    @Transactional
    fun deleteTask(taskId: Long) {
        occurrenceRepository.deleteByTaskId(taskId)
        taskRepository.deleteById(taskId)
    }

    fun getAllTasks(): List<Task> {
        return taskRepository.findAllWithPet()
    }

    fun getCompletedTasks(): List<Task> {
        return taskRepository.findAllWithPet().filter { it.done }
    }

    fun getCompletedTasksByUser(userId: Long): List<Task> {
        return taskRepository.findByUserIdOrderByIdDesc(userId).filter { it.done }
    }

    fun getIncompleteTasks(): List<Task> {
        return taskRepository.findAllWithPet().filter { !it.done }
    }

    fun getTodayTasks(): List<Task> {
        val today = LocalDate.now()
        return taskRepository.findAllWithPet().filter { it.dueDate == today }
    }

    fun getTodayTasksByUser(userId: Long): List<Task> {
        val today = LocalDate.now()
        return taskRepository.findByUserIdOrderByIdDesc(userId).filter { it.dueDate == today }
    }
}
