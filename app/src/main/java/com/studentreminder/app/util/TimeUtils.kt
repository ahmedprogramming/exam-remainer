package com.studentreminder.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun toMillis(date: LocalDate, time: LocalTime): Long =
        LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

    fun toLocalDateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toLocalDate(millis: Long): LocalDate = toLocalDateTime(millis).toLocalDate()

    fun isToday(millis: Long): Boolean = toLocalDate(millis) == LocalDate.now()

    fun daysUntil(millis: Long): Long =
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(millis))

    /** "3d 4h 12m", "5h 2m", "12m" or "now". */
    fun countdown(targetMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - now
        if (diff <= 0) return "now"
        val totalMinutes = diff / 60_000L
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "less than a minute"
        }
    }

    /** Human friendly "in 3 days", "tomorrow", "today", "2 days ago". */
    fun relativeDay(millis: Long): String {
        val d = daysUntil(millis)
        return when {
            d == 0L -> "Today"
            d == 1L -> "Tomorrow"
            d == -1L -> "Yesterday"
            d > 1 -> "In $d days"
            else -> "${-d} days ago"
        }
    }

    /** Text for notifications: "starts in 2 days, 3 hours". */
    fun remainingLong(targetMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - now
        if (diff <= 60_000L) return "now"
        val totalMinutes = diff / 60_000L
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val parts = buildList {
            if (days > 0) add(if (days == 1L) "1 day" else "$days days")
            if (hours > 0) add(if (hours == 1L) "1 hour" else "$hours hours")
            if (days == 0L && minutes > 0) add(if (minutes == 1L) "1 minute" else "$minutes minutes")
        }
        return "in " + parts.joinToString(", ")
    }
}

/** Formats dates/times respecting the user's 12/24-hour preference. */
class Formatters(use24h: Boolean) {
    private val locale = Locale.getDefault()
    private val timeFmt = DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale)
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
    private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    private val longDateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    fun time(t: LocalTime): String = t.format(timeFmt)
    fun time(millis: Long): String = TimeUtils.toLocalDateTime(millis).format(timeFmt)
    fun date(d: LocalDate): String = d.format(dateFmt)
    fun date(millis: Long): String = TimeUtils.toLocalDate(millis).format(dateFmt)
    fun shortDate(millis: Long): String = TimeUtils.toLocalDate(millis).format(shortDateFmt)
    fun longDate(d: LocalDate): String = d.format(longDateFmt)
    fun month(d: LocalDate): String = d.format(monthFmt)
    fun dateTime(millis: Long): String = "${date(millis)} · ${time(millis)}"
}
