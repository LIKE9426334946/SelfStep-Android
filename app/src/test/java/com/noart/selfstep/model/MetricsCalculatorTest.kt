package com.noart.selfstep.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MetricsCalculatorTest {
    private val today = LocalDate.of(2026, 8, 13)

    @Test
    fun calculatesCurrentAndBestStreaksAcrossCalendarDays() {
        val records = listOf(
            successfulRecord("2026-08-09"),
            successfulRecord("2026-08-11"),
            successfulRecord("2026-08-12"),
            successfulRecord("2026-08-13")
        ).associateBy { it.date }

        val metrics = MetricsCalculator.calculate(records, today)

        assertEquals(3, metrics.currentStreak)
        assertEquals(3, metrics.bestStreak)
        assertEquals(4, metrics.successfulDays)
    }

    @Test
    fun keepsYesterdayStreakUntilTodayIsCompleted() {
        val records = listOf(
            successfulRecord("2026-08-11"),
            successfulRecord("2026-08-12"),
            incompleteRecord("2026-08-13")
        ).associateBy { it.date }

        val metrics = MetricsCalculator.calculate(records, today)

        assertEquals(2, metrics.currentStreak)
        assertEquals(2, metrics.bestStreak)
    }

    private fun successfulRecord(date: String) = DailyRecord(
        date = date,
        tasks = listOf(
            DailyTaskStatus("task", "学习", TaskType.MUST_DO, completed = true)
        )
    )

    private fun incompleteRecord(date: String) = DailyRecord(
        date = date,
        tasks = listOf(
            DailyTaskStatus("task", "学习", TaskType.MUST_DO, completed = false)
        )
    )
}
