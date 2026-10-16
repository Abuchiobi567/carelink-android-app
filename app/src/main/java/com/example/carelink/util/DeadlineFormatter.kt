package com.example.carelink.util

import java.util.concurrent.TimeUnit

// "Due in 2 days" / "Overdue by 3 hours" - text-based deadline indicator
object DeadlineFormatter {

    fun format(deadlineMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (deadlineMillis == null) return "No deadline set"
        val diff = deadlineMillis - nowMillis
        val absDiff = if (diff < 0) -diff else diff
        val days = TimeUnit.MILLISECONDS.toDays(absDiff)
        val hours = TimeUnit.MILLISECONDS.toHours(absDiff) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(absDiff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(absDiff))

        return if (diff >= 0) {
            when {
                days >= 1 -> "Due in $days ${pluralise("day", days)}"
                TimeUnit.MILLISECONDS.toHours(absDiff) >= 1 -> {
                    val totalHours = TimeUnit.MILLISECONDS.toHours(absDiff)
                    "Due in $totalHours ${pluralise("hour", totalHours)}"
                }
                minutes >= 1 -> "Due in $minutes ${pluralise("minute", minutes)}"
                else -> "Due now"
            }
        } else {
            when {
                days >= 1 -> "Overdue by $days ${pluralise("day", days)}"
                hours >= 1 -> "Overdue by $hours ${pluralise("hour", hours)}"
                else -> "Overdue"
            }
        }
    }

    fun isOverdue(deadlineMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (deadlineMillis == null) return false
        return deadlineMillis < nowMillis
    }

    private fun pluralise(word: String, count: Long): String =
        if (count == 1L) word else "${word}s"
}