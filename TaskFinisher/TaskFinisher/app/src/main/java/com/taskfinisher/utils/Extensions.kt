package com.taskfinisher.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

// ─── Long (epoch millis) helpers ─────────────────────────────────────────────

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

fun Long.formatDate(): String {
    val dt = this.toLocalDateTime()
    val today = LocalDate.now()
    val date = dt.toLocalDate()
    return when {
        date == today -> "Today ${dt.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        date == today.minusDays(1) -> "Yesterday"
        date == today.plusDays(1) -> "Tomorrow"
        else -> dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

fun Long.formatDeadline(): String {
    val dt = this.toLocalDateTime()
    val today = LocalDate.now()
    val date = dt.toLocalDate()
    val timeStr = dt.format(DateTimeFormatter.ofPattern("h:mm a"))
    return when {
        date == today -> "Today, $timeStr"
        date == today.minusDays(1) -> "Yesterday, $timeStr"
        date == today.plusDays(1) -> "Tomorrow, $timeStr"
        else -> dt.format(DateTimeFormatter.ofPattern("EEE, MMM d • $timeStr"))
    }
}

fun Long.isToday(): Boolean = this.toLocalDate() == LocalDate.now()

fun Long.isOverdue(): Boolean = this < System.currentTimeMillis()

// ─── LocalDate / LocalDateTime helpers ───────────────────────────────────────

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    this.atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    this.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

fun LocalDate.tomorrowStartMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    this.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

// ─── UUID shorthand ───────────────────────────────────────────────────────────

fun newId(): String = UUID.randomUUID().toString()
