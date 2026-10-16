package com.petcare.petcare

import com.petcare.petcare.entity.Task
import com.petcare.petcare.entity.TaskCategory
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.ScheduleExpander
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.TaskService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class TaskController(
    private val petService: PetService,
    private val taskService: TaskService,
    private val familyService: FamilyService
) {

    @GetMapping("/pets/{petId}/tasks")
    fun tasks(
        @PathVariable petId: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) filter: String?, // "all", "completed", "incomplete"
        @RequestParam(required = false) priority: String?, // "HIGH", "MEDIUM", "LOW"
        @RequestParam(required = false) sort: String?, // "date", "priority", "title"
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) on: String?,
        model: Model,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        
        // 사용자 확인
        if (!familyService.canAccessPet(user.id!!, petId)) {
            return "redirect:/?error=unauthorized"
        }
        
        var tasks: List<Task> = taskService.getTasksByPet(petId)
        val selectedDate = try {
            localDateOrThrow(on)
        } catch (_: IllegalArgumentException) {
            null
        }
        
        // 검색 필터
        if (!search.isNullOrBlank()) {
            tasks = tasks.filter {
                it.title.contains(search, ignoreCase = true) ||
                    (it.description?.contains(search, ignoreCase = true) == true)
            }
        }

        if (!category.isNullOrBlank()) {
            tasks = tasks.filter { it.category == category }
        }

        if (selectedDate != null) {
            tasks = tasks.filter { ScheduleExpander.occursOn(it, selectedDate) }
        }

        taskService.applyViewState(tasks, selectedDate)
        
        // 완료 상태 필터. 날짜가 있으면 그 날짜의 완료, 없으면 규칙의 완료 표시를 본다.
        when (filter) {
            "completed" -> tasks = tasks.filter { it.doneOnView }
            "incomplete" -> tasks = tasks.filter { !it.doneOnView }
        }
        
        // 우선순위 필터
        if (!priority.isNullOrBlank()) {
            tasks = tasks.filter { it.priority == priority }
        }
        
        // 정렬
        tasks = when (sort) {
            "date" -> {
                tasks.sortedWith(
                    Comparator<Task> { t1, t2 ->
                        when {
                            t1.dueDate == null && t2.dueDate == null -> 0
                            t1.dueDate == null -> 1
                            t2.dueDate == null -> -1
                            else -> t1.dueDate!!.compareTo(t2.dueDate!!)
                        }
                    }
                )
            }
            "priority" -> {
                val priorityOrder = mapOf("HIGH" to 1, "MEDIUM" to 2, "LOW" to 3, null to 4)
                tasks.sortedBy { priorityOrder[it.priority] }
            }
            "title" -> tasks.sortedBy { it.title }
            else -> tasks.reversed() // 기본값: 최신순
        }

        model.addAttribute("pet", pet)
        model.addAttribute("tasks", tasks)
        model.addAttribute("user", user)
        model.addAttribute("search", search ?: "")
        model.addAttribute("filter", filter ?: "all")
        model.addAttribute("priorityFilter", priority ?: "")
        model.addAttribute("sort", sort ?: "")
        model.addAttribute("category", category ?: "")
        model.addAttribute("on", on ?: "")
        model.addAttribute("categories", TaskCategory.entries)
        model.addAttribute("members", familyService.membersForPet(petId))
        return "tasks"
    }

    @GetMapping("/pets/{petId}/tasks/{taskId}/edit")
    fun editTask(
        @PathVariable petId: Long,
        @PathVariable taskId: Long,
        model: Model,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val pet = petService.getPet(petId)
        val task = taskService.getTask(taskId)

        // 사용자 확인
        if (!familyService.canAccessPet(user.id!!, petId) || task.pet?.id != petId) {
            return "redirect:/?error=unauthorized"
        }

        model.addAttribute("pet", pet)
        model.addAttribute("task", task)
        model.addAttribute("user", user)
        model.addAttribute("members", familyService.membersForPet(petId))
        return "task-edit"
    }

    @PostMapping("/pets/{petId}/tasks")
    fun addTask(
        @PathVariable petId: Long,
        @RequestParam title: String,
        @RequestParam(required = false) dueDate: String?,
        @RequestParam(required = false) dueTime: String?,
        @RequestParam(required = false) repeatType: String?,
        @RequestParam(required = false) notificationTime: String?,
        @RequestParam(required = false) priority: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) weekdays: List<String>?,
        @RequestParam(required = false) assigneeId: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val repeat = repeatType?.takeIf { it.isNotBlank() && it != "NONE" }
        val notification = notificationTime?.takeIf { it.isNotBlank() && it != "NONE" }
        val taskPriority = priority?.takeIf { it.isNotBlank() && it != "NONE" }
        val taskCategory = category?.takeIf { it.isNotBlank() && it != "NONE" }
        val days = weekdays?.takeIf { it.isNotEmpty() }?.joinToString(",")

        try {
            taskService.addTask(
                user.id!!, petId, title, localDateOrThrow(dueDate), localTimeOrThrow(dueTime), repeat, notification, taskPriority,
                taskCategory, description?.takeIf { it.isNotBlank() }, days, assigneeId?.toLongOrNull()
            )
        } catch (e: IllegalArgumentException) {
            val code = if (e.message?.startsWith("unauthorized") == true) "unauthorized" else "invalid"
            return "redirect:/pets/$petId/tasks?error=$code"
        }
        return "redirect:/pets/$petId/tasks"
    }

    @PostMapping("/pets/{petId}/tasks/{taskId}/update")
    fun updateTask(
        @PathVariable petId: Long,
        @PathVariable taskId: Long,
        @RequestParam title: String,
        @RequestParam(required = false) dueDate: String?,
        @RequestParam(required = false) dueTime: String?,
        @RequestParam(required = false) repeatType: String?,
        @RequestParam(required = false) notificationTime: String?,
        @RequestParam(required = false) priority: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) weekdays: List<String>?,
        @RequestParam(required = false) assigneeId: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val repeat = repeatType?.takeIf { it.isNotBlank() && it != "NONE" }
        val notification = notificationTime?.takeIf { it.isNotBlank() && it != "NONE" }
        val taskPriority = priority?.takeIf { it.isNotBlank() && it != "NONE" }
        val taskCategory = category?.takeIf { it.isNotBlank() && it != "NONE" }
        val days = weekdays?.takeIf { it.isNotEmpty() }?.joinToString(",")

        try {
            taskService.updateTask(
                user.id!!, taskId, title, localDateOrThrow(dueDate), localTimeOrThrow(dueTime), repeat, notification, taskPriority,
                taskCategory, description?.takeIf { it.isNotBlank() }, days, assigneeId?.toLongOrNull()
            )
        } catch (e: IllegalArgumentException) {
            val code = if (e.message?.startsWith("unauthorized") == true) "unauthorized" else "invalid"
            return "redirect:/pets/$petId/tasks?error=$code"
        }
        return "redirect:/pets/$petId/tasks"
    }

    @PostMapping("/pets/{petId}/tasks/{taskId}/toggle")
    fun toggle(
        @PathVariable petId: Long,
        @PathVariable taskId: Long,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) returnTo: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        try {
            val parsed = localDateOrThrow(date)
            taskService.toggleDone(user.id!!, taskId, parsed)
        } catch (e: IllegalArgumentException) {
            return "redirect:/?error=unauthorized"
        }
        return safeReturn(returnTo)
    }

    @PostMapping("/pets/{petId}/tasks/{taskId}/delete")
    fun delete(
        @PathVariable petId: Long,
        @PathVariable taskId: Long,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        try {
            taskService.deleteTask(user.id!!, taskId)
        } catch (e: IllegalArgumentException) {
            // 권한 오류 처리
            return "redirect:/pets/$petId/tasks?error=unauthorized"
        }
        return "redirect:/pets/$petId/tasks"
    }

    private fun safeReturn(returnTo: String?): String {
        if (returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")) {
            return "redirect:$returnTo"
        }
        return "redirect:/"
    }
}
