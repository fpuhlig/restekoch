package com.restekoch.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.restekoch.embedding.EmbeddingService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SemanticCacheServiceTest {
    private lateinit var service: SemanticCacheService
    private lateinit var meterRegistry: SimpleMeterRegistry
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
    }

    @Test
    fun `lookup returns null when cache disabled`() {
        service =
            SemanticCacheService(
                embeddingService = mockEmbedding(),
                cacheRepository = mockRepo(null),
                objectMapper = objectMapper,
                meterRegistry = meterRegistry,
                cacheEnabled = false,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        val result = service.lookup(listOf("chicken", "rice"))
        assertNull(result)
    }

    @Test
    fun `lookup returns null on cache miss`() {
        service =
            SemanticCacheService(
                embeddingService = mockEmbedding(),
                cacheRepository = mockRepo(null),
                objectMapper = objectMapper,
                meterRegistry = meterRegistry,
                cacheEnabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        val result = service.lookup(listOf("chicken", "rice"))
        assertNull(result)
        assertEquals(1.0, meterRegistry.counter("restekoch.cache.misses").count())
    }

    @Test
    fun `lookup returns response on cache hit`() {
        val entry =
            CacheEntry(
                ingredients = listOf("chicken", "rice"),
                recipesJson = "[]",
                explanation = "Cached answer.",
            )
        service =
            SemanticCacheService(
                embeddingService = mockEmbedding(),
                cacheRepository = mockRepo(entry),
                objectMapper = objectMapper,
                meterRegistry = meterRegistry,
                cacheEnabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        val result = service.lookup(listOf("chicken", "rice"))
        assertNotNull(result)
        assertTrue(result!!.cached)
        assertEquals("Cached answer.", result.explanation)
        assertEquals(1.0, meterRegistry.counter("restekoch.cache.hits").count())
    }

    @Test
    fun `stats returns correct counts`() {
        service =
            SemanticCacheService(
                embeddingService = mockEmbedding(),
                cacheRepository = mockRepo(null),
                objectMapper = objectMapper,
                meterRegistry = meterRegistry,
                cacheEnabled = true,
                similarityThreshold = 0.95,
                ttlSeconds = 3600,
            )
        service.lookup(listOf("a"))
        service.lookup(listOf("b"))
        val stats = service.stats()
        assertEquals(2, stats.misses)
        assertEquals(0, stats.hits)
        assertTrue(stats.enabled)
    }

    private fun mockEmbedding(): EmbeddingService {
        return object : EmbeddingService {
            override fun embed(text: String): FloatArray {
                return FloatArray(EmbeddingService.VECTOR_DIM) { 0.1f }
            }

            override fun embedBatch(texts: List<String>): List<FloatArray> {
                return texts.map { embed(it) }
            }
        }
    }

    private fun mockRepo(result: CacheEntry?): StubCacheRepository {
        return StubCacheRepository(result)
    }
}

class StubCacheRepository(
    private val searchResult: CacheEntry? = null,
) : RedisCacheRepository(
        redis = stubRedisDataSource(),
        redisClient = stubRedisClient(),
    ) {
    override fun searchNearest(
        queryVector: FloatArray,
        distanceThreshold: Double,
    ): CacheEntry? {
        return searchResult
    }

    override fun store(
        vector: FloatArray,
        entry: CacheEntry,
        ttlSeconds: Long,
    ) {
        // no-op
    }

    override fun entryCount(): Long {
        return 0
    }

    override fun clear() {
        // no-op
    }

    companion object {
        private fun stubRedisDataSource(): io.quarkus.redis.datasource.RedisDataSource {
            return java.lang.reflect.Proxy.newProxyInstance(
                io.quarkus.redis.datasource.RedisDataSource::class.java.classLoader,
                arrayOf(io.quarkus.redis.datasource.RedisDataSource::class.java),
            ) { _, _, _ -> null } as io.quarkus.redis.datasource.RedisDataSource
        }

        private fun stubRedisClient(): io.vertx.mutiny.redis.client.Redis {
            return io.vertx.mutiny.redis.client.Redis(null)
        }
    }
}
