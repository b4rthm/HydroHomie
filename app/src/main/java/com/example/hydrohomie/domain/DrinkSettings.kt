package com.example.hydrohomie.domain

/**
 * The settings of the app.
 *
 * The drinking window is stored as minutes since midnight, which makes it independent of
 * the time zone and easy to calculate with.
 */
data class DrinkSettings(
    /** The daily goal in ml. */
    val goalMl: Int = DEFAULT_GOAL_ML,
    /** The size of a default glass in ml. */
    val glassSizeMl: Int = DEFAULT_GLASS_SIZE_ML,
    /** The minute of the day the drinking window starts. */
    val startMinuteOfDay: Int = DEFAULT_START_MINUTE,
    /** The minute of the day the drinking window ends. */
    val endMinuteOfDay: Int = DEFAULT_END_MINUTE,
    /** Whether reminders are shown at all. */
    val notificationsEnabled: Boolean = true,
    /** The language of the user interface, `en` or `de`. */
    val language: String = DEFAULT_LANGUAGE,
) {

    /** The duration of the drinking window in minutes. */
    val windowMinutes: Int
        get() = endMinuteOfDay - startMinuteOfDay

    companion object {
        const val DEFAULT_GOAL_ML = 2500
        const val DEFAULT_GLASS_SIZE_ML = 250
        const val DEFAULT_START_MINUTE = 8 * 60
        const val DEFAULT_END_MINUTE = 20 * 60

        /** The largest goal that can be entered. */
        const val MAX_GOAL_ML = 20_000

        /** The largest glass size that can be entered. */
        const val MAX_GLASS_SIZE_ML = 5_000

        /** The default language of the user interface. */
        const val DEFAULT_LANGUAGE = "en"

        /** The language of the user interface, `en` or `de`. */
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_GERMAN = "de"

        /**
         * Brings a language code into the form the app uses.
         *
         * The previous version of the app used `eng` and `deu`.
         */
        fun normalizeLanguage(language: String): String = when (language.lowercase()) {
            "de", "deu", "german", "deutsch" -> LANGUAGE_GERMAN
            else -> LANGUAGE_ENGLISH
        }
    }
}
