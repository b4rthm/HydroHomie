package com.example.hydrohomie

import android.os.Looper
import android.widget.TextView
import com.example.hydrohomie.domain.DrinkHistory
import com.example.hydrohomie.domain.DrinkSettings
import com.example.hydrohomie.notifications.DataChangedReceiver
import java.time.LocalDate
import java.time.ZoneId
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        idle()
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `the progress of the day is shown`() {
        val progress = activity.findViewById<TextView>(R.id.progressAmount)

        assertEquals("0 / 2500 ml", progress.text.toString())
    }

    @Test
    fun `drinking adds a glass to the progress`() {
        activity.findViewById<AppCompatButton>(R.id.drinkButton).performClick()
        idle()

        val progress = activity.findViewById<TextView>(R.id.progressAmount)
        assertEquals("250 / 2500 ml", progress.text.toString())
    }

    @Test
    fun `the glass size can be changed`() {
        activity.findViewById<TextInputEditText>(R.id.glassSizeInput).setText("500")
        idle()
        activity.findViewById<AppCompatButton>(R.id.drinkButton).performClick()
        idle()

        val progress = activity.findViewById<TextView>(R.id.progressAmount)
        assertEquals("500 / 2500 ml", progress.text.toString())
    }

    @Test
    fun `undo removes the glass again`() {
        activity.findViewById<AppCompatButton>(R.id.drinkButton).performClick()
        idle()
        activity.findViewById<android.view.View>(R.id.undoButton).performClick()
        idle()

        val progress = activity.findViewById<TextView>(R.id.progressAmount)
        assertEquals("0 / 2500 ml", progress.text.toString())
    }

    @Test
    fun `the undo button is disabled without a glass`() {
        assertFalse(activity.findViewById<android.view.View>(R.id.undoButton).isEnabled)

        activity.findViewById<AppCompatButton>(R.id.drinkButton).performClick()
        idle()

        assertTrue(activity.findViewById<android.view.View>(R.id.undoButton).isEnabled)
    }

    @Test
    fun `the screen is updated when a glass was drunk by the notification`() {
        val app = activity.application as HydroHomieApp
        val now = LocalDate.now(ZoneId.systemDefault())
            .atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        app.historyStore.save(DrinkHistory.EMPTY.add(300, now))

        // The notification counts the glass in the background and announces it.
        DataChangedReceiver.notifyChanged(activity)
        idle()

        val progress = activity.findViewById<TextView>(R.id.progressAmount)
        assertEquals("300 / 2500 ml", progress.text.toString())
    }

    @Test
    fun `the screen is updated when the settings were changed`() {
        val app = activity.application as HydroHomieApp
        app.settingsStore.save(DrinkSettings(goalMl = 3000, glassSizeMl = 250))

        DataChangedReceiver.notifyChanged(activity)
        idle()

        val progress = activity.findViewById<TextView>(R.id.progressAmount)
        assertEquals("0 / 3000 ml", progress.text.toString())
    }

    @Test
    fun `the goal is shown when it is reached`() {
        activity.findViewById<TextInputEditText>(R.id.glassSizeInput).setText("2500")
        idle()
        activity.findViewById<AppCompatButton>(R.id.drinkButton).performClick()
        idle()

        val status = activity.findViewById<TextView>(R.id.goalStatus)
        assertEquals(activity.getString(R.string.goal_reached), status.text.toString())
    }
}
