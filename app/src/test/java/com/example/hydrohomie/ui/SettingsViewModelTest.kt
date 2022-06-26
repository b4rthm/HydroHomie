package com.example.hydrohomie.ui

import com.example.hydrohomie.data.SettingsStore
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.domain.SettingsField
import com.example.hydrohomie.domain.SettingsProblem
import com.example.hydrohomie.notifications.ReminderScheduling
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var settingsStore: SettingsStore
    private lateinit var reminders: FakeReminderScheduling

    @Before
    fun setUp() {
        val file = File.createTempFile("settings", ".json").also { it.delete() }
        settingsStore = SettingsStore(file)
        reminders = FakeReminderScheduling()
    }

    private fun viewModel() = SettingsViewModel(settingsStore, reminders)

    @Test
    fun `the stored settings are shown`() {
        settingsStore.save(
            DrinkSettings(
                goalMl = 3000,
                glassSizeMl = 300,
                startMinuteOfDay = 9 * 60,
                endMinuteOfDay = 21 * 60,
                notificationsEnabled = false,
                language = "de",
            ),
        )

        val state = viewModel().state.value

        assertEquals("3000", state.goalText)
        assertEquals("300", state.glassSizeText)
        assertEquals(9 * 60, state.startMinuteOfDay)
        assertEquals(21 * 60, state.endMinuteOfDay)
        assertFalse(state.notificationsEnabled)
        assertEquals("de", state.language)
    }

    @Test
    fun `saving writes the settings`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("3000")
        viewModel.onGlassSizeChanged("300")
        viewModel.onTimeRangeChanged(9 * 60, 21 * 60)
        viewModel.onNotificationsChanged(false)
        viewModel.onLanguageChanged("de")

        val saved = viewModel.save()

        assertTrue(saved)
        val settings = settingsStore.load()
        assertEquals(3000, settings.goalMl)
        assertEquals(300, settings.glassSizeMl)
        assertEquals(9 * 60, settings.startMinuteOfDay)
        assertEquals(21 * 60, settings.endMinuteOfDay)
        assertFalse(settings.notificationsEnabled)
        assertEquals("de", settings.language)
    }

    @Test
    fun `saving recalculates the reminders`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("3000")
        viewModel.onGlassSizeChanged("300")

        viewModel.save()

        assertEquals(1, reminders.rescheduleCount)
    }

    @Test
    fun `invalid input is not saved`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("abc")

        val saved = viewModel.save()

        assertFalse(saved)
        assertEquals(SettingsProblem.NOT_A_NUMBER, viewModel.state.value.problems[SettingsField.GOAL])
        assertTrue(viewModel.state.value.showErrors)
        assertEquals(0, reminders.rescheduleCount)
    }

    @Test
    fun `an invalid drinking window is reported`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("3000")
        viewModel.onGlassSizeChanged("300")
        viewModel.onTimeRangeChanged(20 * 60, 8 * 60)

        val saved = viewModel.save()

        assertFalse(saved)
        assertEquals(
            SettingsProblem.INVALID_RANGE,
            viewModel.state.value.problems[SettingsField.TIME_RANGE],
        )
    }

    @Test
    fun `the problems are updated while typing after a failed save`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("abc")
        viewModel.save()

        viewModel.onGoalChanged("2500")

        assertTrue(viewModel.state.value.problems.isEmpty())
    }

    @Test
    fun `the errors are only shown after a failed save`() {
        val viewModel = viewModel()
        viewModel.onGoalChanged("abc")

        assertFalse(viewModel.state.value.showErrors)
        assertTrue(viewModel.state.value.problems.isEmpty())
    }

    private class FakeReminderScheduling : ReminderScheduling {
        var rescheduleCount = 0

        override fun reschedule(): Int {
            rescheduleCount++
            return 0
        }

        override fun cancelAll() = Unit

        override fun nextReminderMillis(): Long? = null
    }
}
