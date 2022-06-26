package com.example.hydrohomie.ui

import com.example.hydrohomie.data.HistoryStore
import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.notifications.ReminderScheduling
import java.io.File
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var settingsFile: File
    private lateinit var historyFile: File
    private lateinit var settingsStore: SettingsStore
    private lateinit var historyStore: HistoryStore
    private lateinit var reminders: FakeReminderScheduling

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    @Before
    fun setUp() {
        settingsFile = File.createTempFile("settings", ".json").also { it.delete() }
        historyFile = File.createTempFile("history", ".json").also { it.delete() }
        settingsStore = SettingsStore(settingsFile)
        historyStore = HistoryStore(historyFile)
        reminders = FakeReminderScheduling()
    }

    private fun viewModel() = MainViewModel(
        settingsStore = settingsStore,
        historyStore = historyStore,
        reminders = reminders,
        zoneId = zone,
        // The glass is drunk today at 9 o'clock.
        nowMillis = {
            java.time.LocalDate.now(zone).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        },
    )

    @Test
    fun `the state shows the progress of the day`() {
        settingsStore.save(DrinkSettings(goalMl = 2500, glassSizeMl = 250))

        val viewModel = viewModel()

        assertEquals(2500, viewModel.state.value.progress.goalMl)
        assertEquals(0, viewModel.state.value.progress.drunkMl)
    }

    @Test
    fun `drinking adds a glass and saves it`() {
        val viewModel = viewModel()
        viewModel.onGlassSizeChanged(500)

        viewModel.drink()

        assertEquals(500, viewModel.state.value.progress.drunkMl)
        assertEquals(500, historyStore.load().totalMl)
    }

    @Test
    fun `drinking recalculates the reminders`() {
        val viewModel = viewModel()

        viewModel.drink()

        assertEquals(1, reminders.rescheduleCount)
    }

    @Test
    fun `an empty glass size uses the default glass size`() {
        settingsStore.save(DrinkSettings(goalMl = 2500, glassSizeMl = 300))
        val viewModel = viewModel()
        viewModel.onGlassSizeChanged(0)

        viewModel.drink()

        assertEquals(300, viewModel.state.value.progress.drunkMl)
    }

    @Test
    fun `a glass of zero is not counted`() {
        val viewModel = viewModel()

        viewModel.drink(0)

        assertEquals(0, viewModel.state.value.progress.drunkMl)
        assertEquals(0, reminders.rescheduleCount)
    }

    @Test
    fun `undo removes the glass that was drunk last`() {
        val viewModel = viewModel()
        viewModel.onGlassSizeChanged(250)
        viewModel.drink()
        viewModel.onGlassSizeChanged(500)
        viewModel.drink()

        assertEquals(750, viewModel.state.value.progress.drunkMl)

        viewModel.undo()

        // The last glass of 500 ml is removed, not the 250 ml that is entered now.
        assertEquals(250, viewModel.state.value.progress.drunkMl)
        assertEquals(250, historyStore.load().totalMl)
    }

    @Test
    fun `undo without a glass does nothing`() {
        val viewModel = viewModel()

        viewModel.undo()

        assertEquals(0, viewModel.state.value.progress.drunkMl)
        assertEquals(0, reminders.rescheduleCount)
    }

    @Test
    fun `the undo button is only enabled with a glass to undo`() {
        val viewModel = viewModel()
        assertNull(viewModel.state.value.lastDrinkMl)

        viewModel.drink()

        assertEquals(250, viewModel.state.value.lastDrinkMl)
    }

    @Test
    fun `reload picks up the changes of the notification`() {
        val viewModel = viewModel()

        // The glass was drunk by tapping the notification.
        val today = java.time.LocalDate.now(zone).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        historyStore.save(com.example.hydrohomie.domain.DrinkHistory.EMPTY.add(300, today))
        viewModel.reload()

        assertEquals(300, viewModel.state.value.progress.drunkMl)
    }

    @Test
    fun `the next reminder is shown`() {
        reminders.nextReminder = 12_345L

        val viewModel = viewModel()

        assertEquals(12_345L, viewModel.state.value.nextReminderMillis)
    }

    /** Remembers the calls, so that the tests can check them. */
    private class FakeReminderScheduling : ReminderScheduling {
        var rescheduleCount = 0
        var nextReminder: Long? = null

        override fun reschedule(): Int {
            rescheduleCount++
            return 0
        }

        override fun cancelAll() = Unit

        override fun nextReminderMillis(): Long? = nextReminder
    }
}
