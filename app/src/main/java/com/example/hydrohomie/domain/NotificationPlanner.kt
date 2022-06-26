package com.example.hydrohomie.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A single reminder: when it is shown and how much should be drunk. */
data class ReminderSlot(
    /** When the reminder is shown. */
    val timestampMillis: Long,
    /** How much should be drunk, in ml. */
    val amountMl: Int,
    /** A stable id for the reminder, used to schedule and cancel the alarm. */
    val id: Int,
)

/**
 * Calculates when the reminders are shown.
 *
 * The plan is calculated in the time zone of the device, so the drinking window always
 * means the same local time, no matter where the user is.
 */
object NotificationPlanner {

    /** How many days in advance the reminders are planned. */
    const val DAYS_AHEAD = 7

    /**
     * The first reminder is at least this many seconds in the future.
     *
     * Without it a reminder that is planned for this very moment would fire immediately.
     */
    const val LEAD_TIME_SECONDS = 30L

    /**
     * Calculates all reminders.
     *
     * For today only the still missing amount is planned and only inside the remaining
     * drinking window. For the following days the full goal is planned.
     *
     * @param settings the current settings.
     * @param alreadyDrunkMl the amount that was drunk today, in ml.
     * @param nowMillis the current time.
     * @param zoneId the time zone of the device.
     */
    fun plan(
        settings: DrinkSettings,
        alreadyDrunkMl: Int,
        nowMillis: Long,
        zoneId: ZoneId,
        daysAhead: Int = DAYS_AHEAD,
    ): List<ReminderSlot> {
        val glassSize = settings.glassSizeMl
        if (glassSize <= 0 || settings.goalMl <= 0) return emptyList()

        val zone = zoneId
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate()
        val startMinute = settings.startMinuteOfDay
        val endMinute = settings.endMinuteOfDay
        if (endMinute <= startMinute) return emptyList()

        val slots = mutableListOf<ReminderSlot>()

        // Today: only the missing amount and only inside the remaining window.
        val todayStart = today.atTime(LocalTime.ofSecondOfDay(startMinute * 60L))
            .atZone(zone).toInstant().toEpochMilli()
        val todayEnd = today.atTime(LocalTime.ofSecondOfDay(endMinute * 60L))
            .atZone(zone).toInstant().toEpochMilli()
        val windowStart = maxOf(todayStart, nowMillis)
        if (windowStart < todayEnd) {
            val missing = settings.goalMl - alreadyDrunkMl
            // If the drinking window has already started, the first reminder comes after a
            // full pause: after a glass was drunk there should be a pause before the next
            // reminder. If the window still lies ahead, the first reminder is at the
            // beginning of the window.
            val pauseFirst = nowMillis > todayStart
            slots += distribute(missing, glassSize, windowStart, todayEnd, pauseFirst = pauseFirst)
        }

        // The following days: the full goal.
        for (dayOffset in 1..daysAhead) {
            val day: LocalDate = today.plusDays(dayOffset.toLong())
            val start = day.atTime(LocalTime.ofSecondOfDay(startMinute * 60L))
                .atZone(zone).toInstant().toEpochMilli()
            val end = day.atTime(LocalTime.ofSecondOfDay(endMinute * 60L))
                .atZone(zone).toInstant().toEpochMilli()
            slots += distribute(settings.goalMl, glassSize, start, end)
        }

        // Reminders that are too close to now are dropped.
        val planned = slots.filter { it.timestampMillis > nowMillis + LEAD_TIME_SECONDS * 1000L }

        return planned.mapIndexed { index, slot -> slot.copy(id = idFor(index, slot.timestampMillis)) }
    }

    /**
     * Distributes [amountMl] over the window.
     *
     * The amount is split into glasses of [glassSizeMl] ml. The last glass contains the
     * remainder so that the sum is exactly [amountMl].
     *
     * The pauses between the reminders are always the same length and the last reminder is
     * exactly at the end of the window.
     *
     * @param pauseFirst `true` if the window starts with a pause. That is the case if a
     *   glass was just drunk: the next reminder then comes after a full pause instead of
     *   right away. With `false` the first reminder is at the beginning of the window.
     */
    fun distribute(
        amountMl: Int,
        glassSizeMl: Int,
        windowStartMillis: Long,
        windowEndMillis: Long,
        pauseFirst: Boolean = false,
    ): List<ReminderSlot> {
        if (amountMl <= 0 || glassSizeMl <= 0 || windowEndMillis <= windowStartMillis) return emptyList()

        val wantedCount = (amountMl + glassSizeMl - 1) / glassSizeMl
        val count = wantedCount

        val slots = mutableListOf<ReminderSlot>()
        val windowMillis = windowEndMillis - windowStartMillis
        for (index in 0 until count) {
            val amount = if (index == count - 1) amountMl - (count - 1) * glassSizeMl else glassSizeMl
            val timestamp = if (pauseFirst) {
                // Pause first, then drink: the last reminder is at the end of the window.
                windowStartMillis + windowMillis * (index + 1) / count
            } else if (count > 1) {
                // The first reminder is at the beginning of the window, the last one at the
                // end, everything in between is equally spread.
                windowStartMillis + windowMillis * index / (count - 1)
            } else {
                // Only one reminder: it is at the end of the window.
                windowEndMillis
            }
            slots += ReminderSlot(
                timestampMillis = timestamp,
                amountMl = amount,
                id = 0,
            )
        }
        return slots
    }

    /** A stable id for a reminder, so that it can be cancelled again later. */
    private fun idFor(index: Int, timestampMillis: Long): Int = index
}
