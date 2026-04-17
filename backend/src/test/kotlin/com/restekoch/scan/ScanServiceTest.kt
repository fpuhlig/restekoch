package com.restekoch.scan

import com.restekoch.cache.ImageCacheService
import com.restekoch.cache.RedisCacheRepository
import com.restekoch.search.RedisVectorRepository
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class ScanServiceTest {
    @Inject
    lateinit var scanService: ScanService

    @Inject
    lateinit var imageCacheService: ImageCacheService

    @Inject
    lateinit var redis: RedisDataSource

    @Inject
    lateinit var redisCacheRepository: RedisCacheRepository

    @Inject
    lateinit var redisVectorRepository: RedisVectorRepository

    @BeforeEach
    fun setUp() {
        redis.execute("FLUSHDB")
        redisCacheRepository.createIndex()
        redisVectorRepository.createIndex()
    }

    @Test
    fun `scan returns detected ingredients`() {
        val result = scanService.scan(uniqueBytes(1), "image/jpeg", 5)
        assertEquals(listOf("chicken", "tomatoes", "garlic", "olive oil", "basil"), result.ingredients)
    }

    @Test
    fun `scan returns explanation`() {
        val result = scanService.scan(uniqueBytes(2), "image/jpeg", 5)
        assertTrue(result.explanation.isNotBlank())
    }

    @Test
    fun `scan response contains all fields`() {
        val result = scanService.scan(uniqueBytes(3), "image/jpeg", 5)
        assertTrue(result.ingredients.isNotEmpty())
        assertTrue(result.explanation.isNotBlank())
    }

    @Test
    fun `first scan stores image cache entry`() {
        val bytes = uniqueBytes(10)
        scanService.scan(bytes, "image/jpeg", 5)
        val cached = imageCacheService.lookup(bytes)
        assertEquals(listOf("chicken", "tomatoes", "garlic", "olive oil", "basil"), cached)
    }

    @Test
    fun `second scan of same image hits L1 cache`() {
        val bytes = uniqueBytes(11)
        scanService.scan(bytes, "image/jpeg", 5)
        val second = scanService.scan(bytes, "image/jpeg", 5)
        assertTrue(second.cached)
        assertTrue(second.cacheLevel == "L1" || second.cacheLevel == "L1+L2")
    }

    @Test
    fun `first scan response has no cache level`() {
        val result = scanService.scan(uniqueBytes(12), "image/jpeg", 5)
        assertEquals(false, result.cached)
        assertNull(result.cacheLevel)
    }

    private fun uniqueBytes(seed: Int): ByteArray = ByteArray(100) { (seed + it).toByte() }
}
