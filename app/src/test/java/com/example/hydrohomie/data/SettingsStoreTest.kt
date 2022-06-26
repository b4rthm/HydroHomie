package com.example.hydrohomie.data

import com.example.hydrohomie.domain.DrinkSettings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStoreTest {

    private lateinit var file: File
    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        file = File.createTempFile("settings", ".json")
        file.delete()
        store = SettingsStore(file)
    }

    @Test
    fun `without a file the default settings are used`() {
        val settings = store.load()

        assertEquals(DrinkSettingsDefaults.goal, settings.goalMl)
        assertEquals(DrinkSettingsDefaults.glassSize, settings.glassSizeMl)
        assertEquals(DrinkSettingsDefaults.startMinute, settings.startMinuteOfDay)
        assertEquals(DrinkSettingsDefaults.endMinute, settings.endMinuteOfDay)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun `settings survive a round trip`() {
        val settings = DrinkSettings(
            goalMl = 3000,
            glassSizeMl = 300,
            startMinuteOfDay = 9 * 60 + 30,
            endMinuteOfDay = 21 * 60 + 15,
            notificationsEnabled = false,
            language = "de",
        )

        store.save(settings)

        assertEquals(settings, store.load())
    }

    @Test
    fun `the settings of the previous version are read`() {
        file.writeText(
            """
            {
              "totalDrinkAmount": 2200,
              "defaultDrinkSize": 200,
              "startHour": 7,
              "startMin": 30,
              "endHour": 21,
              "endMin": 45,
              "disableNotifications": true,
              "language": "deu"
            }
            """.trimIndent(),
        )

        val settings = store.load()

        assertEquals(2200, settings.goalMl)
        assertEquals(200, settings.glassSizeMl)
        assertEquals(7 * 60 + 30, settings.startMinuteOfDay)
        assertEquals(21 * 60 + 45, settings.endMinuteOfDay)
        // The old key means "notifications are disabled".
        assertFalse(settings.notificationsEnabled)
        // The old language code is converted.
        assertEquals("de", settings.language)
    }

    @Test
    fun `an empty file falls back to the defaults`() {
        file.writeText("")

        val settings = store.load()

        assertEquals(DrinkSettingsDefaults.goal, settings.goalMl)
    }

    @Test
    fun `a broken file falls back to the defaults`() {
        file.writeText("{not json")

        val settings = store.load()

        assertEquals(DrinkSettingsDefaults.goal, settings.goalMl)
    }
}

/** The default values of the settings, kept here to make the test independent. */
private object DrinkSettingsDefaults {
    const val goal = 2500
    const val glassSize = 250
    const val startMinute = 8 * 60
    const val endMinute = 20 * 60
}
