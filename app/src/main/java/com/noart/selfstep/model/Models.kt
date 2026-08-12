package com.noart.selfstep.model

import java.time.LocalDate

enum class TaskType {
    MUST_DO,
    AVOID
}

data class DisciplineTask(
    val id: String,
    val title: String,
    val type: TaskType,
    val createdAt: Long
)

data class DailyTaskStatus(
    val taskId: String,
    val title: String,
    val type: TaskType,
    val completed: Boolean
)

data class DailyRecord(
    val date: String,
    val tasks: List<DailyTaskStatus>
) {
    val completedCount: Int
        get() = tasks.count { it.completed }

    val isSuccessful: Boolean
        get() = tasks.isNotEmpty() && tasks.all { it.completed }
}

data class SelfStepData(
    val version: Int = 1,
    val tasks: List<DisciplineTask> = emptyList(),
    val records: Map<String, DailyRecord> = emptyMap()
)

data class DisciplineMetrics(
    val currentStreak: Int,
    val bestStreak: Int,
    val successfulDays: Int
)

object MetricsCalculator {
    fun calculate(
        records: Map<String, DailyRecord>,
        today: LocalDate = LocalDate.now()
    ): DisciplineMetrics {
        val successfulDates = records.values
            .asSequence()
            .filter { it.isSuccessful }
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .filterNot { it.isAfter(today) }
            .toSet()

        val currentStart = if (today in successfulDates) today else today.minusDays(1)
        var cursor = currentStart
        var currentStreak = 0
        while (cursor in successfulDates) {
            currentStreak += 1
            cursor = cursor.minusDays(1)
        }

        var bestStreak = 0
        var running = 0
        var previous: LocalDate? = null
        successfulDates.sorted().forEach { date ->
            running = if (previous != null && date == previous!!.plusDays(1)) {
                running + 1
            } else {
                1
            }
            bestStreak = maxOf(bestStreak, running)
            previous = date
        }

        return DisciplineMetrics(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            successfulDays = successfulDates.size
        )
    }
}
