package com.taskfinisher.utils

import java.time.*

/**
 * Calculates the user's current completion streak.
 *
 * Definition: A streak is the number of consecutive *calendar days* (ending
 * today or yesterday) on which the user completed at least one task.
 *
 * Algorithm:
 *  1. Convert all completedAt timestamps → LocalDate (device time zone).
 *  2. Deduplicate dates (multiple completions same day = one day credit).
 *  3. Sort descending.
 *  4. Walk backwards from today; if a day is missing, the streak ends.
 *
 * Edge cases:
 *  - No completions today yet → streak still counts if yesterday was covered.
 *  - Gap > 1 day → streak resets to 0.
 */
object StreakCalculator {

    fun calculate(completionTimestamps: List<Long>): Int {
        if (completionTimestamps.isEmpty()) return 0

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // Distinct completion dates, sorted newest-first
        val dates: List<LocalDate> = completionTimestamps
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSortedSet(compareByDescending { it })
            .toList()

        // The streak can start from today OR yesterday
        // (if user hasn't completed anything yet today)
        val startDay = if (dates.first() == today) today else {
            // If the most recent completion was yesterday, streak is still alive
            if (dates.first() == today.minusDays(1)) today.minusDays(1)
            else return 0  // Last completion was 2+ days ago → no streak
        }

        var streak = 0
        var expected = startDay

        for (date in dates) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date.isBefore(expected)) {
                // Gap: a day was skipped
                break
            }
            // date > expected: multiple completions on same day, skip duplicates
        }

        return streak
    }
}
