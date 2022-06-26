package com.example.hydrohomie.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.DrinkHistory
import com.example.hydrohomie.domain.ReminderSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Handles the reminders.
 *
 * * [ACTION_SHOW] is fired by the alarm and shows the notification.
 * * [ACTION_DRINK] is fired when the user taps the notification. It adds a glass of water
 *   to the history and recalculates the reminders. The app itself is **not** opened.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 0)
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        when (intent.action) {
            ACTION_SHOW -> showNotification(context, amountMl, reminderId, timestamp)
            ACTION_DRINK -> drink(context, amountMl)
        }
    }

    private fun showNotification(context: Context, amountMl: Int, reminderId: Int, timestamp: Long) {
        val factory = NotificationFactory(context)
        val slot = ReminderSlot(timestampMillis = timestamp, amountMl = amountMl, id = reminderId)
        val coordinator = coordinator(context)
        val notification = factory.buildNotification(slot, coordinator.nextSlotAfter(reminderId))

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(reminderId, notification)
    }

    private fun coordinator(context: Context) = ReminderCoordinator(
        context = context,
        settingsStore = SettingsStore(settingsFile(context)),
        historyStore = HistoryStore(historyFile(context)),
        reminderStore = ReminderStore(remindersFile(context)),
    )

    /**
     * Adds a glass of water to the history.
     *
     * The reminders are **not** recalculated: the click was planned, so the remaining
     * reminders stay as they are. Only a drink that was not planned (over the user
     * interface) leads to a new plan.
     *
     * This happens in the background, the app is not opened.
     */
    private fun drink(context: Context, amountMl: Int) {
        if (amountMl <= 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val historyStore = HistoryStore(historyFile(context))
                val history: DrinkHistory = historyStore.load()
                    .add(amountMl, System.currentTimeMillis())
                historyStore.save(history)

                // The open app should show the new amount right away.
                DataChangedReceiver.notifyChanged(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.example.hydrohomie.action.SHOW_REMINDER"
        const val ACTION_DRINK = "com.example.hydrohomie.action.DRINK"

        const val EXTRA_AMOUNT_ML = "amount_ml"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TIMESTAMP = "timestamp"

        internal fun settingsFile(context: Context) = File(context.filesDir, "settings.json")
        internal fun historyFile(context: Context) = File(context.filesDir, "history.json")
        internal fun remindersFile(context: Context) = File(context.filesDir, "reminders.json")
    }
}
