package com.example.hydrohomie.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkProgressTest {

    @Test
    fun `the fraction is the ratio of the goal`() {
        val progress = DrinkProgress(drunkMl = 1250, goalMl = 2500)

        assertEquals(0.5f, progress.fraction, 0.001f)
        assertEquals(50, progress.percent)
    }

    @Test
    fun `the progress never exceeds the goal`() {
        val progress = DrinkProgress(drunkMl = 3000, goalMl = 2500)

        assertEquals(1f, progress.fraction, 0.001f)
        assertEquals(100, progress.percent)
        assertEquals(0, progress.remainingMl)
        assertTrue(progress.isGoalReached)
    }

    @Test
    fun `a goal of zero does not lead to a crash`() {
        val progress = DrinkProgress(drunkMl = 100, goalMl = 0)

        assertEquals(0f, progress.fraction, 0.001f)
        assertFalse(progress.isGoalReached)
    }

    @Test
    fun `the remaining glasses are rounded up`() {
        val progress = DrinkProgress(drunkMl = 100, goalMl = 1000)

        assertEquals(4, progress.remainingGlasses(250))
        assertEquals(3, progress.remainingGlasses(300))
    }

    @Test
    fun `no glasses are missing when the goal is reached`() {
        val progress = DrinkProgress(drunkMl = 1000, goalMl = 1000)

        assertEquals(0, progress.remainingGlasses(250))
    }

    @Test
    fun `a glass size of zero does not lead to a crash`() {
        val progress = DrinkProgress(drunkMl = 100, goalMl = 1000)

        assertEquals(0, progress.remainingGlasses(0))
    }
}
