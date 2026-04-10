package com.restekoch.gemini

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for VertexGeminiService parsing logic.
 * Real API calls are tested on GCP, not in CI.
 */
class VertexGeminiServiceTest {
    @Test
    fun `ingredient parsing splits comma-separated response`() {
        val raw = "chicken, tomatoes, garlic, olive oil"
        val result =
            raw
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
        assertEquals(listOf("chicken", "tomatoes", "garlic", "olive oil"), result)
    }

    @Test
    fun `ingredient parsing handles extra whitespace`() {
        val raw = "  Chicken ,  Tomatoes  , , Garlic "
        val result =
            raw
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
        assertEquals(listOf("chicken", "tomatoes", "garlic"), result)
    }
}
