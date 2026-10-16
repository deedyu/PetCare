package com.petcare.petcare.service

import com.petcare.petcare.entity.Task
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleExpanderTest {
    private val start = LocalDate.of(2026, 10, 15)

    @Test
    fun oneOffOccursOnlyOnDueDate() {
        val task = task(repeat = null)
        assertTrue(ScheduleExpander.occursOn(task, start))
        assertFalse(ScheduleExpander.occursOn(task, start.plusDays(1)))
    }

    @Test
    fun dailyOccursAfterStartAndNotBefore() {
        val task = task(repeat = "DAILY")
        assertFalse(ScheduleExpander.occursOn(task, start.minusDays(1)))
        assertTrue(ScheduleExpander.occursOn(task, start))
        assertTrue(ScheduleExpander.occursOn(task, start.plusDays(10)))
    }

    @Test
    fun weeklyStaysOnTheSameWeekday() {
        val task = task(repeat = "WEEKLY")
        assertTrue(ScheduleExpander.occursOn(task, start.plusWeeks(1)))
        assertFalse(ScheduleExpander.occursOn(task, start.plusDays(1)))
    }

    @Test
    fun monthlyUsesTheSameDayAndClampsShortMonths() {
        val task = task(repeat = "MONTHLY", due = LocalDate.of(2026, 1, 31))
        assertTrue(ScheduleExpander.occursOn(task, LocalDate.of(2026, 3, 31)))
        assertTrue(ScheduleExpander.occursOn(task, LocalDate.of(2026, 2, 28)))
        assertFalse(ScheduleExpander.occursOn(task, LocalDate.of(2026, 3, 30)))
    }

    @Test
    fun weekdaysUseTheSelectedDays() {
        val task = task(repeat = "WEEKDAYS").apply { weekdays = "MON,WED" }
        assertTrue(ScheduleExpander.occursOn(task, LocalDate.of(2026, 10, 19)))
        assertFalse(ScheduleExpander.occursOn(task, LocalDate.of(2026, 10, 20)))
    }

    private fun task(repeat: String?, due: LocalDate = start) = Task(
        title = "산책",
        dueDate = due,
        repeatType = repeat
    )
}
