package com.example.hydrohomie.data

import com.example.hydrohomie.domain.DrinkSettings
import org.json.JSONObject
import java.io.File

/**
 * Stores the settings of the app.
 *
 * The keys are the same as in the previous version of the app, so an existing installation
 * keeps its settings after the update.
 */
class SettingsStore(file: File) {

    private val jsonFile = JsonFile(file)

    /** Reads the settings, falling back to the defaults if there are none. */
    fun load(): DrinkSettings {
        val json = jsonFile.read() ?: return DrinkSettings()
        return fromJson(json)
    }

    /** Writes the settings. */
    fun save(settings: DrinkSettings) {
        jsonFile.write(toJson(settings))
    }

    /** Whether settings were stored before. */
    fun exists(): Boolean = jsonFile.exists()

    private fun fromJson(json: JSONObject): DrinkSettings {
        val defaults = DrinkSettings()
        return DrinkSettings(
            goalMl = json.optInt(KEY_GOAL, defaults.goalMl),
            glassSizeMl = json.optInt(KEY_GLASS_SIZE, defaults.glassSizeMl),
            startMinuteOfDay = json.optInt(
                KEY_START_HOUR,
                defaults.startMinuteOfDay / 60,
            ) * 60 + json.optInt(KEY_START_MINUTE, 0),
            endMinuteOfDay = json.optInt(
                KEY_END_HOUR,
                defaults.endMinuteOfDay / 60,
            ) * 60 + json.optInt(KEY_END_MINUTE, 0),
            // The old key means "notifications are disabled", the new model the other way
            // around.
            notificationsEnabled = !json.optBoolean(KEY_NOTIFICATIONS, false),
            language = json.optString(KEY_LANGUAGE, defaults.language)
                .let { DrinkSettings.normalizeLanguage(it) },
        )
    }

    private fun toJson(settings: DrinkSettings): JSONObject = JSONObject().apply {
        put(KEY_GOAL, settings.goalMl)
        put(KEY_GLASS_SIZE, settings.glassSizeMl)
        put(KEY_START_HOUR, settings.startMinuteOfDay / 60)
        put(KEY_START_MINUTE, settings.startMinuteOfDay % 60)
        put(KEY_END_HOUR, settings.endMinuteOfDay / 60)
        put(KEY_END_MINUTE, settings.endMinuteOfDay % 60)
        put(KEY_NOTIFICATIONS, !settings.notificationsEnabled)
        put(KEY_LANGUAGE, settings.language)
    }

    private companion object {
        // The keys of the previous version of the app.
        const val KEY_GOAL = "totalDrinkAmount"
        const val KEY_GLASS_SIZE = "defaultDrinkSize"
        const val KEY_START_HOUR = "startHour"
        const val KEY_START_MINUTE = "startMin"
        const val KEY_END_HOUR = "endHour"
        const val KEY_END_MINUTE = "endMin"
        const val KEY_NOTIFICATIONS = "disableNotifications"
        const val KEY_LANGUAGE = "language"
    }
}
