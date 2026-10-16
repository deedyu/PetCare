package com.petcare.petcare

import com.petcare.petcare.entity.TaskCategory
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.TaskService
import com.petcare.petcare.service.TaskSlot
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Controller
class CalendarController(
    private val taskService: TaskService,
    private val petService: PetService
) {
    @GetMapping("/calendar")
    fun calendar(
        @RequestParam(required = false, defaultValue = "month") view: String,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) petId: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) open: Long?,
        model: Model,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        val userId = user.id ?: return "redirect:/login"
        val selected = try {
            date?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: LocalDate.now()
        } catch (_: DateTimeParseException) {
            LocalDate.now()
        }
        val safeView = if (view in setOf("month", "week", "day")) view else "month"
        val selectedPetId = petId?.toLongOrNull()
        val (from, to) = rangeOf(safeView, selected)
        val byDate = taskService.slotsBetween(userId, from, to, selectedPetId, category?.takeIf { it.isNotBlank() })
        val days = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { day ->
                val slots = byDate[day].orEmpty().sortedBy { it.dueTime }
                CalendarDay(
                    date = day,
                    inMonth = day.month == selected.month,
                    slots = slots.take(3),
                    more = (slots.size - 3).coerceAtLeast(0)
                )
            }
            .toList()

        val opened = open?.let { taskId ->
            byDate[selected].orEmpty().find { it.id == taskId }
        }

        model.addAttribute("user", user)
        model.addAttribute("pets", petService.getVisiblePets(userId))
        model.addAttribute("categories", TaskCategory.entries)
        model.addAttribute("view", safeView)
        model.addAttribute("selected", selected)
        model.addAttribute("petId", selectedPetId)
        model.addAttribute("category", category ?: "")
        model.addAttribute("days", days)
        model.addAttribute("daySlots", byDate[selected].orEmpty().sortedBy { it.dueTime })
        model.addAttribute("opened", opened)
        model.addAttribute("prev", shift(safeView, selected, -1))
        model.addAttribute("next", shift(safeView, selected, 1))
        model.addAttribute("title", titleOf(safeView, selected))
        return "calendar"
    }

    @PostMapping("/calendar/complete")
    fun complete(
        @RequestParam taskId: Long,
        @RequestParam date: String,
        @RequestParam done: Boolean,
        @RequestParam(required = false) memo: String?,
        @RequestParam(required = false) returnTo: String?,
        session: HttpSession
    ): String {
        val user = session.getAttribute("user") as? User ?: return "redirect:/login"
        val parsed = try {
            LocalDate.parse(date)
        } catch (_: DateTimeParseException) {
            return "redirect:/calendar"
        }
        try {
            taskService.setDone(user.id!!, taskId, parsed, done, memo)
        } catch (_: IllegalArgumentException) {
            return "redirect:/calendar?error=unauthorized"
        }
        return safeReturn(returnTo)
    }

    private fun rangeOf(view: String, date: LocalDate): Pair<LocalDate, LocalDate> {
        return when (view) {
            "day" -> date to date
            "week" -> {
                val start = date.minusDays((date.dayOfWeek.value - 1).toLong())
                start to start.plusDays(6)
            }
            else -> {
                val month = YearMonth.from(date)
                val first = month.atDay(1)
                val last = month.atEndOfMonth()
                val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
                val end = last.plusDays(((7 - last.dayOfWeek.value) % 7).toLong())
                start to end
            }
        }
    }

    private fun shift(view: String, date: LocalDate, direction: Long): LocalDate {
        return when (view) {
            "day" -> date.plusDays(direction)
            "week" -> date.plusWeeks(direction)
            else -> date.plusMonths(direction)
        }
    }

    private fun titleOf(view: String, date: LocalDate): String {
        return when (view) {
            "day" -> "${date.monthValue}월 ${date.dayOfMonth}일"
            "week" -> {
                val start = date.minusDays((date.dayOfWeek.value - 1).toLong())
                "${start.monthValue}/${start.dayOfMonth} – ${start.plusDays(6).monthValue}/${start.plusDays(6).dayOfMonth}"
            }
            else -> "${date.year}년 ${date.monthValue}월"
        }
    }

    private fun safeReturn(returnTo: String?): String {
        if (returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")) {
            return "redirect:$returnTo"
        }
        return "redirect:/calendar"
    }
}

class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val slots: List<TaskSlot>,
    val more: Int
)
