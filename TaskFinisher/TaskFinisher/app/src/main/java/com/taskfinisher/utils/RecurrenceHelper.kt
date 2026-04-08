package com.taskfinisher.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parses and evaluates the recurrenceRule string format:
 *
 *   "FREQ=DAILY;INTERVAL=1"
 *   "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR"
 *   "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=15"
 *   "FREQ=CUSTOM;INTERVAL=3"   ← every N days
 *
 * The design mirrors a small subset of iCalendar RRULE to keep it readable.
 */
object RecurrenceHelper {

    /** Build a rule string from discrete UI choices */
    fun buildRule(
        freq: Frequency,
        interval: Int = 1,
        byDay: List<DayOfWeek> = emptyList(),   // for WEEKLY
        byMonthDay: Int? = null                  // for MONTHLY
    ): String {
        val sb = StringBuilder("FREQ=${freq.name};INTERVAL=$interval")
        if (freq == Frequency.WEEKLY && byDay.isNotEmpty()) {
            sb.append(";BYDAY=${byDay.joinToString(",") { it.toRuleCode() }}")
        }
        if (freq == Frequency.MONTHLY && byMonthDay != null) {
            sb.append(";BYMONTHDAY=$byMonthDay")
        }
        return sb.toString()
    }

    /**
     * Given the rule and a *completed* deadline (or now if no deadline),
     * returns the next occurrence as epoch millis.
     *
     * Returns null if the rule cannot be parsed (treat as one-time task).
     */
    fun nextOccurrence(rule: String, completedDeadline: Long): Long? {
        val params = parseRule(rule) ?: return null
        val base = Instant.ofEpochMilli(completedDeadline)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        return when (params.freq) {
            Frequency.DAILY, Frequency.CUSTOM -> {
                base.plusDays(params.interval.toLong())
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            }

            Frequency.WEEKLY -> {
                if (params.byDay.isEmpty()) {
                    // No specific days → just add 7 * interval days
                    base.plusWeeks(params.interval.toLong())
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                } else {
                    // Find the next day-of-week in the list after `base`
                    nextWeekdayAfter(base, params.byDay)
                        ?.atZone(ZoneId.systemDefault())
                        ?.toInstant()?.toEpochMilli()
                }
            }

            Frequency.MONTHLY -> {
                val targetDay = params.byMonthDay ?: base.dayOfMonth
                // Advance to the same day-of-month in the next occurrence month
                var next = base.plusMonths(params.interval.toLong())
                // Clamp day to valid range for that month
                val maxDay = next.toLocalDate().lengthOfMonth()
                next = next.withDayOfMonth(minOf(targetDay, maxDay))
                next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun nextWeekdayAfter(
        from: LocalDateTime,
        days: List<DayOfWeek>
    ): LocalDateTime? {
        // Walk up to 7 days forward to find the next matching day-of-week
        for (offset in 1..7) {
            val candidate = from.plusDays(offset.toLong())
            if (candidate.dayOfWeek in days) return candidate
        }
        return null
    }

    data class ParsedRule(
        val freq: Frequency,
        val interval: Int,
        val byDay: List<DayOfWeek>,
        val byMonthDay: Int?
    )

    fun parseRule(rule: String): ParsedRule? {
        return try {
            val map = rule.split(";").associate {
                val (k, v) = it.split("=")
                k to v
            }
            val freq = Frequency.valueOf(map["FREQ"] ?: return null)
            val interval = map["INTERVAL"]?.toIntOrNull() ?: 1
            val byDay = map["BYDAY"]?.split(",")?.mapNotNull { it.toDayOfWeek() } ?: emptyList()
            val byMonthDay = map["BYMONTHDAY"]?.toIntOrNull()
            ParsedRule(freq, interval, byDay, byMonthDay)
        } catch (_: Exception) {
            null
        }
    }

    /** Human-readable summary shown in the UI */
    fun describeRule(rule: String): String {
        val p = parseRule(rule) ?: return "Recurring"
        return when (p.freq) {
            Frequency.DAILY -> if (p.interval == 1) "Every day" else "Every ${p.interval} days"
            Frequency.CUSTOM -> "Every ${p.interval} days"
            Frequency.WEEKLY -> {
                val days = if (p.byDay.isEmpty()) ""
                else " on ${p.byDay.joinToString(", ") { it.toShortName() }}"
                if (p.interval == 1) "Every week$days" else "Every ${p.interval} weeks$days"
            }
            Frequency.MONTHLY -> {
                val day = if (p.byMonthDay != null) " on the ${p.byMonthDay.ordinal()}" else ""
                if (p.interval == 1) "Every month$day" else "Every ${p.interval} months$day"
            }
        }
    }

    private fun Int.ordinal(): String {
        val suffix = when {
            this in 11..13 -> "th"
            this % 10 == 1 -> "st"
            this % 10 == 2 -> "nd"
            this % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$this$suffix"
    }

    enum class Frequency { DAILY, WEEKLY, MONTHLY, CUSTOM }

    private fun DayOfWeek.toRuleCode() = when (this) {
        DayOfWeek.MONDAY -> "MO"; DayOfWeek.TUESDAY -> "TU"
        DayOfWeek.WEDNESDAY -> "WE"; DayOfWeek.THURSDAY -> "TH"
        DayOfWeek.FRIDAY -> "FR"; DayOfWeek.SATURDAY -> "SA"
        DayOfWeek.SUNDAY -> "SU"
    }

    private fun String.toDayOfWeek() = when (this) {
        "MO" -> DayOfWeek.MONDAY; "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY; "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY; "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY; else -> null
    }

    private fun DayOfWeek.toShortName() = when (this) {
        DayOfWeek.MONDAY -> "Mon"; DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"; DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"; DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }
}
