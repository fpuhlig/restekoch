package com.restekoch.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.restekoch.recipe.Recipe
import com.restekoch.scan.ScanResponse
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageCacheServiceTest {
    private lateinit var service: ImageCacheService
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var repo: StubImageCacheRepository
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        repo = StubImageCacheRepository()
    }

    private fun newService(enabled: Boolean = true): ImageCacheService {
        return ImageCacheService(
            cacheRepository = repo,
            objectMapper = objectMapper,
            meterRegistry = meterRegistry,
            cacheEnabled = enabled,
            ttlSeconds = 86400,
            modelName = "gemini-2.5-flash",
        )
    }

    @Test
    fun `hash same bytes produces same key`() {
        service = newService()
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        assertEquals(service.hash(bytes), service.hash(bytes))
    }

    @Test
    fun `hash different bytes produces different keys`() {
        service = newService()
        assertNotEquals(
            service.hash(byteArrayOf(1, 2, 3)),
            service.hash(byteArrayOf(1, 2, 4)),
        )
    }

    @Test
    fun `hash rejects empty bytes`() {
        service = newService()
        assertThrows(IllegalArgumentException::class.java) {
            service.hash(ByteArray(0))
        }
    }

    @Test
    fun `keyFor includes model name`() {
        service = newService()
        val hash = service.hash(byteArrayOf(1, 2, 3))
        assertTrue(service.keyFor(hash).startsWith("img:gemini-2.5-flash:"))
    }

    @Test
    fun `lookup returns null on cache miss`() {
        service = newService()
        val result = service.lookup(byteArrayOf(1, 2, 3))
        assertNull(result)
        assertEquals(1.0, meterRegistry.counter("restekoch.image_cache.misses").count())
    }

    @Test
    fun `lookup returns full scan response on cache hit`() {
        service = newService()
        val bytes = byteArrayOf(1, 2, 3)
        val key = service.keyFor(service.hash(bytes))
        val stored =
            ScanResponse(
                ingredients = listOf("egg", "milk", "flour"),
                recipes = listOf(Recipe(id = "42", title = "Pancakes")),
                explanation = "Classic pancakes with your fridge basics.",
                cached = false,
                cacheLevel = null,
            )
        repo.storage[key] = objectMapper.writeValueAsString(stored)
        val result = service.lookup(bytes)
        assertNotNull(result)
        assertEquals(listOf("egg", "milk", "flour"), result!!.ingredients)
        assertEquals("Pancakes", result.recipes[0].title)
        assertEquals("Classic pancakes with your fridge basics.", result.explanation)
        assertEquals(1.0, meterRegistry.counter("restekoch.image_cache.hits").count())
    }

    @Test
    fun `lookup treats corrupted json as miss`() {
        service = newService()
        val bytes = byteArrayOf(1, 2, 3)
        val key = service.keyFor(service.hash(bytes))
        repo.storage[key] = "not valid json"
        val result = service.lookup(bytes)
        assertNull(result)
        assertEquals(1.0, meterRegistry.counter("restekoch.image_cache.misses").count())
    }

    @Test
    fun `lookup treats legacy ingredient-only entry as miss`() {
        // ADR 012 format: stored only a List<String> of ingredients
        service = newService()
        val bytes = byteArrayOf(1, 2, 3)
        val key = service.keyFor(service.hash(bytes))
        repo.storage[key] = """["apple","pear"]"""
        val result = service.lookup(bytes)
        assertNull(result)
        assertEquals(1.0, meterRegistry.counter("restekoch.image_cache.misses").count())
    }

    @Test
    fun `store saves full response under hashed key`() {
        service = newService()
        val bytes = byteArrayOf(1, 2, 3)
        val response =
            ScanResponse(
                ingredients = listOf("apple", "pear"),
                recipes = listOf(Recipe(id = "1", title = "Fruit salad")),
                explanation = "Easy fruit salad.",
            )
        service.store(bytes, response)
        val key = service.keyFor(service.hash(bytes))
        val raw = repo.storage[key]
        assertNotNull(raw)
        val roundTrip = objectMapper.readValue(raw, ScanResponse::class.java)
        assertEquals(listOf("apple", "pear"), roundTrip.ingredients)
        assertEquals("Fruit salad", roundTrip.recipes[0].title)
        assertEquals("Easy fruit salad.", roundTrip.explanation)
    }

    @Test
    fun `store skips empty ingredients`() {
        service = newService()
        val empty =
            ScanResponse(
                ingredients = emptyList(),
                recipes = emptyList(),
                explanation = "nothing",
            )
        service.store(byteArrayOf(1, 2, 3), empty)
        assertTrue(repo.storage.isEmpty())
    }

    @Test
    fun `disabled cache returns null on lookup and skips store`() {
        service = newService(enabled = false)
        assertNull(service.lookup(byteArrayOf(1, 2, 3)))
        service.store(
            byteArrayOf(1, 2, 3),
            ScanResponse(
                ingredients = listOf("egg"),
                recipes = emptyList(),
                explanation = "none",
            ),
        )
        assertTrue(repo.storage.isEmpty())
        assertEquals(0.0, meterRegistry.counter("restekoch.image_cache.misses").count())
    }

    @Test
    fun `stats reports hits misses and disabled flag`() {
        service = newService(enabled = true)
        service.lookup(byteArrayOf(9, 9, 9))
        service.lookup(byteArrayOf(8, 8, 8))
        val stats = service.stats()
        assertEquals(2, stats.misses)
        assertEquals(0, stats.hits)
        assertTrue(stats.enabled)
        assertEquals(86400, stats.ttlSeconds)
    }

    @Test
    fun `disabled stats reflect configuration`() {
        service = newService(enabled = false)
        val stats = service.stats()
        assertFalse(stats.enabled)
    }
}

class StubImageCacheRepository : ImageCacheRepository(
    redis = stubRedisDataSource(),
) {
    val storage = mutableMapOf<String, String>()

    override fun get(key: String): String? = storage[key]

    override fun store(
        key: String,
        value: String,
        ttlSeconds: Long,
    ) {
        storage[key] = value
    }

    override fun entryCount(): Long = storage.size.toLong()

    override fun clear() {
        storage.clear()
    }

    companion object {
        private fun stubRedisDataSource(): io.quarkus.redis.datasource.RedisDataSource {
            return java.lang.reflect.Proxy.newProxyInstance(
                io.quarkus.redis.datasource.RedisDataSource::class.java.classLoader,
                arrayOf(io.quarkus.redis.datasource.RedisDataSource::class.java),
            ) { _, _, _ -> null } as io.quarkus.redis.datasource.RedisDataSource
        }
    }
}
