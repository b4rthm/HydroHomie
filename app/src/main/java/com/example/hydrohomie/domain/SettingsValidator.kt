package com.example.hydrohomie.domain

/** A field of the settings screen that can contain an error. */
enum class SettingsField {
    GOAL,
    GLASS_SIZE,
    TIME_RANGE,
}

/** The reason why a settings input is not valid. */
enum class SettingsProblem {
    REQUIRED,
    NOT_A_NUMBER,
    NOT_POSITIVE,
    TOO_LARGE,
    INVALID_RANGE,
}

/** The result of validating the settings input. */
sealed interface SettingsValidation {

    /** The problems of the input, empty if the input is valid. */
    val problems: Map<SettingsField, SettingsProblem>

    /** The input describes valid settings. */
    data class Valid(val settings: DrinkSettings) : SettingsValidation {
        override val problems: Map<SettingsField, SettingsProblem> = emptyMap()
    }

    /** The input contains at least one problem. */
    data class Invalid(override val problems: Map<SettingsField, SettingsProblem>) : SettingsValidation

    /** The settings if the validation succeeded, otherwise `null`. */
    val settingsOrNull: DrinkSettings?
        get() = (this as? Valid)?.settings
}

/**
 * Validates the raw text of the settings screen.
 *
 * The validation is free of Android classes and can therefore be tested on the JVM.
 */
object SettingsValidator {

    /**
     * Validates the given input.
     *
     * @param goal the text of the goal input.
     * @param glassSize the text of the default glass size input.
     * @param startMinuteOfDay the start of the drinking window.
     * @param endMinuteOfDay the end of the drinking window.
     */
    fun validate(
        goal: String,
        glassSize: String,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
    ): SettingsValidation {
        val problems = mutableMapOf<SettingsField, SettingsProblem>()

        val goalMl = parseAmount(goal, DrinkSettings.MAX_GOAL_ML, problems, SettingsField.GOAL)
        val glassSizeMl = parseAmount(
            glassSize,
            DrinkSettings.MAX_GLASS_SIZE_ML,
            problems,
            SettingsField.GLASS_SIZE,
        )

        if (startMinuteOfDay >= endMinuteOfDay) {
            problems[SettingsField.TIME_RANGE] = SettingsProblem.INVALID_RANGE
        }

        if (problems.isNotEmpty()) return SettingsValidation.Invalid(problems)

        return SettingsValidation.Valid(
            DrinkSettings(
                goalMl = goalMl!!,
                glassSizeMl = glassSizeMl!!,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
            ),
        )
    }

    private fun parseAmount(
        raw: String,
        max: Int,
        problems: MutableMap<SettingsField, SettingsProblem>,
        field: SettingsField,
    ): Int? {
        val text = raw.trim()
        if (text.isEmpty()) {
            problems[field] = SettingsProblem.REQUIRED
            return null
        }
        val value = text.toIntOrNull()
        if (value == null) {
            problems[field] = SettingsProblem.NOT_A_NUMBER
            return null
        }
        if (value <= 0) {
            problems[field] = SettingsProblem.NOT_POSITIVE
            return null
        }
        if (value > max) {
            problems[field] = SettingsProblem.TOO_LARGE
            return null
        }
        return value
    }
}
