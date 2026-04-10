package com.restekoch.gemini

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MockGeminiServiceTest {
    private val service = MockGeminiService()

    @Test
    fun `returns fixed ingredient list`() {
        val result = service.detectIngredients(ByteArray(10), "image/jpeg")
        assertEquals(5, result.size)
        assertTrue(result.contains("chicken"))
        assertTrue(result.contains("tomatoes"))
    }

    @Test
    fun `returns explanation for each recipe`() {
        val result =
            service.explainRecipes(
                listOf("chicken", "rice"),
                listOf("Chicken Rice", "Fried Rice"),
            )
        assertTrue(result.contains("Chicken Rice"))
        assertTrue(result.contains("Fried Rice"))
    }
}
