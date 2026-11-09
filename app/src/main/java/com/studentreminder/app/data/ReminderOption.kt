package com.studentreminder.app.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Reminder offsets are stored as "minutes before the event". */
object ReminderOption {
    const val AT_TIME = 0L
    /** Special value: the morning of the event day (hour comes from Settings). */
    const val SAME_DAY = -1L

    private const val HOUR = 60L
    private const val DAY = 24 * HOUR

    data class Preset(val minutes: Long, val label: String)

    val presets = listOf(
        Preset(7 * DAY, "7 days before"),
        Preset(5 * DAY, "5 days before"),
        Preset(3 * DAY, "3 days before"),
        Preset(2 * DAY, "2 days before"),
        Preset(1 * DAY, "1 day before"),
        Preset(SAME_DAY, "On the same day"),
        Preset(AT_TIME, "At the exact time"),
    )

    val defaultExam = listOf(7 * DAY, 3 * DAY, 1 * DAY, SAME_DAY)
    val defaultAssignment = listOf(2 * DAY, 1 * DAY, SAME_DAY)

    fun isPreset(minutes: Long) = presets.any { it.minutes == minutes }

    fun label(minutes: Long): String {
        presets.firstOrNull { it.minutes == minutes }?.let { return it.label }
        return when {
            minutes % DAY == 0L -> plural(minutes / DAY, "day") + " before"
            minutes % HOUR == 0L -> plural(minutes / HOUR, "hour") + " before"
            else -> plural(minutes, "minute") + " before"
        }
    }

    /** Sort order for display: furthest-away reminder first, same-day and at-time last. */
    fun sortKey(minutes: Long): Long = when (minutes) {
        SAME_DAY -> -1L
        AT_TIME -> -2L
        else -> minutes
    }

    /**
     * Computes when a reminder should fire.
     * SAME_DAY fires at [sameDayHour]:00 on the event day, or one hour before the
     * event if the event starts before that hour (never before midnight).
     */
    fun triggerTime(eventMillis: Long, offsetMinutes: Long, sameDayHour: Int): Long {
        if (offsetMinutes != SAME_DAY) return eventMillis - offsetMinutes * 60_000L
        val zone = ZoneId.systemDefault()
        val eventDate = Instant.ofEpochMilli(eventMillis).atZone(zone).toLocalDate()
        val startOfDay = eventDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val morning = eventDate.atTime(LocalTime.of(sameDayHour.coerceIn(0, 23), 0))
            .atZone(zone).toInstant().toEpochMilli()
        return if (morning < eventMillis) morning
        else maxOf(startOfDay, eventMillis - 60 * 60_000L)
    }

    private fun plural(n: Long, unit: String) = if (n == 1L) "1 $unit" else "$n ${unit}s"
}
