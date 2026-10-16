package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.AnnouncementService
import com.petcare.petcare.service.NotificationService
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.TaskService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val petService: PetService,
    private val taskService: TaskService,
    private val announcementService: AnnouncementService,
    private val notificationService: NotificationService
) {

    @GetMapping("/")
    fun home(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        val userId = user.id!!
        
        // 사용자별 반려동물 조회 (최신 3개)
        val visiblePets = petService.getVisiblePets(userId)
        val pets = visiblePets.take(3)
        
        // 사용자별 일정 조회
        val allTasks = taskService.getTasksByUser(userId)
        val today = java.time.LocalDate.now()
        val todaySlots = taskService.slotsOn(userId, today)
        
        // 오늘 날짜에 해당하는 일정만, 그 날짜의 완료 기록으로 나눈다.
        val incompleteTodayTasks = todaySlots.filter { !it.done }
        val completedTodayTasks = todaySlots.filter { it.done }
        
        // 우선순위 높은 일정 목록 (미완료만, 우선순위 순으로 정렬)
        val priorityOrder = mapOf("HIGH" to 1, "MEDIUM" to 2, "LOW" to 3, null to 4)
        val highPriorityTasks = allTasks
            .filter { !it.done && it.priority != null }
            .sortedBy { priorityOrder[it.priority] }
            .take(5) // 최대 5개만 표시
        
        // 대시보드 데이터 계산
        val petCount = visiblePets.size
        val totalTaskCount = allTasks.size
        val todayTaskCount = todaySlots.size
        val completedTaskCount = taskService.countCompleted(userId)
        
        // 예정된 일정: 시작일이 오늘 이후이거나, 반복 중인 일정
        val scheduledTasks = allTasks.filter {
            val start = it.dueDate
            (it.repeatType != null && start != null) || (start != null && start.isAfter(today))
        }
        val scheduledTaskCount = scheduledTasks.size
        
        // 깃발 표시 (우선순위가 있는 일정)
        val flaggedTasks = allTasks.filter { it.priority != null }
        val flaggedTaskCount = flaggedTasks.size
        
        val completionRate = if (totalTaskCount > 0) {
            (completedTaskCount.toDouble() / totalTaskCount.toDouble() * 100).toInt()
        } else {
            0
        }
        
        model.addAttribute("pets", pets)
        model.addAttribute("incompleteTodayTasks", incompleteTodayTasks)
        model.addAttribute("completedTodayTasks", completedTodayTasks)
        model.addAttribute("highPriorityTasks", highPriorityTasks)
        model.addAttribute("user", user)
        model.addAttribute("petCount", petCount)
        model.addAttribute("totalTaskCount", totalTaskCount)
        model.addAttribute("todayTaskCount", todayTaskCount)
        model.addAttribute("completedTaskCount", completedTaskCount)
        model.addAttribute("scheduledTaskCount", scheduledTaskCount)
        model.addAttribute("flaggedTaskCount", flaggedTaskCount)
        model.addAttribute("completionRate", completionRate)
        model.addAttribute("announcements", announcementService.list().take(5))
        notificationService.refreshFor(userId, java.time.LocalDateTime.now())
        model.addAttribute("unreadNotifications", notificationService.unreadCount(userId))
        
        return "index"
    }
}
