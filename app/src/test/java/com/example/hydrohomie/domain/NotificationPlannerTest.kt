package com.example.hydrohomie.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPlannerTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    private val settings = DrinkSettings(
        goalMl = 2500,
        glassSizeMl = 250,
        startMinuteOfDay = 8 * 60,
        endMinuteOfDay = 20 * 60,
    )

    /** Monday, 13.05.2024, 07:50 in Berlin, before the drinking window starts. */
    private val mondayMorning: Long =
        LocalDate.of(2024, 5, 13).atTime(LocalTime.of(7, 50)).atZone(zone).toInstant().toEpochMilli()

    /** Monday, 13.05.2024, 12:00 in Berlin. */
    private val mondayNoon: Long =
        LocalDate.of(2024, 5, 13).atTime(LocalTime.of(12, 0)).atZone(zone).toInstant().toEpochMilli()

    /** Monday, 13.05.2024, 21:00 in Berlin, the drinking window is over. */
    private val mondayEvening: Long =
        LocalDate.of(2024, 5, 13).atTime(LocalTime.of(21, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun plan(
        now: Long = mondayMorning,
        settings: DrinkSettings = this.settings,
        alreadyDrunkMl: Int = 0,
        daysAhead: Int = 7,
    ) = NotificationPlanner.plan(settings, alreadyDrunkMl, now, zone, daysAhead)

    private fun timeOf(timestampMillis: Long): LocalTime =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalTime()

    @Test
    fun `the goal is split into glasses of the default size`() {
        val today = plan(now = mondayMorning, daysAhead = 0)

        assertEquals(10, today.size)
        assertEquals(10, today.count { it.amountMl == 250 })
        // The last glass contains the remainder so that the sum is exactly the goal.
        assertEquals(250, today.last().amountMl)
        assertEquals(2500, today.sumOf { it.amountMl })
    }

    @Test
    fun `the remainder ends up in the last glass`() {
        val settings = settings.copy(goalMl = 1000, glassSizeMl = 300)

        val today = plan(now = mondayMorning, daysAhead = 0, settings = settings)

        assertEquals(4, today.size)
        assertEquals(300, today[0].amountMl)
        assertEquals(300, today[1].amountMl)
        assertEquals(300, today[2].amountMl)
        assertEquals(100, today[3].amountMl)
        assertEquals(1000, today.sumOf { it.amountMl })
    }

    @Test
    fun `the reminders are spread over the drinking window`() {
        val today = plan(now = mondayMorning, daysAhead = 0)

        // The window is 12 hours for ten glasses, so every 80 minutes. The first reminder
        // is at the beginning of the window, the last one at the end.
        assertEquals(LocalTime.of(8, 0), timeOf(today.first().timestampMillis))
        assertEquals(LocalTime.of(20, 0), timeOf(today.last().timestampMillis))
    }

    @Test
    fun `the first reminder is at the beginning of the drinking window`() {
        // The drinking window starts at 14:30, the user opens the app before that.
        val beforeWindow = LocalDate.of(2024, 5, 13).atTime(LocalTime.of(14, 20))
            .atZone(zone).toInstant().toEpochMilli()
        val settings = settings.copy(startMinuteOfDay = 14 * 60 + 30)

        val today = plan(now = beforeWindow, settings = settings, daysAhead = 0)

        assertEquals(LocalTime.of(14, 30), timeOf(today.first().timestampMillis))
    }

    @Test
    fun `the pauses between the reminders are equal`() {
        val today = plan(now = mondayMorning, daysAhead = 0)

        val pauses = today.zipWithNext { first, second -> second.timestampMillis - first.timestampMillis }
        val first = pauses.first()
        assertTrue(
            "all pauses should have the same length",
            pauses.all { kotlin.math.abs(it - first) <= 1 },
        )
    }

    @Test
    fun `the last reminder is exactly at the end of the drinking window`() {
        val today = plan(now = mondayMorning, daysAhead = 0)
        val endOfWindow = LocalDate.of(2024, 5, 13).atTime(LocalTime.of(20, 0))
            .atZone(zone).toInstant().toEpochMilli()

        assertEquals(endOfWindow, today.last().timestampMillis)
    }

    @Test
    fun `after a glass the next reminder comes after a pause`() {
        // The user drank at 12:00, the window ends at 20:00.
        val today = plan(now = mondayNoon, daysAhead = 0)

        // Eight hours for ten glasses, so every 48 minutes.
        assertEquals(LocalTime.of(12, 48), timeOf(today.first().timestampMillis))
        assertEquals(LocalTime.of(20, 0), timeOf(today.last().timestampMillis))
    }

    @Test
    fun `only the missing amount is planned for today`() {
        val today = plan(now = mondayMorning, alreadyDrunkMl = 500, daysAhead = 0)

        assertEquals(8, today.size)
        assertEquals(2000, today.sumOf { it.amountMl })
    }

    @Test
    fun `the remaining window is used when the day has already started`() {
        val today = plan(now = mondayNoon, daysAhead = 0)

        // The window from 12:00 to 20:00 is eight hours, the goal still needs ten glasses.
        assertEquals(10, today.size)
        assertEquals(LocalTime.of(12, 48), timeOf(today.first().timestampMillis))
        assertEquals(LocalTime.of(20, 0), timeOf(today.last().timestampMillis))
    }

    @Test
    fun `no reminders are planned after the end of the window`() {
        val today = plan(now = mondayEvening, daysAhead = 0)

        assertTrue(today.isEmpty())
    }

    @Test
    fun `no reminders are planned when the goal is reached`() {
        val today = plan(now = mondayMorning, alreadyDrunkMl = 2500, daysAhead = 0)

        assertTrue(today.isEmpty())
    }

    @Test
    fun `the following days are planned with the full goal`() {
        val slots = plan(now = mondayMorning, daysAhead = 3)

        // Today plus three days, each with ten reminders.
        assertEquals(40, slots.size)
        val future = slots.filter { it.timestampMillis > mondayEvening }
        assertEquals(30, future.size)
        assertEquals(7500, future.sumOf { it.amountMl })
    }

    @Test
    fun `a glass size of zero does not lead to a crash`() {
        val slots = plan(now = mondayMorning, settings = settings.copy(glassSizeMl = 0))

        assertTrue(slots.isEmpty())
    }

    @Test
    fun `a goal of zero does not lead to a crash`() {
        val slots = plan(now = mondayMorning, settings = settings.copy(goalMl = 0))

        assertTrue(slots.isEmpty())
    }

    @Test
    fun `an invalid drinking window does not lead to a crash`() {
        val slots = plan(
            now = mondayMorning,
            settings = settings.copy(startMinuteOfDay = 20 * 60, endMinuteOfDay = 8 * 60),
        )

        assertTrue(slots.isEmpty())
    }

    @Test
    fun `no reminder fires immediately`() {
        val today = plan(now = mondayMorning, daysAhead = 0)

        val first = today.first().timestampMillis
        assertTrue(
            "the first reminder must be in the future",
            first >= mondayMorning + NotificationPlanner.LEAD_TIME_SECONDS * 1000L,
        )
    }

    @Test
    fun `reminders in the past are dropped`() {
        val today = plan(now = mondayMorning, daysAhead = 0)

        assertTrue(today.all { it.timestampMillis > mondayMorning })
    }

    @Test
    fun `the whole amount is distributed over the window`() {
        // A window of only 30 minutes, but the amount is spread over the whole window.
        val slots = NotificationPlanner.distribute(
            amountMl = 1_000,
            glassSizeMl = 100,
            windowStartMillis = 0L,
            windowEndMillis = 30 * 60_000L,
        )

        assertEquals(10, slots.size)
        assertEquals(1_000, slots.sumOf { it.amountMl })
        assertEquals(30 * 60_000L, slots.last().timestampMillis)
    }

    @Test
    fun `the future days start with a reminder at the beginning of the window`() {
        val slots = plan(now = mondayMorning, daysAhead = 1)
        val tomorrow = slots.filter { it.timestampMillis > mondayEvening }

        assertEquals(LocalTime.of(8, 0), timeOf(tomorrow.first().timestampMillis))
        assertEquals(LocalTime.of(20, 0), timeOf(tomorrow.last().timestampMillis))
    }

    @Test
    fun `after an unplanned drink the next reminder comes after a full pause`() {
        // At 12:00 the user drank a glass outside of the plan, 2250 ml are missing.
        val today = plan(now = mondayNoon, alreadyDrunkMl = 250, daysAhead = 0)

        // Eight hours for nine glasses, so every 53 minutes. The first reminder is not
        // right after the glass, the last one is at the end of the window.
        assertEquals(9, today.size)
        val first = timeOf(today.first().timestampMillis)
        assertEquals(12, first.hour)
        assertEquals(53, first.minute)
        assertEquals(LocalTime.of(20, 0), timeOf(today.last().timestampMillis))
        assertEquals(2250, today.sumOf { it.amountMl })
    }

    @Test
    fun `the ids of the reminders are unique`() {
        val slots = plan(now = mondayMorning, daysAhead = 7)

        assertEquals(slots.size, slots.map { it.id }.toSet().size)
    }

    @Test
    fun `the reminders use the time zone of the device`() {
        val slots = plan(now = mondayMorning, daysAhead = 0)

        // 08:00 in Berlin is 06:00 UTC, the plan is calculated in local time.
        assertEquals(6, Instant.ofEpochMilli(slots.first().timestampMillis).atZone(ZoneId.of("UTC")).hour)
    }
}
