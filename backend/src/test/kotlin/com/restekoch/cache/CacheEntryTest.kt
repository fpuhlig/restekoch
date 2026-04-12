package com.restekoch.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CacheEntryTest {
    @Test
    fun `cache entry holds all fields`() {
        val entry =
            CacheEntry(
                ingredients = listOf("chicken", "rice"),
                recipesJson = """[{"id":"1","title":"Fried Rice"}]""",
                explanation = "Quick stir fry.",
            )
        assertEquals(2, entry.ingredients.size)
        assertEquals("chicken", entry.ingredients[0])
        assertEquals("Quick stir fry.", entry.explanation)
    }

    @Test
    fun `cache entry with empty recipes`() {
        val entry =
            CacheEntry(
                ingredients = listOf("water"),
                recipesJson = "[]",
                explanation = "No matches.",
            )
        assertEquals("[]", entry.recipesJson)
        assertEquals(1, entry.ingredients.size)
    }
}
