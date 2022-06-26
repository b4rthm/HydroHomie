package com.example.hydrohomie

import android.app.Application
import android.app.AlarmManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.notifications.ReminderCoordinator
import com.example.hydrohomie.notifications.ReminderStore
import java.io.File

/**
 * Holds the objects that are shared by the screens of the app.
 */
class HydroHomieApp : Application() {

    val settingsStore: SettingsStore by lazy { SettingsStore(File(filesDir, SETTINGS_FILE)) }

    val historyStore: HistoryStore by lazy { HistoryStore(File(filesDir, HISTORY_FILE)) }

    val reminderStore: ReminderStore by lazy { ReminderStore(File(filesDir, REMINDERS_FILE)) }

    val reminderCoordinator: ReminderCoordinator by lazy {
        ReminderCoordinator(
            context = this,
            settingsStore = settingsStore,
            historyStore = historyStore,
            reminderStore = reminderStore,
        )
    }

    override fun onCreate() {
        super.onCreate()
        reminderCoordinator.createChannel()
        applyStoredLanguage()
        planReminders()
    }

    /**
     * Uses the language of the settings for the first start.
     *
     * Afterwards AppCompat takes care of the language, it also remembers the choice of the
     * user.
     */
    private fun applyStoredLanguage() {
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
        val language = settingsStore.load().language
        if (language.isEmpty()) return
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(DrinkSettings.normalizeLanguage(language)),
        )
    }

    /**
     * Plans the reminders if there are none.
     *
     * The plan is only calculated when there is nothing planned, so that opening the app
     * does not move the reminders that are already set. After a reboot the
     * [BootReceiver] takes care of it.
     */
    private fun planReminders() {
        if (!settingsStore.exists()) return
        if (reminderStore.load().isNotEmpty()) return
        if (!settingsStore.load().notificationsEnabled) return
        reminderCoordinator.reschedule()
    }

    /** Whether the system allows reminders at an exact time. */
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    companion object {
        const val SETTINGS_FILE = "settings.json"
        const val HISTORY_FILE = "history.json"
        const val REMINDERS_FILE = "reminders.json"

        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_GERMAN = "de"
    }
}
