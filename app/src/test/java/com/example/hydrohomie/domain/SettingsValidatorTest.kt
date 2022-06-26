package com.example.hydrohomie.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {

    private fun validate(
        goal: String = "2500",
        glassSize: String = "250",
        start: Int = 8 * 60,
        end: Int = 20 * 60,
    ) = SettingsValidator.validate(goal, glassSize, start, end)

    @Test
    fun `a complete valid input produces settings`() {
        val result = validate()

        assertTrue(result is SettingsValidation.Valid)
        val settings = result.settingsOrNull!!
        assertEquals(2500, settings.goalMl)
        assertEquals(250, settings.glassSizeMl)
        assertEquals(8 * 60, settings.startMinuteOfDay)
        assertEquals(20 * 60, settings.endMinuteOfDay)
    }

    @Test
    fun `an empty goal is reported`() {
        assertEquals(
            mapOf(SettingsField.GOAL to SettingsProblem.REQUIRED),
            validate(goal = "").problems,
        )
    }

    @Test
    fun `an empty glass size is reported`() {
        assertEquals(
            mapOf(SettingsField.GLASS_SIZE to SettingsProblem.REQUIRED),
            validate(glassSize = "").problems,
        )
    }

    @Test
    fun `text that is not a number is reported`() {
        assertEquals(
            mapOf(SettingsField.GOAL to SettingsProblem.NOT_A_NUMBER),
            validate(goal = "abc").problems,
        )
    }

    @Test
    fun `a goal of zero is not allowed`() {
        assertEquals(
            mapOf(SettingsField.GOAL to SettingsProblem.NOT_POSITIVE),
            validate(goal = "0").problems,
        )
    }

    @Test
    fun `a negative glass size is not allowed`() {
        assertEquals(
            mapOf(SettingsField.GLASS_SIZE to SettingsProblem.NOT_POSITIVE),
            validate(glassSize = "-5").problems,
        )
    }

    @Test
    fun `a goal above the maximum is reported`() {
        assertEquals(
            mapOf(SettingsField.GOAL to SettingsProblem.TOO_LARGE),
            validate(goal = (DrinkSettings.MAX_GOAL_ML + 1).toString()).problems,
        )
    }

    @Test
    fun `a drinking window that ends before it starts is reported`() {
        assertEquals(
            mapOf(SettingsField.TIME_RANGE to SettingsProblem.INVALID_RANGE),
            validate(start = 20 * 60, end = 8 * 60).problems,
        )
    }

    @Test
    fun `a drinking window of zero minutes is reported`() {
        assertEquals(
            mapOf(SettingsField.TIME_RANGE to SettingsProblem.INVALID_RANGE),
            validate(start = 8 * 60, end = 8 * 60).problems,
        )
    }

    @Test
    fun `all problems are reported at once`() {
        val result = validate(goal = "abc", glassSize = "")

        assertEquals(2, result.problems.size)
        assertEquals(SettingsProblem.NOT_A_NUMBER, result.problems[SettingsField.GOAL])
        assertEquals(SettingsProblem.REQUIRED, result.problems[SettingsField.GLASS_SIZE])
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        val result = validate(goal = " 2500 ", glassSize = " 250 ")

        assertNull(result.problems[SettingsField.GOAL])
        assertTrue(result is SettingsValidation.Valid)
    }
}
