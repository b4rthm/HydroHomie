package com.example.hydrohomie.ui

import androidx.lifecycle.ViewModel
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.domain.SettingsField
import com.example.hydrohomie.domain.SettingsProblem
import com.example.hydrohomie.domain.SettingsValidation
import com.example.hydrohomie.domain.SettingsValidator
import com.example.hydrohomie.notifications.ReminderScheduling
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The state of the settings screen.
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val reminders: ReminderScheduling,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var storedSettings: DrinkSettings = DrinkSettings()

    init {
        load()
    }

    /** Reads the stored settings into the screen. */
    fun load() {
        val settings = settingsStore.load()
        storedSettings = settings
        _state.value = SettingsUiState(
            goalText = settings.goalMl.toString(),
            glassSizeText = settings.glassSizeMl.toString(),
            startMinuteOfDay = settings.startMinuteOfDay,
            endMinuteOfDay = settings.endMinuteOfDay,
            notificationsEnabled = settings.notificationsEnabled,
            language = settings.language,
        )
    }

    /** Called when the goal input changes. */
    fun onGoalChanged(text: String) = updateInput { it.copy(goalText = text) }

    /** Called when the default glass size input changes. */
    fun onGlassSizeChanged(text: String) = updateInput { it.copy(glassSizeText = text) }

    /** Called when the drinking window changes. */
    fun onTimeRangeChanged(startMinuteOfDay: Int, endMinuteOfDay: Int) =
        updateInput { it.copy(startMinuteOfDay = startMinuteOfDay, endMinuteOfDay = endMinuteOfDay) }

    /** Called when the notifications are switched on or off. */
    fun onNotificationsChanged(enabled: Boolean) =
        updateInput { it.copy(notificationsEnabled = enabled) }

    /** Called when the language changes. */
    fun onLanguageChanged(language: String) =
        updateInput { it.copy(language = language) }

    /**
     * Validates and saves the settings.
     *
     * @return `true` if the settings were valid and saved.
     */
    fun save(): Boolean {
        val current = _state.value
        val validation = SettingsValidator.validate(
            goal = current.goalText,
            glassSize = current.glassSizeText,
            startMinuteOfDay = current.startMinuteOfDay,
            endMinuteOfDay = current.endMinuteOfDay,
        )

        if (validation is SettingsValidation.Invalid) {
            _state.update { it.copy(showErrors = true, problems = validation.problems) }
            return false
        }

        val settings = validation.settingsOrNull!!.copy(
            notificationsEnabled = current.notificationsEnabled,
            language = current.language,
        )
        settingsStore.save(settings)
        storedSettings = settings
        _state.update { it.copy(showErrors = false, problems = emptyMap(), isSaved = true) }

        // The reminders depend on the settings, so they are calculated again.
        reminders.reschedule()
        return true
    }

    private fun updateInput(transform: (SettingsUiState) -> SettingsUiState) {
        _state.update { current ->
            val updated = transform(current)
            val problems = if (updated.showErrors) {
                SettingsValidator.validate(
                    goal = updated.goalText,
                    glassSize = updated.glassSizeText,
                    startMinuteOfDay = updated.startMinuteOfDay,
                    endMinuteOfDay = updated.endMinuteOfDay,
                ).problems
            } else {
                emptyMap()
            }
            updated.copy(problems = problems)
        }
    }
}
