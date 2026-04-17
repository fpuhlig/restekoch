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

    @Test
    fun `cache stats exposes image cache fields`() {
        val stats =
            CacheStats(
                entries = 5,
                hits = 4,
                misses = 1,
                hitRate = 0.8,
                enabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
                imageEntries = 2,
                imageHits = 10,
                imageMisses = 5,
                imageHitRate = 0.6667,
                imageEnabled = true,
                imageTtlSeconds = 86400,
            )
        assertEquals(2, stats.imageEntries)
        assertEquals(10, stats.imageHits)
        assertEquals(5, stats.imageMisses)
        assertEquals(0.6667, stats.imageHitRate)
        assertTrue(stats.imageEnabled)
        assertEquals(86400, stats.imageTtlSeconds)
    }
}
