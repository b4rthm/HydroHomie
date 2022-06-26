package com.example.hydrohomie.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * The progress of the daily goal.
 */
data class DrinkProgress(
    /** The amount that was drunk today, in ml. */
    val drunkMl: Int,
    /** The daily goal, in ml. */
    val goalMl: Int,
) {
    /** The progress between `0f` and `1f`. */
    val fraction: Float
        get() = if (goalMl <= 0) 0f else (drunkMl.toFloat() / goalMl).coerceIn(0f, 1f)

    /** The percentage of the goal that was reached. */
    val percent: Int
        get() = (fraction * 100).toInt()

    /** The amount that is still missing, never negative. */
    val remainingMl: Int
        get() = (goalMl - drunkMl).coerceAtLeast(0)

    /** Whether the daily goal was reached. */
    val isGoalReached: Boolean
        get() = goalMl > 0 && drunkMl >= goalMl

    /** The number of glasses that are still missing, rounded up. */
    fun remainingGlasses(glassSizeMl: Int): Int {
        if (glassSizeMl <= 0 || remainingMl <= 0) return 0
        return (remainingMl + glassSizeMl - 1) / glassSizeMl
    }

    companion object {
        fun of(history: DrinkHistory, settings: DrinkSettings, today: LocalDate, zoneId: ZoneId): DrinkProgress =
            DrinkProgress(
                drunkMl = history.amountOn(today, zoneId),
                goalMl = settings.goalMl,
            )
    }
}
