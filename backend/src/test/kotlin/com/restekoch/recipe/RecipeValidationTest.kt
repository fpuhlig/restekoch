package com.restekoch.recipe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecipeValidationTest {
    @Test
    fun limitClampedToMinimum() {
        assertEquals(1, (-5).coerceIn(1, 100))
        assertEquals(1, 0.coerceIn(1, 100))
    }

    @Test
    fun limitClampedToMaximum() {
        assertEquals(100, 999.coerceIn(1, 100))
        assertEquals(100, 101.coerceIn(1, 100))
    }

    @Test
    fun limitPassesThroughInRange() {
        assertEquals(20, 20.coerceIn(1, 100))
        assertEquals(1, 1.coerceIn(1, 100))
        assertEquals(100, 100.coerceIn(1, 100))
    }

    @Test
    fun offsetNeverNegative() {
        assertEquals(0, maxOf(0, -5))
        assertEquals(0, maxOf(0, -1))
        assertEquals(0, maxOf(0, 0))
        assertEquals(5, maxOf(0, 5))
    }
}
