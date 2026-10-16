package com.petcare.petcare

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.InquiryService
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.TaskService
import com.petcare.petcare.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AdminController(
    private val userService: UserService,
    private val petService: PetService,
    private val taskService: TaskService,
    private val inquiryService: InquiryService
) {

    @GetMapping("/admin")
    fun adminPage(
        @RequestParam(required = false, defaultValue = "name_asc") sort: String,
        model: Model, 
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User
        if (admin == null) {
            return "redirect:/admin/login"
        }

        // 전체 데이터 조회 (관리자 제외)
        var allUsers = userService.getAllUsers().filter { it.effectiveRole() != Role.ADMIN }
        
        // 사용자 정렬
        allUsers = when (sort) {
            "latest" -> allUsers.sortedByDescending { it.id } // 최신순 (ID 내림차순)
            "name_asc" -> allUsers.sortedBy { it.name } // 오름차순 (가나다 순)
            "name_desc" -> allUsers.sortedByDescending { it.name } // 내림차순 (하파타 순)
            else -> allUsers.sortedBy { it.name } // 기본값: 가나다 순
        }
        
        val allPets = petService.getAllPets()
        val allTasks = taskService.getAllTasks()

        // 통계 계산
        val totalUsers = allUsers.size
        val totalPets = allPets.size
        val totalTasks = allTasks.size
        val completedTasks = taskService.getCompletedTasks().size
        val incompleteTasks = taskService.getIncompleteTasks().size
        val completionRate = if (totalTasks > 0) {
            (completedTasks.toDouble() / totalTasks.toDouble() * 100).toInt()
        } else {
            0
        }

        // 반려동물 종류별 통계
        val petTypeStats = allPets.groupBy { it.type }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // 사용자별 활동 통계
        val userActivityStats = allUsers.map { user ->
            val userPets = allPets.filter { it.user?.id == user.id }
            val userTasks = allTasks.filter { it.pet?.user?.id == user.id }
            val userCompletedTasks = userTasks.filter { it.done }.size
            mapOf(
                "user" to user,
                "petCount" to userPets.size,
                "taskCount" to userTasks.size,
                "completedTaskCount" to userCompletedTasks,
                "activityScore" to (userPets.size * 2 + userTasks.size)
            )
        }.sortedByDescending { it["activityScore"] as Int }.take(5)

        // 최근 가입한 사용자 (최신 5명)
        val recentUsers = allUsers.sortedByDescending { it.id }.take(5)

        // 우선순위별 일정 통계
        val priorityStats = allTasks.groupBy { it.priority ?: "NONE" }
            .mapValues { it.value.size }

        val taskCountByOwner = allTasks.groupingBy { it.pet?.user?.id }.eachCount()

        // 사용자별 데이터. 일정 제목은 집계에만 쓰고 화면에 넘기지 않는다.
        val userDataList = allUsers.map { user ->
            val userPets = allPets.filter { it.user?.id == user.id }
            mapOf(
                "user" to user,
                "pets" to userPets,
                "taskCount" to (taskCountByOwner[user.id] ?: 0)
            )
        }

        model.addAttribute("admin", admin)
        model.addAttribute("users", allUsers)
        model.addAttribute("pets", allPets)
        model.addAttribute("userDataList", userDataList)
        model.addAttribute("totalUsers", totalUsers)
        model.addAttribute("totalPets", totalPets)
        model.addAttribute("totalTasks", totalTasks)
        model.addAttribute("completedTasks", completedTasks)
        model.addAttribute("incompleteTasks", incompleteTasks)
        model.addAttribute("completionRate", completionRate)
        model.addAttribute("petTypeStats", petTypeStats)
        model.addAttribute("userActivityStats", userActivityStats)
        model.addAttribute("recentUsers", recentUsers)
        model.addAttribute("priorityStats", priorityStats)
        model.addAttribute("currentSort", sort)
        model.addAttribute("waitingInquiries", inquiryService.countWaiting())

        return "admin"
    }

    @PostMapping("/admin/users/delete")
    fun deleteUser(
        @RequestParam userId: Long,
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User
        if (admin == null) {
            return "redirect:/admin/login"
        }

        try {
            userService.deleteUser(userId)
        } catch (_: IllegalArgumentException) {
            return "redirect:/admin"
        }
        return "redirect:/admin"
    }

    @PostMapping("/admin/users/status")
    fun changeStatus(
        @RequestParam userId: Long,
        @RequestParam status: String,
        session: HttpSession
    ): String {
        if (session.getAttribute("admin") as? User == null) {
            return "redirect:/admin/login"
        }
        val parsed = runCatching { AccountStatus.valueOf(status) }.getOrNull()
            ?: return "redirect:/admin"
        try {
            userService.changeStatus(userId, parsed)
        } catch (_: IllegalArgumentException) {
            return "redirect:/admin"
        }
        return "redirect:/admin"
    }

    @PostMapping("/admin/pets/delete")
    fun deletePet(
        @RequestParam petId: Long,
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User
        if (admin == null) {
            return "redirect:/admin/login"
        }

        petService.deletePet(petId)
        return "redirect:/admin"
    }

    @PostMapping("/admin/tasks/delete")
    fun deleteTask(
        @RequestParam taskId: Long,
        session: HttpSession
    ): String {
        val admin = session.getAttribute("admin") as? User
        if (admin == null) {
            return "redirect:/admin/login"
        }

        taskService.deleteTask(taskId)
        return "redirect:/admin"
    }
}

