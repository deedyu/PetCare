package com.petcare.petcare.api

import com.petcare.petcare.dto.TaskDto
import com.petcare.petcare.entity.Task
import com.petcare.petcare.security.CurrentUser
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.TaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.petcare.petcare.localDateOrThrow
import com.petcare.petcare.localTimeOrThrow

@RestController
@RequestMapping("/api/tasks")
class TaskApiController(
    private val taskService: TaskService,
    private val currentUser: CurrentUser,
    private val familyService: FamilyService
) {

    @GetMapping("/pet/{petId}")
    fun getTasksByPet(@PathVariable petId: Long): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        if (!ownsPet(userId, petId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        }
        return ResponseEntity.ok(taskService.getTasksByPet(petId).map { it.toDto() })
    }

    @GetMapping("/{taskId}")
    fun getTask(@PathVariable taskId: Long): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        val task = findOwnedTask(userId, taskId) ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        return ResponseEntity.ok(task.toDto())
    }

    @GetMapping
    fun getMyTasks(): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        return ResponseEntity.ok(taskService.getTasksByUser(userId).map { it.toDto() })
    }

    @PostMapping
    fun createTask(@RequestBody request: CreateTaskRequest): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        return try {
            val dueDate = localDateOrThrow(request.dueDate)
            val dueTime = localTimeOrThrow(request.dueTime)
            taskService.addTask(
                userId = userId,
                petId = request.petId,
                title = request.title,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatType = request.repeatType,
                notificationTime = request.notificationTime,
                priority = request.priority,
                category = request.category,
                description = request.description,
                weekdays = request.weekdays,
                assigneeId = request.assigneeId
            )
            ResponseEntity.ok().build<Void>()
        } catch (e: IllegalArgumentException) {
            rejected(e)
        }
    }

    @PutMapping("/{taskId}")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<*> {
        val userId = currentUserId() ?: return unauthorized()
        return try {
            val dueDate = localDateOrThrow(request.dueDate)
            val dueTime = localTimeOrThrow(request.dueTime)
            taskService.updateTask(
                userId = userId,
                taskId = taskId,
                title = request.title,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatType = request.repeatType,
                notificationTime = request.notificationTime,
                priority = request.priority,
                category = request.category,
                description = request.description,
                weekdays = request.weekdays,
                assigneeId = request.assigneeId
            )
            ResponseEntity.ok().build<Void>()
        } catch (e: IllegalArgumentException) {
            rejected(e)
        }
    }

    @PatchMapping("/{taskId}/toggle")
    fun toggleTask(
        @PathVariable taskId: Long,
        @RequestParam(required = false) date: String?
    ): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            val parsed = localDateOrThrow(date)
            taskService.toggleDone(userId, taskId, parsed)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            val message = e.message
            val forbidden = message?.startsWith("unauthorized") == true || message?.contains("not found") == true
            if (forbidden) {
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            } else {
                ResponseEntity.badRequest().build()
            }
        }
    }

    @DeleteMapping("/{taskId}")
    fun deleteTask(@PathVariable taskId: Long): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            taskService.deleteTask(userId, taskId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    private fun rejected(e: IllegalArgumentException): ResponseEntity<*> {
        val message = e.message
        val forbidden = message?.startsWith("unauthorized") == true || message?.contains("not found") == true
        return if (forbidden) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        } else {
            ResponseEntity.badRequest().body(message)
        }
    }

    private fun currentUserId(): Long? = currentUser.optional()?.id

    private fun unauthorized() = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

    private fun ownsPet(userId: Long, petId: Long): Boolean = familyService.canAccessPet(userId, petId)

    private fun findOwnedTask(userId: Long, taskId: Long): Task? {
        return try {
            val task = taskService.getTask(taskId)
            if (task.pet?.id != null && familyService.canAccessPet(userId, task.pet!!.id!!)) task else null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun Task.toDto() = TaskDto(
        id = id,
        title = title,
        done = done,
        dueDate = dueDate,
        dueTime = dueTime,
        repeatType = repeatType,
        notificationTime = notificationTime,
        priority = priority,
        petId = pet?.id,
        category = category,
        description = description,
        weekdays = weekdays,
        assigneeId = assignee?.id,
        assigneeName = assignee?.name
    )

    data class CreateTaskRequest(
        val petId: Long,
        val title: String,
        val dueDate: String? = null,
        val dueTime: String? = null,
        val repeatType: String? = null,
        val notificationTime: String? = null,
        val priority: String? = null,
        val category: String? = null,
        val description: String? = null,
        val weekdays: String? = null,
        val assigneeId: Long? = null
    )
    
    data class UpdateTaskRequest(
        val title: String,
        val dueDate: String? = null,
        val dueTime: String? = null,
        val repeatType: String? = null,
        val notificationTime: String? = null,
        val priority: String? = null,
        val category: String? = null,
        val description: String? = null,
        val weekdays: String? = null,
        val assigneeId: Long? = null
    )
}
