package com.restekoch.gemini

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class GeminiServiceTest {
    @Inject
    lateinit var geminiService: GeminiService

    @Test
    fun `detectIngredients returns non-empty list`() {
        val result = geminiService.detectIngredients(ByteArray(100), "image/jpeg")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `explainRecipes returns non-empty string`() {
        val result =
            geminiService.explainRecipes(
                listOf("chicken", "tomatoes"),
                listOf("Chicken Salad"),
            )
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `mock returns fixed ingredients`() {
        val result = geminiService.detectIngredients(ByteArray(50), "image/png")
        assertEquals(listOf("chicken", "tomatoes", "garlic", "olive oil", "basil"), result)
    }
}
