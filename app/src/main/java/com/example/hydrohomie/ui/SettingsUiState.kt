package com.example.hydrohomie.ui

import com.example.hydrohomie.domain.SettingsField
import com.example.hydrohomie.domain.SettingsProblem

/**
 * The state of the settings screen.
 */
data class SettingsUiState(
    /** The text of the goal input. */
    val goalText: String = "",
    /** The text of the default glass size input. */
    val glassSizeText: String = "",
    /** The start of the drinking window, in minutes since midnight. */
    val startMinuteOfDay: Int = 8 * 60,
    /** The end of the drinking window, in minutes since midnight. */
    val endMinuteOfDay: Int = 20 * 60,
    /** Whether reminders are shown. */
    val notificationsEnabled: Boolean = true,
    /** The language of the user interface. */
    val language: String = "en",
    /** The problems of the current input. */
    val problems: Map<SettingsField, SettingsProblem> = emptyMap(),
    /** Whether the problems should be shown to the user. */
    val showErrors: Boolean = false,
    /** Whether the settings were saved. */
    val isSaved: Boolean = false,
)
