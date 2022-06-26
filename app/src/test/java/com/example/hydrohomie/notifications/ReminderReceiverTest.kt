package com.example.hydrohomie.notifications

import android.content.Intent
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReminderReceiverTest {

    private lateinit var historyStore: HistoryStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        historyStore = HistoryStore(File(context.filesDir, "history.json"))
        historyStore.save(com.example.hydrohomie.domain.DrinkHistory.EMPTY)
    }

    private fun sendDrinkIntent(amountMl: Int) {
        val context = RuntimeEnvironment.getApplication()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DRINK
            putExtra(ReminderReceiver.EXTRA_AMOUNT_ML, amountMl)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, 1)
            putExtra(ReminderReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        ReminderReceiver().onReceive(context, intent)
    }

    /** Waits until the background work of the receiver is done. */
    private fun awaitHistory(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (historyStore.load().totalMl == expected) return
            Thread.sleep(20)
        }
    }

    @Test
    fun `tapping a notification counts a glass`() {
        sendDrinkIntent(250)

        awaitHistory(250)

        assertEquals(250, historyStore.load().totalMl)
    }

    @Test
    fun `tapping a notification counts the amount of the notification`() {
        sendDrinkIntent(500)

        awaitHistory(500)

        assertEquals(500, historyStore.load().totalMl)
    }

    @Test
    fun `tapping a notification does not open the app`() {
        val context = RuntimeEnvironment.getApplication()

        sendDrinkIntent(250)
        awaitHistory(250)

        // No activity was started, the glass is counted in the background.
        val startedActivities = org.robolectric.Shadows.shadowOf(context as android.app.Application)
            .nextStartedActivity
        assertEquals(null, startedActivities)
    }

    @Test
    fun `the reminders stay as they are after a glass`() {
        val context = RuntimeEnvironment.getApplication()
        val settingsStore = SettingsStore(File(context.filesDir, "settings.json"))
        settingsStore.save(com.example.hydrohomie.domain.DrinkSettings())

        // A plan that was calculated before the glass was drunk.
        val reminderStore = ReminderStore(File(context.filesDir, "reminders.json"))
        reminderStore.save(
            listOf(
                com.example.hydrohomie.domain.ReminderSlot(1_000L, 250, 1),
                com.example.hydrohomie.domain.ReminderSlot(2_000L, 250, 2),
                com.example.hydrohomie.domain.ReminderSlot(3_000L, 250, 3),
            ),
        )

        sendDrinkIntent(250)
        awaitHistory(250)

        // The click was planned, so the reminders are not moved.
        assertEquals(listOf(1, 2, 3), reminderStore.loadIds())
    }
}
