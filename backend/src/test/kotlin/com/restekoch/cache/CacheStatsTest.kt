package com.restekoch.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheStatsTest {
    @Test
    fun `cache stats calculates hit rate`() {
        val stats =
            CacheStats(
                entries = 10,
                hits = 7,
                misses = 3,
                hitRate = 0.7,
                enabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        assertEquals(0.7, stats.hitRate)
        assertEquals(10, stats.entries)
        assertTrue(stats.enabled)
    }

    @Test
    fun `cache stats with zero requests`() {
        val stats =
            CacheStats(
                entries = 0,
                hits = 0,
                misses = 0,
                hitRate = 0.0,
                enabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        assertEquals(0.0, stats.hitRate)
        assertEquals(0, stats.entries)
    }
}
