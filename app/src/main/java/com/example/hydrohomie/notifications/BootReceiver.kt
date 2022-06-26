package com.example.hydrohomie.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.HydroHomieApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Schedules the reminders again after a reboot or an update of the app.
 *
 * All alarms are lost when the device is switched off, so they have to be planned again.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderCoordinator(
                    context = context,
                    settingsStore = SettingsStore(File(context.filesDir, HydroHomieApp.SETTINGS_FILE)),
                    historyStore = HistoryStore(File(context.filesDir, HydroHomieApp.HISTORY_FILE)),
                    reminderStore = ReminderStore(File(context.filesDir, HydroHomieApp.REMINDERS_FILE)),
                ).reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
