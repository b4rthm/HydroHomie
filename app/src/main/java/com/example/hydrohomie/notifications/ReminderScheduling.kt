package com.example.hydrohomie.notifications

/**
 * Schedules the reminders.
 *
 * The interface makes the reminder handling replaceable in the tests.
 */
interface ReminderScheduling {

    /** Cancels the old reminders and schedules the new ones. */
    fun reschedule(): Int

    /** Cancels all reminders. */
    fun cancelAll()

    /** When the next reminder is shown, `null` if there is none. */
    fun nextReminderMillis(): Long?
}
