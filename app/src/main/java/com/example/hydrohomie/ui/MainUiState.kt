package com.example.hydrohomie.ui

import com.example.hydrohomie.domain.DrinkProgress
import com.example.hydrohomie.domain.DrinkSettings

/**
 * The state of the main screen.
 */
data class MainUiState(
    /** The progress of the daily goal. */
    val progress: DrinkProgress = DrinkProgress(0, DrinkSettings.DEFAULT_GOAL_ML),
    /** The size of the glass the user is about to drink, in ml. */
    val glassSizeMl: Int = DrinkSettings.DEFAULT_GLASS_SIZE_ML,
    /** The current settings. */
    val settings: DrinkSettings = DrinkSettings(),
    /** The amount of the last glass, `null` if there is nothing to undo. */
    val lastDrinkMl: Int? = null,
    /** When the next reminder is shown. */
    val nextReminderMillis: Long? = null,
) {
    /** Whether the goal was reached today. */
    val isGoalReached: Boolean
        get() = progress.isGoalReached
}
