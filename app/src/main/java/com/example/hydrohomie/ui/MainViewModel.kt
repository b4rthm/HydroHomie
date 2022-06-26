package com.example.hydrohomie.ui

import androidx.lifecycle.ViewModel
import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.DrinkHistory
import com.example.hydrohomie.domain.DrinkProgress
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.notifications.ReminderScheduling
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

/**
 * The state of the main screen and everything that changes the history.
 */
class MainViewModel(
    private val settingsStore: SettingsStore,
    private val historyStore: HistoryStore,
    private val reminders: ReminderScheduling,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        reload()
    }

    /** Reads the settings and the history and updates the screen. */
    fun reload() {
        val settings = settingsStore.load()
        val history = historyStore.load()
        publish(settings, history)
    }

    /** Sets the size of the glass the user is about to drink. */
    fun onGlassSizeChanged(sizeMl: Int) {
        _state.update { it.copy(glassSizeMl = sizeMl.coerceAtLeast(0)) }
    }

    /** The size of the glass that is drunk, the default size if the field is empty. */
    private fun currentGlassSize(): Int =
        _state.value.glassSizeMl.takeIf { it > 0 } ?: _state.value.settings.glassSizeMl

    /**
     * Adds a glass of water.
     *
     * @param amountMl the amount that was drunk, the entered glass size by default.
     */
    fun drink(amountMl: Int = currentGlassSize()) {
        if (amountMl <= 0) return
        val history = historyStore.load().add(amountMl, nowMillis())
        historyStore.save(history)
        publish(settingsStore.load(), history)
        reminders.reschedule()
    }

    /** Removes the glass that was drunk last. */
    fun undo() {
        val history = historyStore.load()
        if (history.entries.isEmpty()) return
        val undone = history.undo()
        historyStore.save(undone)
        publish(settingsStore.load(), undone)
        reminders.reschedule()
    }

    private fun publish(settings: DrinkSettings, history: DrinkHistory) {
        val today = LocalDate.now(zoneId)
        val progress = DrinkProgress.of(history, settings, today, zoneId)
        _state.update { current ->
            current.copy(
                progress = progress,
                settings = settings,
                lastDrinkMl = history.lastEntry?.amountMl,
                nextReminderMillis = reminders.nextReminderMillis(),
            )
        }
    }
}
