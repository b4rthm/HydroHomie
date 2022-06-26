package com.example.hydrohomie.notifications

import android.content.Context
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.NotificationPlanner
import com.example.hydrohomie.domain.ReminderSlot
import java.time.LocalDate
import java.time.ZoneId

/**
 * Keeps the scheduled reminders in sync with the settings and the glasses that were
 * drunk.
 *
 * Every change (a glass of water, an undo, new settings) recalculates the whole plan:
 * the old reminders are cancelled and the new ones are scheduled.
 */
class ReminderCoordinator(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val historyStore: HistoryStore,
    private val reminderStore: ReminderStore,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : ReminderScheduling {

    private val notificationFactory = NotificationFactory(context)
    private val alarmScheduler = AlarmScheduler(context)

    /** Creates the notification channel. */
    fun createChannel() = notificationFactory.createChannel()

    /**
     * Cancels all reminders and schedules the new ones.
     *
     * @return the number of scheduled reminders.
     */
    override fun reschedule(): Int {
        cancelAll()

        val settings = settingsStore.load()
        if (!settings.notificationsEnabled) return 0

        val slots = currentSlots()
        slots.forEach { slot ->
            alarmScheduler.schedule(slot, notificationFactory.showPendingIntent(slot))
        }
        reminderStore.save(slots)
        return slots.size
    }

    /** The reminders that would be scheduled right now. */
    fun currentSlots(): List<ReminderSlot> {
        val settings = settingsStore.load()
        if (!settings.notificationsEnabled) return emptyList()

        val history = historyStore.load()
        val today = LocalDate.now(zoneId)
        val drunkToday = history.amountOn(today, zoneId)

        return NotificationPlanner.plan(
            settings = settings,
            alreadyDrunkMl = drunkToday,
            nowMillis = nowMillis(),
            zoneId = zoneId,
        )
    }

    /**
     * The reminder that follows the one with the given id.
     *
     * It is looked up in the plan that is actually scheduled, so that the hint in the
     * notification is correct.
     */
    fun nextSlotAfter(id: Int): ReminderSlot? =
        reminderStore.load().firstOrNull { it.id > id }

    /**
     * When the next reminder is shown.
     *
     * It is taken from the plan that is actually scheduled, so that the screen and the
     * notifications always show the same time.
     */
    override fun nextReminderMillis(): Long? =
        reminderStore.load()
            .firstOrNull { it.timestampMillis > nowMillis() }
            ?.timestampMillis

    /** Cancels all scheduled reminders. */
    override fun cancelAll() {
        reminderStore.loadIds().forEach { alarmScheduler.cancel(it) }
        reminderStore.save(emptyList())
    }
}
