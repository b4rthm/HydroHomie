package com.example.hydrohomie.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A single glass of water that was drunk. */
data class DrinkEntry(
    /** When the glass was drunk. */
    val timestampMillis: Long,
    /** How much was drunk, in ml. */
    val amountMl: Int,
) {
    fun toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
}

/**
 * All glasses that were drunk, newest last.
 *
 * Keeping the individual glasses (instead of only a sum) makes it possible to undo the
 * last glass and to show the amount of a single day.
 */
data class DrinkHistory(
    val entries: List<DrinkEntry> = emptyList(),
) {

    /** The sum of all glasses. */
    val totalMl: Int
        get() = entries.sumOf { it.amountMl }

    /** Adds a glass of water. */
    fun add(amountMl: Int, timestampMillis: Long): DrinkHistory {
        if (amountMl <= 0) return this
        return copy(entries = entries + DrinkEntry(timestampMillis, amountMl))
    }

    /** Removes the glass that was drunk last. */
    fun undo(): DrinkHistory {
        if (entries.isEmpty()) return this
        return copy(entries = entries.dropLast(1))
    }

    /** The last glass that was drunk. */
    val lastEntry: DrinkEntry?
        get() = entries.lastOrNull()

    /** The amount that was drunk on the given day. */
    fun amountOn(date: LocalDate, zoneId: ZoneId): Int =
        entries.filter { it.toLocalDate(zoneId) == date }.sumOf { it.amountMl }

    /** Removes all glasses that were drunk before the given day. */
    fun dropBefore(date: LocalDate, zoneId: ZoneId): DrinkHistory =
        copy(entries = entries.filter { !it.toLocalDate(zoneId).isBefore(date) })

    companion object {
        val EMPTY = DrinkHistory()
    }
}
