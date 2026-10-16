package com.petcare.petcare.service

import com.petcare.petcare.entity.Task
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 반복 규칙이 어떤 날짜에 해당하는지 계산한다.
 * 완료 여부는 여기서 다루지 않는다. 날짜별 기록은 TaskOccurrence가 가진다.
 */
object ScheduleExpander {
    fun occursOn(task: Task, date: LocalDate): Boolean {
        val start = task.dueDate ?: return false
        if (date.isBefore(start)) return false
        return when (task.repeatType) {
            null, "", "NONE" -> date == start
            "DAILY" -> true
            "WEEKLY" -> date.dayOfWeek == start.dayOfWeek
            "MONTHLY" -> date.dayOfMonth == dayInMonth(start.dayOfMonth, date)
            "WEEKDAYS" -> date.dayOfWeek in selectedDays(task, start)
            else -> date == start
        }
    }

    fun selectedDays(task: Task, start: LocalDate): Set<DayOfWeek> {
        val parsed = task.weekdays
            ?.split(",")
            ?.mapNotNull { name -> parseDay(name) }
            ?.toSet()
            .orEmpty()
        return parsed.ifEmpty { setOf(start.dayOfWeek) }
    }

    private fun parseDay(name: String): DayOfWeek? {
        return when (name.trim().uppercase()) {
            "MON", "MONDAY" -> DayOfWeek.MONDAY
            "TUE", "TUESDAY" -> DayOfWeek.TUESDAY
            "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY
            "THU", "THURSDAY" -> DayOfWeek.THURSDAY
            "FRI", "FRIDAY" -> DayOfWeek.FRIDAY
            "SAT", "SATURDAY" -> DayOfWeek.SATURDAY
            "SUN", "SUNDAY" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun dayInMonth(wanted: Int, date: LocalDate): Int {
        return minOf(wanted, date.lengthOfMonth())
    }
}
