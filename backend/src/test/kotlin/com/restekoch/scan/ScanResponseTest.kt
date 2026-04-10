package com.restekoch.scan

import com.restekoch.recipe.Recipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScanResponseTest {
    @Test
    fun `scan response holds all fields`() {
        val recipes = listOf(Recipe(id = "1", title = "Salad"))
        val response =
            ScanResponse(
                ingredients = listOf("tomato", "lettuce"),
                recipes = recipes,
                explanation = "Fresh salad ingredients.",
            )
        assertEquals(listOf("tomato", "lettuce"), response.ingredients)
        assertEquals(1, response.recipes.size)
        assertEquals("Fresh salad ingredients.", response.explanation)
    }

    @Test
    fun `scan response with empty recipes`() {
        val response =
            ScanResponse(
                ingredients = listOf("unknown"),
                recipes = emptyList(),
                explanation = "No matches.",
            )
        assertEquals(0, response.recipes.size)
        assertEquals("No matches.", response.explanation)
    }
}
