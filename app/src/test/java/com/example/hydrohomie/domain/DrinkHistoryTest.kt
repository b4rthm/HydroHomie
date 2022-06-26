package com.example.hydrohomie.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkHistoryTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")
    private val monday: LocalDate = LocalDate.of(2024, 5, 13)
    private val tuesday: LocalDate = LocalDate.of(2024, 5, 14)

    private fun millis(date: LocalDate, hour: Int): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `a new glass increases the sum`() {
        val history = DrinkHistory.EMPTY.add(250, millis(monday, 9))

        assertEquals(250, history.totalMl)
        assertEquals(1, history.entries.size)
    }

    @Test
    fun `a glass of zero is ignored`() {
        val history = DrinkHistory.EMPTY.add(0, millis(monday, 9))

        assertEquals(0, history.totalMl)
        assertTrue(history.entries.isEmpty())
    }

    @Test
    fun `undo removes the last glass`() {
        val history = DrinkHistory.EMPTY
            .add(250, millis(monday, 9))
            .add(500, millis(monday, 10))

        val undone = history.undo()

        assertEquals(250, undone.totalMl)
        assertEquals(1, undone.entries.size)
    }

    @Test
    fun `undo on an empty history does nothing`() {
        val history = DrinkHistory.EMPTY.undo()

        assertEquals(0, history.totalMl)
        assertTrue(history.entries.isEmpty())
    }

    @Test
    fun `the amount of a day only counts the glasses of that day`() {
        val history = DrinkHistory.EMPTY
            .add(250, millis(monday, 9))
            .add(300, millis(tuesday, 9))

        assertEquals(250, history.amountOn(monday, zone))
        assertEquals(300, history.amountOn(tuesday, zone))
        assertEquals(550, history.totalMl)
    }

    @Test
    fun `the glasses of older days can be removed`() {
        val history = DrinkHistory.EMPTY
            .add(250, millis(monday, 9))
            .add(300, millis(tuesday, 9))

        val cleaned = history.dropBefore(tuesday, zone)

        assertEquals(1, cleaned.entries.size)
        assertEquals(300, cleaned.totalMl)
    }

    @Test
    fun `the last glass is remembered`() {
        val history = DrinkHistory.EMPTY.add(250, millis(monday, 9))

        assertEquals(250, history.lastEntry?.amountMl)
        assertNull(DrinkHistory.EMPTY.lastEntry)
    }
}
