package com.example.hydrohomie

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hydrohomie.databinding.ActivitySettingsBinding
import com.example.hydrohomie.domain.SettingsField
import com.example.hydrohomie.domain.SettingsProblem
import com.example.hydrohomie.notifications.DataChangedReceiver
import com.example.hydrohomie.ui.SettingsUiState
import com.example.hydrohomie.ui.SettingsViewModel
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The settings of the app: goal, glass size, drinking window, reminders and language.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels<SettingsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as HydroHomieApp
                return SettingsViewModel(
                    settingsStore = app.settingsStore,
                    reminders = app.reminderCoordinator,
                ) as T
            }
        }
    }

    /** Guards the listeners while the state is written into the views. */
    private var isRendering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goalInput.doAfterTextChanged {
            if (!isRendering) viewModel.onGoalChanged(it?.toString().orEmpty())
        }
        binding.glassSizeInput.doAfterTextChanged {
            if (!isRendering) viewModel.onGlassSizeChanged(it?.toString().orEmpty())
        }
        binding.startTimeButton.setOnClickListener { showTimePicker(isStart = true) }
        binding.endTimeButton.setOnClickListener { showTimePicker(isStart = false) }
        binding.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isRendering) viewModel.onNotificationsChanged(checked)
        }
        binding.languageToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isRendering && isChecked) {
                viewModel.onLanguageChanged(
                    if (checkedId == R.id.languageGerman) LANGUAGE_GERMAN else LANGUAGE_ENGLISH,
                )
            }
        }
        binding.saveButton.setOnClickListener { save() }
        binding.backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: SettingsUiState) {
        isRendering = true

        if (binding.goalInput.text?.toString() != state.goalText) {
            binding.goalInput.setText(state.goalText)
        }
        if (binding.glassSizeInput.text?.toString() != state.glassSizeText) {
            binding.glassSizeInput.setText(state.glassSizeText)
        }

        binding.startTimeButton.text = formatTime(state.startMinuteOfDay)
        binding.endTimeButton.text = formatTime(state.endMinuteOfDay)

        binding.notificationsSwitch.isChecked = state.notificationsEnabled

        val languageButton = if (state.language == LANGUAGE_GERMAN) {
            R.id.languageGerman
        } else {
            R.id.languageEnglish
        }
        if (binding.languageToggle.checkedButtonId != languageButton) {
            binding.languageToggle.check(languageButton)
        }

        val problems = if (state.showErrors) state.problems else emptyMap()
        binding.goalLayout.error = problemMessage(problems[SettingsField.GOAL])
        binding.glassSizeLayout.error = problemMessage(problems[SettingsField.GLASS_SIZE])
        binding.startTimeButton.error = problemMessage(problems[SettingsField.TIME_RANGE])
        binding.endTimeButton.error = problemMessage(problems[SettingsField.TIME_RANGE])

        isRendering = false
    }

    private fun save() {
        if (!viewModel.save()) return
        // The open main screen should show the new settings right away.
        DataChangedReceiver.notifyChanged(this)
        applyLanguage(viewModel.state.value.language)
        finish()
    }

    /** Shows the time picker for the start or the end of the drinking window. */
    private fun showTimePicker(isStart: Boolean) {
        val state = viewModel.state.value
        val minuteOfDay = if (isStart) state.startMinuteOfDay else state.endMinuteOfDay

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(minuteOfDay / 60)
            .setMinute(minuteOfDay % 60)
            .setTitleText(R.string.settings_window_label)
            .build()

        picker.addOnPositiveButtonClickListener {
            val newMinute = picker.hour * 60 + picker.minute
            viewModel.onTimeRangeChanged(
                startMinuteOfDay = if (isStart) newMinute else state.startMinuteOfDay,
                endMinuteOfDay = if (isStart) state.endMinuteOfDay else newMinute,
            )
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    /** Switches the language of the whole app. */
    private fun applyLanguage(language: String) {
        val locales = LocaleListCompat.forLanguageTags(language)
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private fun problemMessage(problem: SettingsProblem?): String? = when (problem) {
        null -> null
        SettingsProblem.REQUIRED -> getString(R.string.error_required)
        SettingsProblem.NOT_A_NUMBER -> getString(R.string.error_not_a_number)
        SettingsProblem.NOT_POSITIVE -> getString(R.string.error_not_positive)
        SettingsProblem.TOO_LARGE -> getString(
            R.string.error_too_large,
            com.example.hydrohomie.domain.DrinkSettings.MAX_GOAL_ML,
        )
        SettingsProblem.INVALID_RANGE -> getString(R.string.error_invalid_range)
    }

    private fun formatTime(minuteOfDay: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_GERMAN = "de"
    }
}
