package com.studentreminder.app

import com.studentreminder.app.data.ReminderOption
import com.studentreminder.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderLogicTest {

    private val examDay = LocalDate.of(2026, 11, 20)
    private val examTime = TimeUtils.toMillis(examDay, LocalTime.of(9, 0))

    @Test
    fun `days-before reminders fire at the same clock time on earlier days`() {
        val sevenDays = ReminderOption.triggerTime(examTime, 7 * 24 * 60L, 8)
        assertEquals(TimeUtils.toMillis(LocalDate.of(2026, 11, 13), LocalTime.of(9, 0)), sevenDays)

        val oneDay = ReminderOption.triggerTime(examTime, 24 * 60L, 8)
        assertEquals(TimeUtils.toMillis(LocalDate.of(2026, 11, 19), LocalTime.of(9, 0)), oneDay)
    }

    @Test
    fun `at exact time fires at the event`() {
        assertEquals(examTime, ReminderOption.triggerTime(examTime, ReminderOption.AT_TIME, 8))
    }

    @Test
    fun `same day fires in the morning of the event day`() {
        val t = ReminderOption.triggerTime(examTime, ReminderOption.SAME_DAY, 8)
        assertEquals(TimeUtils.toMillis(examDay, LocalTime.of(8, 0)), t)
    }

    @Test
    fun `same day falls back to one hour before an early event`() {
        val early = TimeUtils.toMillis(examDay, LocalTime.of(7, 30))
        val t = ReminderOption.triggerTime(early, ReminderOption.SAME_DAY, 8)
        assertEquals(TimeUtils.toMillis(examDay, LocalTime.of(6, 30)), t)
    }

    @Test
    fun `same day never goes before midnight`() {
        val veryEarly = TimeUtils.toMillis(examDay, LocalTime.of(0, 20))
        val t = ReminderOption.triggerTime(veryEarly, ReminderOption.SAME_DAY, 8)
        assertEquals(TimeUtils.toMillis(examDay, LocalTime.MIDNIGHT), t)
    }

    @Test
    fun `custom hours offset`() {
        val t = ReminderOption.triggerTime(examTime, 5 * 60L, 8)
        assertEquals(TimeUtils.toMillis(examDay, LocalTime.of(4, 0)), t)
    }

    @Test
    fun `labels for presets and custom values`() {
        assertEquals("7 days before", ReminderOption.label(7 * 24 * 60L))
        assertEquals("On the same day", ReminderOption.label(ReminderOption.SAME_DAY))
        assertEquals("At the exact time", ReminderOption.label(0))
        assertEquals("10 days before", ReminderOption.label(10 * 24 * 60L))
        assertEquals("1 hour before", ReminderOption.label(60L))
        assertEquals("6 hours before", ReminderOption.label(6 * 60L))
        assertEquals("45 minutes before", ReminderOption.label(45L))
    }

    @Test
    fun `countdown formatting`() {
        val now = 0L
        val target = (2 * 24 * 60 + 3 * 60 + 15) * 60_000L
        assertEquals("2d 3h 15m", TimeUtils.countdown(target, now))
        assertEquals("5h 0m", TimeUtils.countdown(5 * 60 * 60_000L, now))
        assertEquals("now", TimeUtils.countdown(-1, now))
    }

    @Test
    fun `exams sort chronologically`() {
        val db = TimeUtils.toMillis(LocalDate.of(2026, 11, 15), LocalTime.of(9, 0))
        val net = TimeUtils.toMillis(LocalDate.of(2026, 11, 18), LocalTime.of(11, 0))
        val math = TimeUtils.toMillis(LocalDate.of(2026, 11, 25), LocalTime.of(13, 0))
        val sorted = listOf("Mathematics" to math, "Database" to db, "Networking" to net).sortedBy { it.second }
        assertEquals(listOf("Database", "Networking", "Mathematics"), sorted.map { it.first })
        assertTrue(sorted.zipWithNext().all { (a, b) -> a.second <= b.second })
    }
}
